package dev.bartuzen.qbitcontroller.ui.torrent.tabs.trackers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.bartuzen.qbitcontroller.data.repositories.TorrentRepository
import dev.bartuzen.qbitcontroller.model.ServerConfig
import dev.bartuzen.qbitcontroller.model.TorrentTracker
import dev.bartuzen.qbitcontroller.network.RequestError
import dev.bartuzen.qbitcontroller.network.RequestResult
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TorrentTrackersViewModel @Inject constructor(
    private val repository: TorrentRepository
) : ViewModel() {
    private val _torrentTrackers = MutableStateFlow<List<TorrentTracker>?>(null)
    val torrentTrackers = _torrentTrackers.asStateFlow()

    private val eventChannel = Channel<Event>()
    val eventFlow = eventChannel.receiveAsFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    var isInitialLoadStarted = false

    private fun updateTrackers(serverConfig: ServerConfig, torrentHash: String) =
        viewModelScope.launch {
            when (val result = repository.getTrackers(serverConfig, torrentHash)) {
                is RequestResult.Success -> {
                    _torrentTrackers.value = result.data
                }
                is RequestResult.Error -> {
                    eventChannel.send(Event.Error(result.error))
                }
            }
        }

    fun loadTrackers(serverConfig: ServerConfig, torrentHash: String) {
        if (!isLoading.value) {
            _isLoading.value = true
            updateTrackers(serverConfig, torrentHash).invokeOnCompletion {
                _isLoading.value = false
            }
        }
    }

    fun refreshTrackers(serverConfig: ServerConfig, torrentHash: String) {
        if (!isRefreshing.value) {
            _isRefreshing.value = true
            updateTrackers(serverConfig, torrentHash).invokeOnCompletion {
                _isRefreshing.value = false
            }
        }
    }

    fun addTrackers(serverConfig: ServerConfig, hash: String, urls: List<String>) =
        viewModelScope.launch {
            when (val result = repository.addTrackers(serverConfig, hash, urls)) {
                is RequestResult.Success -> {
                    eventChannel.send(Event.TrackersAdded)
                }
                is RequestResult.Error -> {
                    eventChannel.send(Event.Error(result.error))
                }
            }
        }

    fun deleteTrackers(serverConfig: ServerConfig, hash: String, urls: List<String>) =
        viewModelScope.launch {
            when (val result = repository.deleteTrackers(serverConfig, hash, urls)) {
                is RequestResult.Success -> {
                    eventChannel.send(Event.TrackersDeleted)
                }
                is RequestResult.Error -> {
                    eventChannel.send(Event.Error(result.error))
                }
            }
        }

    sealed class Event {
        data class Error(val error: RequestError) : Event()
        object TrackersAdded : Event()
        object TrackersDeleted : Event()
    }
}
