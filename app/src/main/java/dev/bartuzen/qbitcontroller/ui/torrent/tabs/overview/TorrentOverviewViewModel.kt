package dev.bartuzen.qbitcontroller.ui.torrent.tabs.overview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.bartuzen.qbitcontroller.data.SettingsManager
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
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TorrentOverviewViewModel @Inject constructor(
    settingsManager: SettingsManager,
    private val repository: TorrentOverviewRepository
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

    fun toggleSequentialDownload(serverId: Int, torrentHash: String) = viewModelScope.launch {
        when (val result = repository.toggleSequentialDownload(serverId, torrentHash)) {
            is RequestResult.Success -> {
                eventChannel.send(Event.SequentialDownloadToggled)
            }
            is RequestResult.Error -> {
                eventChannel.send(Event.Error(result))
            }
        }
    }

    fun togglePrioritizeFirstLastPiecesDownload(serverId: Int, torrentHash: String) = viewModelScope.launch {
        when (val result = repository.togglePrioritizeFirstLastPiecesDownload(serverId, torrentHash)) {
            is RequestResult.Success -> {
                eventChannel.send(Event.PrioritizeFirstLastPiecesToggled)
            }
            is RequestResult.Error -> {
                eventChannel.send(Event.Error(result))
            }
        }
    }

    fun setAutomaticTorrentManagement(serverId: Int, torrentHash: String, enable: Boolean) = viewModelScope.launch {
        when (val result = repository.setAutomaticTorrentManagement(serverId, torrentHash, enable)) {
            is RequestResult.Success -> {
                eventChannel.send(Event.AutomaticTorrentManagementChanged(enable))
            }
            is RequestResult.Error -> {
                eventChannel.send(Event.Error(result))
            }
        }
    }

    fun setDownloadSpeedLimit(serverId: Int, torrentHash: String, limit: Int) = viewModelScope.launch {
        when (val result = repository.setDownloadSpeedLimit(serverId, torrentHash, limit)) {
            is RequestResult.Success -> {
                eventChannel.send(Event.DownloadSpeedLimitUpdated)
            }
            is RequestResult.Error -> {
                eventChannel.send(Event.Error(result))
            }
        }
    }

    fun setUploadSpeedLimit(serverId: Int, torrentHash: String, limit: Int) = viewModelScope.launch {
        when (val result = repository.setUploadSpeedLimit(serverId, torrentHash, limit)) {
            is RequestResult.Success -> {
                eventChannel.send(Event.DownloadSpeedLimitUpdated)
            }
            is RequestResult.Error -> {
                eventChannel.send(Event.Error(result))
            }
        }
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

    fun setLocation(serverId: Int, torrentHash: String, location: String) = viewModelScope.launch {
        when (val result = repository.setLocation(serverId, torrentHash, location)) {
            is RequestResult.Success -> {
                eventChannel.send(Event.LocationUpdated)
            }
            is RequestResult.Error -> {
                eventChannel.send(Event.Error(result))
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

    fun setShareLimit(serverId: Int, torrentHash: String, ratioLimit: Double, seedingTimeLimit: Int) =
        viewModelScope.launch {
            when (val result = repository.setShareLimit(serverId, torrentHash, ratioLimit, seedingTimeLimit)) {
                is RequestResult.Success -> {
                    eventChannel.send(Event.ShareLimitUpdated)
                }
                is RequestResult.Error -> {
                    eventChannel.send(Event.Error(result))
                }
            }
        }

    sealed class Event {
        data class Error(val error: RequestResult.Error) : Event()
        object TorrentNotFound : Event()
        object TorrentDeleted : Event()
        object TorrentPaused : Event()
        object TorrentResumed : Event()
        object TorrentRechecked : Event()
        object TorrentReannounced : Event()
        object TorrentRenamed : Event()
        object LocationUpdated : Event()
        object SequentialDownloadToggled : Event()
        object PrioritizeFirstLastPiecesToggled : Event()
        data class AutomaticTorrentManagementChanged(val isEnabled: Boolean) : Event()
        object DownloadSpeedLimitUpdated : Event()
        object UploadSpeedLimitUpdated : Event()
        data class ForceStartChanged(val isEnabled: Boolean) : Event()
        data class SuperSeedingChanged(val isEnabled: Boolean) : Event()
        object CategoryUpdated : Event()
        object TagsUpdated : Event()
        object ShareLimitUpdated : Event()
    }
}
