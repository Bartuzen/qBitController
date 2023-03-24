package dev.bartuzen.qbitcontroller.ui.torrent.tabs.overview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.bartuzen.qbitcontroller.data.SettingsManager
import dev.bartuzen.qbitcontroller.data.notification.TorrentDownloadedNotifier
import dev.bartuzen.qbitcontroller.data.repositories.torrent.TorrentOverviewRepository
import dev.bartuzen.qbitcontroller.model.Torrent
import dev.bartuzen.qbitcontroller.model.TorrentProperties
import dev.bartuzen.qbitcontroller.network.RequestResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TorrentOverviewViewModel @Inject constructor(
    settingsManager: SettingsManager,
    private val repository: TorrentOverviewRepository,
    private val notifier: TorrentDownloadedNotifier
) : ViewModel() {
    private val _torrent = MutableStateFlow<Torrent?>(null)
    val torrent = _torrent.asStateFlow()

    private val _torrentProperties = MutableStateFlow<TorrentProperties?>(null)
    val torrentProperties = _torrentProperties.asStateFlow()

    private val eventChannel = Channel<Event>()
    val eventFlow = eventChannel.receiveAsFlow()

    private val _isNaturalLoading = MutableStateFlow<Boolean?>(null)
    val isNaturalLoading = _isNaturalLoading.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    var isInitialLoadStarted = false

    val autoRefreshInterval = settingsManager.autoRefreshInterval.flow
    val autoRefreshHideLoadingBar = settingsManager.autoRefreshHideLoadingBar.flow

    private fun updateTorrent(serverId: Int, torrentHash: String) = viewModelScope.launch {
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

        val torrent = torrentDeferred.await()
        val properties = propertiesDeferred.await()

        _torrent.value = torrent
        _torrentProperties.value = properties

        notifier.checkCompleted(serverId, torrent)
    }

    fun loadTorrent(serverId: Int, torrentHash: String, autoRefresh: Boolean = false) {
        if (isNaturalLoading.value == null) {
            _isNaturalLoading.value = !autoRefresh
            updateTorrent(serverId, torrentHash).invokeOnCompletion {
                _isNaturalLoading.value = null
            }
        }
    }

    fun refreshTorrent(serverId: Int, torrentHash: String) {
        if (!isRefreshing.value) {
            _isRefreshing.value = true
            updateTorrent(serverId, torrentHash).invokeOnCompletion {
                _isRefreshing.value = false
            }
        }
    }

    fun deleteTorrent(serverId: Int, torrentHash: String, deleteFiles: Boolean) = viewModelScope.launch {
        when (val result = repository.deleteTorrent(serverId, torrentHash, deleteFiles)) {
            is RequestResult.Success -> {
                eventChannel.send(Event.TorrentDeleted)
            }
            is RequestResult.Error -> {
                eventChannel.send(Event.Error(result))
            }
        }
    }

    fun pauseTorrent(serverId: Int, torrentHash: String) = viewModelScope.launch {
        when (val result = repository.pauseTorrent(serverId, torrentHash)) {
            is RequestResult.Success -> {
                eventChannel.send(Event.TorrentPaused)
            }
            is RequestResult.Error -> {
                eventChannel.send(Event.Error(result))
            }
        }
    }

    fun resumeTorrent(serverId: Int, torrentHash: String) = viewModelScope.launch {
        when (val result = repository.resumeTorrent(serverId, torrentHash)) {
            is RequestResult.Success -> {
                eventChannel.send(Event.TorrentResumed)
            }
            is RequestResult.Error -> {
                eventChannel.send(Event.Error(result))
            }
        }
    }

    fun setTorrentOptions(
        serverId: Int,
        torrentHash: String,
        autoTmm: Boolean?,
        savePath: String?,
        toggleSequentialDownload: Boolean,
        togglePrioritizeFirstLastPiece: Boolean,
        uploadSpeedLimit: Int?,
        downloadSpeedLimit: Int?,
        ratioLimit: Double?,
        seedingTimeLimit: Int?
    ) = viewModelScope.launch {
        val requests = mutableListOf<suspend () -> RequestResult<Any>>()

        if (autoTmm != null) {
            requests.add { repository.setAutomaticTorrentManagement(serverId, torrentHash, autoTmm) }
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
        if (ratioLimit != null && seedingTimeLimit != null) {
            requests.add { repository.setShareLimit(serverId, torrentHash, ratioLimit, seedingTimeLimit) }
        }

        if (requests.isEmpty()) {
            return@launch
        }
        try {
            requests.map { request ->
                launch {
                    val result = request()
                    if (result is RequestResult.Error) {
                        eventChannel.send(Event.Error(result))
                        throw CancellationException()
                    }
                }
            }.joinAll()
        } catch (_: CancellationException) {
            return@launch
        }

        eventChannel.send(Event.OptionsUpdated)
    }

    fun setForceStart(serverId: Int, torrentHash: String, value: Boolean) = viewModelScope.launch {
        when (val result = repository.setForceStart(serverId, torrentHash, value)) {
            is RequestResult.Success -> {
                eventChannel.send(Event.ForceStartChanged(value))
            }
            is RequestResult.Error -> {
                eventChannel.send(Event.Error(result))
            }
        }
    }

    fun setSuperSeeding(serverId: Int, torrentHash: String, value: Boolean) = viewModelScope.launch {
        when (val result = repository.setSuperSeeding(serverId, torrentHash, value)) {
            is RequestResult.Success -> {
                eventChannel.send(Event.SuperSeedingChanged(value))
            }
            is RequestResult.Error -> {
                eventChannel.send(Event.Error(result))
            }
        }
    }

    fun recheckTorrent(serverId: Int, torrentHash: String) = viewModelScope.launch {
        when (val result = repository.recheckTorrent(serverId, torrentHash)) {
            is RequestResult.Success -> {
                eventChannel.send(Event.TorrentRechecked)
            }
            is RequestResult.Error -> {
                eventChannel.send(Event.Error(result))
            }
        }
    }

    fun reannounceTorrent(serverId: Int, torrentHash: String) = viewModelScope.launch {
        when (val result = repository.reannounceTorrent(serverId, torrentHash)) {
            is RequestResult.Success -> {
                eventChannel.send(Event.TorrentReannounced)
            }
            is RequestResult.Error -> {
                eventChannel.send(Event.Error(result))
            }
        }
    }

    fun renameTorrent(serverId: Int, torrentHash: String, name: String) = viewModelScope.launch {
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

    fun setCategory(serverId: Int, torrentHash: String, category: String?) = viewModelScope.launch {
        when (val result = repository.setCategory(serverId, torrentHash, category)) {
            is RequestResult.Success -> {
                eventChannel.send(Event.CategoryUpdated)
            }
            is RequestResult.Error -> {
                eventChannel.send(Event.Error(result))
            }
        }
    }

    fun setTags(serverId: Int, torrentHash: String, newTags: List<String>) = viewModelScope.launch {
        val currentTags = torrent.value?.tags ?: emptyList()

        val addedTags = newTags.filter { it !in currentTags }
        val removedTags = currentTags.filter { it !in newTags }

        val addedTagsDeferred = async {
            if (addedTags.isNotEmpty()) {
                when (val result = repository.addTags(serverId, torrentHash, addedTags)) {
                    is RequestResult.Success -> {
                    }
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
                    is RequestResult.Success -> {
                    }
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
        } catch (_: CancellationException) {
        }
    }

    sealed class Event {
        data class Error(val error: RequestResult.Error) : Event()
        object TorrentNotFound : Event()
        object TorrentDeleted : Event()
        object TorrentPaused : Event()
        object TorrentResumed : Event()
        object OptionsUpdated : Event()
        object TorrentRechecked : Event()
        object TorrentReannounced : Event()
        object TorrentRenamed : Event()
        data class ForceStartChanged(val isEnabled: Boolean) : Event()
        data class SuperSeedingChanged(val isEnabled: Boolean) : Event()
        object CategoryUpdated : Event()
        object TagsUpdated : Event()
    }
}
