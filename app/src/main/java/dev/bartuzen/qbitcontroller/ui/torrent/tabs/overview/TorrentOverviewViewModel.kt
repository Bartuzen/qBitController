package dev.bartuzen.qbitcontroller.ui.torrent.tabs.overview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.bartuzen.qbitcontroller.data.repositories.TorrentRepository
import dev.bartuzen.qbitcontroller.model.ServerConfig
import dev.bartuzen.qbitcontroller.model.Torrent
import dev.bartuzen.qbitcontroller.model.TorrentProperties
import dev.bartuzen.qbitcontroller.network.RequestError
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
    private val repository: TorrentRepository
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

    private fun updateTorrent(serverConfig: ServerConfig, torrentHash: String) =
        viewModelScope.launch {
            val torrentDeferred = async {
                when (val result = repository.getTorrent(serverConfig, torrentHash)) {
                    is RequestResult.Success -> {
                        result.data
                    }
                    is RequestResult.Error -> {
                        eventChannel.send(Event.Error(result.error))
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
                        eventChannel.send(Event.Error(result.error))
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

    fun deleteTorrent(serverConfig: ServerConfig, torrentHash: String, deleteFiles: Boolean) =
        viewModelScope.launch {
            when (val result = repository.deleteTorrent(serverConfig, torrentHash, deleteFiles)) {
                is RequestResult.Success -> {
                    eventChannel.send(Event.TorrentDeleted)
                }
                is RequestResult.Error -> {
                    eventChannel.send(Event.Error(result.error))
                }
            }
        }

    fun pauseTorrent(serverConfig: ServerConfig, torrentHash: String) = viewModelScope.launch {
        when (val result = repository.pauseTorrent(serverConfig, torrentHash)) {
            is RequestResult.Success -> {
                eventChannel.send(Event.TorrentPaused)
            }
            is RequestResult.Error -> {
                eventChannel.send(Event.Error(result.error))
            }
        }
    }

    fun resumeTorrent(serverConfig: ServerConfig, torrentHash: String) = viewModelScope.launch {
        when (val result = repository.resumeTorrent(serverConfig, torrentHash)) {
            is RequestResult.Success -> {
                eventChannel.send(Event.TorrentResumed)
            }
            is RequestResult.Error -> {
                eventChannel.send(Event.Error(result.error))
            }
        }
    }

    fun toggleSequentialDownload(serverConfig: ServerConfig, torrentHash: String) =
        viewModelScope.launch {
            when (val result = repository.toggleSequentialDownload(serverConfig, torrentHash)) {
                is RequestResult.Success -> {
                    eventChannel.send(Event.SequentialDownloadToggled)
                }
                is RequestResult.Error -> {
                    eventChannel.send(Event.Error(result.error))
                }
            }
        }

    fun togglePrioritizeFirstLastPiecesDownload(serverConfig: ServerConfig, torrentHash: String) =
        viewModelScope.launch {
            when (val result =
                repository.togglePrioritizeFirstLastPiecesDownload(serverConfig, torrentHash)) {
                is RequestResult.Success -> {
                    eventChannel.send(Event.PrioritizeFirstLastPiecesToggled)
                }
                is RequestResult.Error -> {
                    eventChannel.send(Event.Error(result.error))
                }
            }
        }

    fun setAutomaticTorrentManagement(
        serverConfig: ServerConfig, torrentHash: String, enable: Boolean
    ) = viewModelScope.launch {
        when (
            val result = repository.setAutomaticTorrentManagement(serverConfig, torrentHash, enable)
        ) {
            is RequestResult.Success -> {
                eventChannel.send(Event.AutomaticTorrentManagementChanged(enable))
            }
            is RequestResult.Error -> {
                eventChannel.send(Event.Error(result.error))
            }
        }
    }

    fun setDownloadSpeedLimit(serverConfig: ServerConfig, torrentHash: String, limit: Int) =
        viewModelScope.launch {
            when (
                val result = repository.setDownloadSpeedLimit(serverConfig, torrentHash, limit)
            ) {
                is RequestResult.Success -> {
                    eventChannel.send(Event.DownloadSpeedLimitUpdated)
                }
                is RequestResult.Error -> {
                    eventChannel.send(Event.Error(result.error))
                }
            }
        }

    fun setUploadSpeedLimit(serverConfig: ServerConfig, torrentHash: String, limit: Int) =
        viewModelScope.launch {
            when (
                val result = repository.setUploadSpeedLimit(serverConfig, torrentHash, limit)
            ) {
                is RequestResult.Success -> {
                    eventChannel.send(Event.DownloadSpeedLimitUpdated)
                }
                is RequestResult.Error -> {
                    eventChannel.send(Event.Error(result.error))
                }
            }
        }

    fun setForceStart(serverConfig: ServerConfig, torrentHash: String, value: Boolean) =
        viewModelScope.launch {
            when (val result = repository.setForceStart(serverConfig, torrentHash, value)) {
                is RequestResult.Success -> {
                    eventChannel.send(Event.ForceStartChanged(value))
                }
                is RequestResult.Error -> {
                    eventChannel.send(Event.Error(result.error))
                }
            }
        }

    fun setSuperSeeding(serverConfig: ServerConfig, torrentHash: String, value: Boolean) =
        viewModelScope.launch {
            when (val result = repository.setSuperSeeding(serverConfig, torrentHash, value)) {
                is RequestResult.Success -> {
                    eventChannel.send(Event.SuperSeedingChanged(value))
                }
                is RequestResult.Error -> {
                    eventChannel.send(Event.Error(result.error))
                }
            }
        }

    sealed class Event {
        data class Error(val error: RequestError) : Event()
        object TorrentDeleted : Event()
        object TorrentPaused : Event()
        object TorrentResumed : Event()
        object SequentialDownloadToggled : Event()
        object PrioritizeFirstLastPiecesToggled : Event()
        data class AutomaticTorrentManagementChanged(val isEnabled: Boolean) : Event()
        object DownloadSpeedLimitUpdated : Event()
        object UploadSpeedLimitUpdated : Event()
        data class ForceStartChanged(val isEnabled: Boolean) : Event()
        data class SuperSeedingChanged(val isEnabled: Boolean) : Event()
    }
}
