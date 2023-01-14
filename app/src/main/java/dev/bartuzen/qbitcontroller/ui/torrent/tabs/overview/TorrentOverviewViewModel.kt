package dev.bartuzen.qbitcontroller.ui.torrent.tabs.overview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.bartuzen.qbitcontroller.data.SettingsManager
import dev.bartuzen.qbitcontroller.data.repositories.torrent.TorrentOverviewRepository
import dev.bartuzen.qbitcontroller.model.ServerConfig
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

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    var isInitialLoadStarted = false

    val autoRefreshInterval = settingsManager.autoRefreshInterval.flow

    private fun updateTorrent(serverConfig: ServerConfig, torrentHash: String) = viewModelScope.launch {
        val torrentDeferred = async {
            when (val result = repository.getTorrent(serverConfig, torrentHash)) {
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
            when (val result = repository.getProperties(serverConfig, torrentHash)) {
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

    fun loadTorrent(serverConfig: ServerConfig, torrentHash: String) {
        if (!isLoading.value) {
            _isLoading.value = true
            updateTorrent(serverConfig, torrentHash).invokeOnCompletion {
                _isLoading.value = false
            }
        }
    }

    fun refreshTorrent(serverConfig: ServerConfig, torrentHash: String) {
        if (!isRefreshing.value) {
            _isRefreshing.value = true
            updateTorrent(serverConfig, torrentHash).invokeOnCompletion {
                _isRefreshing.value = false
            }
        }
    }

    fun deleteTorrent(serverConfig: ServerConfig, torrentHash: String, deleteFiles: Boolean) = viewModelScope.launch {
        when (val result = repository.deleteTorrent(serverConfig, torrentHash, deleteFiles)) {
            is RequestResult.Success -> {
                eventChannel.send(Event.TorrentDeleted)
            }
            is RequestResult.Error -> {
                eventChannel.send(Event.Error(result))
            }
        }
    }

    fun pauseTorrent(serverConfig: ServerConfig, torrentHash: String) = viewModelScope.launch {
        when (val result = repository.pauseTorrent(serverConfig, torrentHash)) {
            is RequestResult.Success -> {
                eventChannel.send(Event.TorrentPaused)
            }
            is RequestResult.Error -> {
                eventChannel.send(Event.Error(result))
            }
        }
    }

    fun resumeTorrent(serverConfig: ServerConfig, torrentHash: String) = viewModelScope.launch {
        when (val result = repository.resumeTorrent(serverConfig, torrentHash)) {
            is RequestResult.Success -> {
                eventChannel.send(Event.TorrentResumed)
            }
            is RequestResult.Error -> {
                eventChannel.send(Event.Error(result))
            }
        }
    }

    fun toggleSequentialDownload(serverConfig: ServerConfig, torrentHash: String) = viewModelScope.launch {
        when (val result = repository.toggleSequentialDownload(serverConfig, torrentHash)) {
            is RequestResult.Success -> {
                eventChannel.send(Event.SequentialDownloadToggled)
            }
            is RequestResult.Error -> {
                eventChannel.send(Event.Error(result))
            }
        }
    }

    fun togglePrioritizeFirstLastPiecesDownload(serverConfig: ServerConfig, torrentHash: String) = viewModelScope.launch {
        when (val result = repository.togglePrioritizeFirstLastPiecesDownload(serverConfig, torrentHash)) {
            is RequestResult.Success -> {
                eventChannel.send(Event.PrioritizeFirstLastPiecesToggled)
            }
            is RequestResult.Error -> {
                eventChannel.send(Event.Error(result))
            }
        }
    }

    fun setAutomaticTorrentManagement(serverConfig: ServerConfig, torrentHash: String, enable: Boolean) =
        viewModelScope.launch {
            when (val result = repository.setAutomaticTorrentManagement(serverConfig, torrentHash, enable)) {
                is RequestResult.Success -> {
                    eventChannel.send(Event.AutomaticTorrentManagementChanged(enable))
                }
                is RequestResult.Error -> {
                    eventChannel.send(Event.Error(result))
                }
            }
        }

    fun setDownloadSpeedLimit(serverConfig: ServerConfig, torrentHash: String, limit: Int) = viewModelScope.launch {
        when (val result = repository.setDownloadSpeedLimit(serverConfig, torrentHash, limit)) {
            is RequestResult.Success -> {
                eventChannel.send(Event.DownloadSpeedLimitUpdated)
            }
            is RequestResult.Error -> {
                eventChannel.send(Event.Error(result))
            }
        }
    }

    fun setUploadSpeedLimit(serverConfig: ServerConfig, torrentHash: String, limit: Int) = viewModelScope.launch {
        when (val result = repository.setUploadSpeedLimit(serverConfig, torrentHash, limit)) {
            is RequestResult.Success -> {
                eventChannel.send(Event.DownloadSpeedLimitUpdated)
            }
            is RequestResult.Error -> {
                eventChannel.send(Event.Error(result))
            }
        }
    }

    fun setForceStart(serverConfig: ServerConfig, torrentHash: String, value: Boolean) = viewModelScope.launch {
        when (val result = repository.setForceStart(serverConfig, torrentHash, value)) {
            is RequestResult.Success -> {
                eventChannel.send(Event.ForceStartChanged(value))
            }
            is RequestResult.Error -> {
                eventChannel.send(Event.Error(result))
            }
        }
    }

    fun setSuperSeeding(serverConfig: ServerConfig, torrentHash: String, value: Boolean) = viewModelScope.launch {
        when (val result = repository.setSuperSeeding(serverConfig, torrentHash, value)) {
            is RequestResult.Success -> {
                eventChannel.send(Event.SuperSeedingChanged(value))
            }
            is RequestResult.Error -> {
                eventChannel.send(Event.Error(result))
            }
        }
    }

    fun recheckTorrent(serverConfig: ServerConfig, torrentHash: String) = viewModelScope.launch {
        when (val result = repository.recheckTorrent(serverConfig, torrentHash)) {
            is RequestResult.Success -> {
                eventChannel.send(Event.TorrentRechecked)
            }
            is RequestResult.Error -> {
                eventChannel.send(Event.Error(result))
            }
        }
    }

    fun reannounceTorrent(serverConfig: ServerConfig, torrentHash: String) = viewModelScope.launch {
        when (val result = repository.reannounceTorrent(serverConfig, torrentHash)) {
            is RequestResult.Success -> {
                eventChannel.send(Event.TorrentReannounced)
            }
            is RequestResult.Error -> {
                eventChannel.send(Event.Error(result))
            }
        }
    }

    fun renameTorrent(serverConfig: ServerConfig, torrentHash: String, name: String) = viewModelScope.launch {
        when (val result = repository.renameTorrent(serverConfig, torrentHash, name)) {
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

    fun setCategory(serverConfig: ServerConfig, torrentHash: String, category: String?) = viewModelScope.launch {
        when (val result = repository.setCategory(serverConfig, torrentHash, category)) {
            is RequestResult.Success -> {
                eventChannel.send(Event.CategoryUpdated)
            }
            is RequestResult.Error -> {
                eventChannel.send(Event.Error(result))
            }
        }
    }

    fun setTags(serverConfig: ServerConfig, torrentHash: String, newTags: List<String>) = viewModelScope.launch {
        val currentTags = torrent.value?.tags ?: emptyList()

        val addedTags = newTags.filter { it !in currentTags }
        val removedTags = currentTags.filter { it !in newTags }

        val addedTagsDeferred = async {
            if (addedTags.isNotEmpty()) {
                when (val result = repository.addTags(serverConfig, torrentHash, addedTags)) {
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
                when (val result = repository.removeTags(serverConfig, torrentHash, removedTags)) {
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

    fun setShareLimit(serverConfig: ServerConfig, torrentHash: String, ratioLimit: Double, seedingTimeLimit: Int) =
        viewModelScope.launch {
            when (val result = repository.setShareLimit(serverConfig, torrentHash, ratioLimit, seedingTimeLimit)) {
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
