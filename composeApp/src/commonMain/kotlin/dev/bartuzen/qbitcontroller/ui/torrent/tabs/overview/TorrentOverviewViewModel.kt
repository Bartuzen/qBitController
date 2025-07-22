package dev.bartuzen.qbitcontroller.ui.torrent.tabs.overview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.bartuzen.qbitcontroller.data.SettingsManager
import dev.bartuzen.qbitcontroller.data.notification.TorrentDownloadedNotifier
import dev.bartuzen.qbitcontroller.data.repositories.torrent.TorrentOverviewRepository
import dev.bartuzen.qbitcontroller.model.PieceState
import dev.bartuzen.qbitcontroller.model.Torrent
import dev.bartuzen.qbitcontroller.model.TorrentProperties
import dev.bartuzen.qbitcontroller.network.RequestResult
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.delete
import io.github.vinceglb.filekit.sink
import io.ktor.utils.io.core.remaining
import io.ktor.utils.io.readBuffer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

class TorrentOverviewViewModel(
    private val serverId: Int,
    private val torrentHash: String,
    settingsManager: SettingsManager,
    private val repository: TorrentOverviewRepository,
    private val notifier: TorrentDownloadedNotifier,
) : ViewModel() {
    private val _torrent = MutableStateFlow<Torrent?>(null)
    val torrent = _torrent.asStateFlow()

    private val _torrentProperties = MutableStateFlow<TorrentProperties?>(null)
    val torrentProperties = _torrentProperties.asStateFlow()

    private val _torrentPieces = MutableStateFlow<List<PieceState>?>(null)
    val torrentPieces = _torrentPieces.asStateFlow()

    private val eventChannel = Channel<Event>()
    val eventFlow = eventChannel.receiveAsFlow()

    private val _isNaturalLoading = MutableStateFlow<Boolean?>(null)
    val isNaturalLoading = _isNaturalLoading.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    val autoRefreshInterval = settingsManager.autoRefreshInterval.flow

    private val isScreenActive = MutableStateFlow(false)

    init {
        loadTorrent()

        viewModelScope.launch {
            combine(
                autoRefreshInterval,
                isNaturalLoading,
                isScreenActive,
            ) { autoRefreshInterval, isNaturalLoading, isScreenActive ->
                Triple(autoRefreshInterval, isNaturalLoading, isScreenActive)
            }.collectLatest { (autoRefreshInterval, isNaturalLoading, isScreenActive) ->
                if (isScreenActive && isNaturalLoading == null && autoRefreshInterval != 0) {
                    delay(autoRefreshInterval.seconds)
                    loadTorrent(autoRefresh = true)
                }
            }
        }
    }

    fun setScreenActive(isScreenActive: Boolean) {
        this.isScreenActive.value = isScreenActive
    }

    private fun updateTorrent() = viewModelScope.launch {
        val torrentDeferred = async {
            when (val result = repository.getTorrent(serverId, torrentHash)) {
                is RequestResult.Success -> {
                    if (result.data.size == 1) {
                        result.data.first()
                    } else {
                        eventChannel.send(Event.TorrentNotFound)
                        throw CancellationException()
                    }
                }
                is RequestResult.Error -> {
                    eventChannel.send(Event.Error(result))
                    throw CancellationException()
                }
            }
        }
        val propertiesDeferred = async {
            when (val result = repository.getProperties(serverId, torrentHash)) {
                is RequestResult.Success -> {
                    result.data
                }
                is RequestResult.Error -> {
                    if (result is RequestResult.Error.ApiError && result.code == 404) {
                        eventChannel.send(Event.TorrentNotFound)
                    } else {
                        eventChannel.send(Event.Error(result))
                    }
                    throw CancellationException()
                }
            }
        }
        val piecesDeferred = async {
            when (val result = repository.getPieces(serverId, torrentHash)) {
                is RequestResult.Success -> {
                    result.data
                }
                is RequestResult.Error -> {
                    if (result is RequestResult.Error.ApiError && result.code == 404) {
                        eventChannel.send(Event.TorrentNotFound)
                    } else {
                        eventChannel.send(Event.Error(result))
                    }
                    throw CancellationException()
                }
            }
        }

        val torrent = torrentDeferred.await()
        val properties = propertiesDeferred.await()
        val pieces = piecesDeferred.await()

        _torrent.value = torrent
        _torrentProperties.value = properties
        _torrentPieces.value = pieces

        notifier.checkCompleted(serverId, torrent)
    }

    fun loadTorrent(autoRefresh: Boolean = false) {
        if (isNaturalLoading.value == null) {
            _isNaturalLoading.value = !autoRefresh
            updateTorrent().invokeOnCompletion {
                _isNaturalLoading.value = null
            }
        }
    }

    fun refreshTorrent() {
        if (!isRefreshing.value) {
            _isRefreshing.value = true
            updateTorrent().invokeOnCompletion {
                viewModelScope.launch {
                    delay(25)
                    _isRefreshing.value = false
                }
            }
        }
    }

    fun deleteTorrent(deleteFiles: Boolean) = viewModelScope.launch {
        when (val result = repository.deleteTorrent(serverId, torrentHash, deleteFiles)) {
            is RequestResult.Success -> {
                eventChannel.send(Event.TorrentDeleted)
            }
            is RequestResult.Error -> {
                eventChannel.send(Event.Error(result))
            }
        }
    }

    fun pauseTorrent() = viewModelScope.launch {
        when (val result = repository.pauseTorrent(serverId, torrentHash)) {
            is RequestResult.Success -> {
                eventChannel.send(Event.TorrentPaused)
                launch {
                    delay(1000)
                    loadTorrent()
                }
            }
            is RequestResult.Error -> {
                eventChannel.send(Event.Error(result))
            }
        }
    }

    fun resumeTorrent() = viewModelScope.launch {
        when (val result = repository.resumeTorrent(serverId, torrentHash)) {
            is RequestResult.Success -> {
                eventChannel.send(Event.TorrentResumed)
                launch {
                    delay(1000)
                    loadTorrent()
                }
            }
            is RequestResult.Error -> {
                eventChannel.send(Event.Error(result))
            }
        }
    }

    fun setTorrentOptions(
        autoTmm: Boolean?,
        savePath: String?,
        downloadPath: String?,
        toggleSequentialDownload: Boolean,
        togglePrioritizeFirstLastPiece: Boolean,
        uploadSpeedLimit: Int?,
        downloadSpeedLimit: Int?,
        ratioLimit: Double?,
        seedingTimeLimit: Int?,
        inactiveSeedingTimeLimit: Int?,
    ) = viewModelScope.launch {
        val requests = mutableListOf<suspend () -> RequestResult<Any>>()

        if (autoTmm != null) {
            requests.add { repository.setAutomaticTorrentManagement(serverId, torrentHash, autoTmm) }
        }
        if (downloadPath != null) {
            requests.add { repository.setDownloadPath(serverId, torrentHash, downloadPath) }
        }
        if (savePath != null) {
            requests.add { repository.setLocation(serverId, torrentHash, savePath) }
        }
        if (toggleSequentialDownload) {
            requests.add { repository.toggleSequentialDownload(serverId, torrentHash) }
        }
        if (togglePrioritizeFirstLastPiece) {
            requests.add { repository.togglePrioritizeFirstLastPiecesDownload(serverId, torrentHash) }
        }
        if (uploadSpeedLimit != null) {
            requests.add { repository.setUploadSpeedLimit(serverId, torrentHash, uploadSpeedLimit) }
        }
        if (downloadSpeedLimit != null) {
            requests.add { repository.setDownloadSpeedLimit(serverId, torrentHash, downloadSpeedLimit) }
        }
        if (ratioLimit != null && seedingTimeLimit != null && inactiveSeedingTimeLimit != null) {
            requests.add {
                repository.setShareLimit(
                    serverId,
                    torrentHash,
                    ratioLimit,
                    seedingTimeLimit,
                    inactiveSeedingTimeLimit,
                )
            }
        }

        if (requests.isEmpty()) {
            return@launch
        }

        // setting download path when auto TMM is enabled has no effect, so we need to set auto TMM first
        val delayDownloadPathRequest = autoTmm != null && downloadPath != null

        var isErrored = false

        requests.filterIndexed { index, _ ->
            !delayDownloadPathRequest || index != -1
        }.map { request ->
            launch {
                val result = request()
                if (result is RequestResult.Error) {
                    isErrored = true
                    eventChannel.send(Event.Error(result))
                    throw CancellationException()
                }
            }
        }.joinAll()
        if (delayDownloadPathRequest && !isErrored) {
            launch {
                val result = requests[1]()
                if (result is RequestResult.Error) {
                    isErrored = true
                    eventChannel.send(Event.Error(result))
                }
            }
        }

        if (!isErrored) {
            eventChannel.send(Event.OptionsUpdated)
            loadTorrent()
        }
    }

    fun setForceStart(value: Boolean) = viewModelScope.launch {
        when (val result = repository.setForceStart(serverId, torrentHash, value)) {
            is RequestResult.Success -> {
                eventChannel.send(Event.ForceStartChanged(value))
                launch {
                    delay(1000)
                    loadTorrent()
                }
            }
            is RequestResult.Error -> {
                eventChannel.send(Event.Error(result))
            }
        }
    }

    fun setSuperSeeding(value: Boolean) = viewModelScope.launch {
        when (val result = repository.setSuperSeeding(serverId, torrentHash, value)) {
            is RequestResult.Success -> {
                eventChannel.send(Event.SuperSeedingChanged(value))
                launch {
                    delay(1000)
                    loadTorrent()
                }
            }
            is RequestResult.Error -> {
                eventChannel.send(Event.Error(result))
            }
        }
    }

    fun recheckTorrent() = viewModelScope.launch {
        when (val result = repository.recheckTorrent(serverId, torrentHash)) {
            is RequestResult.Success -> {
                eventChannel.send(Event.TorrentRechecked)
                launch {
                    delay(1000)
                    loadTorrent()
                }
            }
            is RequestResult.Error -> {
                eventChannel.send(Event.Error(result))
            }
        }
    }

    fun reannounceTorrent() = viewModelScope.launch {
        when (val result = repository.reannounceTorrent(serverId, torrentHash)) {
            is RequestResult.Success -> {
                eventChannel.send(Event.TorrentReannounced)
            }
            is RequestResult.Error -> {
                eventChannel.send(Event.Error(result))
            }
        }
    }

    fun renameTorrent(name: String) = viewModelScope.launch {
        when (val result = repository.renameTorrent(serverId, torrentHash, name)) {
            is RequestResult.Success -> {
                eventChannel.send(Event.TorrentRenamed)
            }
            is RequestResult.Error -> {
                if (result is RequestResult.Error.ApiError && result.code == 404) {
                    eventChannel.send(Event.TorrentNotFound)
                } else {
                    eventChannel.send(Event.Error(result))
                }
            }
        }
    }

    private val _categories = MutableStateFlow<List<String>?>(null)
    val categories = _categories.asStateFlow()

    private val _tags = MutableStateFlow<List<String>?>(null)
    val tags = _tags.asStateFlow()

    private var categoryJob = Job()
    private var tagJob = Job()

    fun loadCategories() = viewModelScope.launch(categoryJob) {
        when (val result = repository.getCategories(serverId)) {
            is RequestResult.Success -> {
                _categories.value = result.data.values
                    .toList()
                    .map { it.name }
                    .sorted()
            }
            is RequestResult.Error -> {
                eventChannel.send(Event.Error(result))
            }
        }
    }

    fun resetCategories() {
        categoryJob.cancel()
        categoryJob = Job()
        _categories.value = null
    }

    fun loadTags() = viewModelScope.launch(tagJob) {
        when (val result = repository.getTags(serverId)) {
            is RequestResult.Success -> {
                _tags.value = result.data.sorted()
            }
            is RequestResult.Error -> {
                eventChannel.send(Event.Error(result))
            }
        }
    }

    fun resetTags() {
        tagJob.cancel()
        tagJob = Job()
        _tags.value = null
    }

    fun setCategory(category: String?) = viewModelScope.launch {
        when (val result = repository.setCategory(serverId, torrentHash, category)) {
            is RequestResult.Success -> {
                eventChannel.send(Event.CategoryUpdated)
                loadTorrent()
            }
            is RequestResult.Error -> {
                eventChannel.send(Event.Error(result))
            }
        }
    }

    fun setTags(newTags: List<String>) = viewModelScope.launch {
        val currentTags = torrent.value?.tags ?: emptyList()

        val addedTags = newTags.filter { it !in currentTags }
        val removedTags = currentTags.filter { it !in newTags }

        val addedTagsDeferred = async {
            if (addedTags.isNotEmpty()) {
                when (val result = repository.addTags(serverId, torrentHash, addedTags)) {
                    is RequestResult.Success -> {}
                    is RequestResult.Error -> {
                        eventChannel.send(Event.Error(result))
                        throw CancellationException()
                    }
                }
            }
        }
        val removedTagsDeferred = async {
            if (removedTags.isNotEmpty()) {
                when (val result = repository.removeTags(serverId, torrentHash, removedTags)) {
                    is RequestResult.Success -> {}
                    is RequestResult.Error -> {
                        eventChannel.send(Event.Error(result))
                        throw CancellationException()
                    }
                }
            }
        }

        try {
            addedTagsDeferred.await()
            removedTagsDeferred.await()

            eventChannel.send(Event.TagsUpdated)
            loadTorrent()
        } catch (_: CancellationException) {
        }
    }

    fun exportTorrent(file: PlatformFile) = viewModelScope.launch {
        when (
            val result = repository.exportTorrent(serverId, torrentHash) { channel ->
                try {
                    val buffer = channel.readBuffer()
                    file.sink().write(buffer, buffer.remaining)
                } catch (_: Exception) {
                    file.tryDelete()
                    eventChannel.send(Event.TorrentExportError)
                }
            }
        ) {
            is RequestResult.Success -> {
                eventChannel.send(Event.TorrentExported)
            }
            is RequestResult.Error -> {
                if (result is RequestResult.Error.ApiError && result.code == 409) {
                    eventChannel.send(Event.TorrentExportError)
                } else if (result is RequestResult.Error.ApiError && result.code == 404) {
                    eventChannel.send(Event.TorrentNotFound)
                } else {
                    eventChannel.send(Event.Error(result))
                }

                file.tryDelete()
            }
        }
    }

    suspend fun PlatformFile.tryDelete() {
        try {
            delete()
        } catch (_: Exception) { }
    }

    sealed class Event {
        data class Error(val error: RequestResult.Error) : Event()
        data object TorrentNotFound : Event()
        data object TorrentDeleted : Event()
        data object TorrentPaused : Event()
        data object TorrentResumed : Event()
        data object OptionsUpdated : Event()
        data object TorrentRechecked : Event()
        data object TorrentReannounced : Event()
        data object TorrentRenamed : Event()
        data class ForceStartChanged(val isEnabled: Boolean) : Event()
        data class SuperSeedingChanged(val isEnabled: Boolean) : Event()
        data object CategoryUpdated : Event()
        data object TagsUpdated : Event()
        data object TorrentExported : Event()
        data object TorrentExportError : Event()
    }
}
