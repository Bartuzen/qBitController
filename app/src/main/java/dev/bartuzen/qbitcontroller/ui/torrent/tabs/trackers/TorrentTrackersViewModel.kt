package dev.bartuzen.qbitcontroller.ui.torrent.tabs.trackers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.bartuzen.qbitcontroller.data.SettingsManager
import dev.bartuzen.qbitcontroller.data.repositories.torrent.TorrentTrackersRepository
import dev.bartuzen.qbitcontroller.model.TorrentTracker
import dev.bartuzen.qbitcontroller.network.RequestResult
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TorrentTrackersViewModel @Inject constructor(
    settingsManager: SettingsManager,
    private val repository: TorrentTrackersRepository,
) : ViewModel() {
    private val _torrentTrackers = MutableStateFlow<List<TorrentTracker>?>(null)
    val torrentTrackers = _torrentTrackers.asStateFlow()

    private val eventChannel = Channel<Event>()
    val eventFlow = eventChannel.receiveAsFlow()

    private val _isNaturalLoading = MutableStateFlow<Boolean?>(null)
    val isNaturalLoading = _isNaturalLoading.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    var isInitialLoadStarted = false

    val autoRefreshInterval = settingsManager.autoRefreshInterval.flow

    private fun updateTrackers(serverId: Int, torrentHash: String) = viewModelScope.launch {
        when (val result = repository.getTrackers(serverId, torrentHash)) {
            is RequestResult.Success -> {
                _torrentTrackers.value = result.data
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

    fun loadTrackers(serverId: Int, torrentHash: String, autoRefresh: Boolean = false) {
        if (isNaturalLoading.value == null) {
            _isNaturalLoading.value = !autoRefresh
            updateTrackers(serverId, torrentHash).invokeOnCompletion {
                _isNaturalLoading.value = null
            }
        }
    }

    fun refreshTrackers(serverId: Int, torrentHash: String) {
        if (!isRefreshing.value) {
            _isRefreshing.value = true
            updateTrackers(serverId, torrentHash).invokeOnCompletion {
                _isRefreshing.value = false
            }
        }
    }

    fun addTrackers(serverId: Int, hash: String, urls: List<String>) = viewModelScope.launch {
        when (val result = repository.addTrackers(serverId, hash, urls)) {
            is RequestResult.Success -> {
                eventChannel.send(Event.TrackersAdded)
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

    fun deleteTrackers(serverId: Int, hash: String, urls: List<String>) = viewModelScope.launch {
        when (val result = repository.deleteTrackers(serverId, hash, urls)) {
            is RequestResult.Success -> {
                eventChannel.send(Event.TrackersDeleted)
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

    fun editTracker(serverId: Int, hash: String, tracker: String, newUrl: String) = viewModelScope.launch {
        when (val result = repository.editTrackers(serverId, hash, tracker, newUrl)) {
            is RequestResult.Success -> {
                eventChannel.send(Event.TrackerEdited)
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

    sealed class Event {
        data class Error(val error: RequestResult.Error) : Event()
        data object TorrentNotFound : Event()
        data object TrackersAdded : Event()
        data object TrackersDeleted : Event()
        data object TrackerEdited : Event()
    }
}
