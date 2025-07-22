package dev.bartuzen.qbitcontroller.ui.torrent.tabs.trackers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.bartuzen.qbitcontroller.data.SettingsManager
import dev.bartuzen.qbitcontroller.data.repositories.torrent.TorrentTrackersRepository
import dev.bartuzen.qbitcontroller.model.TorrentTracker
import dev.bartuzen.qbitcontroller.network.RequestResult
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

class TorrentTrackersViewModel(
    private val serverId: Int,
    private val torrentHash: String,
    settingsManager: SettingsManager,
    private val repository: TorrentTrackersRepository,
) : ViewModel() {
    private val _trackers = MutableStateFlow<List<TorrentTracker>?>(null)
    val trackers = _trackers.asStateFlow()

    private val eventChannel = Channel<Event>()
    val eventFlow = eventChannel.receiveAsFlow()

    private val _isNaturalLoading = MutableStateFlow<Boolean?>(null)
    val isNaturalLoading = _isNaturalLoading.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    val autoRefreshInterval = settingsManager.autoRefreshInterval.flow

    private val isScreenActive = MutableStateFlow(false)

    init {
        loadTrackers()

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
                    loadTrackers(autoRefresh = true)
                }
            }
        }
    }

    fun setScreenActive(isScreenActive: Boolean) {
        this.isScreenActive.value = isScreenActive
    }

    private fun updateTrackers() = viewModelScope.launch {
        when (val result = repository.getTrackers(serverId, torrentHash)) {
            is RequestResult.Success -> {
                _trackers.value = result.data
            }
            is RequestResult.Error.ApiError if result.code == 404 -> {
                eventChannel.send(Event.TorrentNotFound)
            }
            is RequestResult.Error -> {
                eventChannel.send(Event.Error(result))
            }
        }
    }

    private fun loadTrackers(autoRefresh: Boolean = false) {
        if (isNaturalLoading.value == null) {
            _isNaturalLoading.value = !autoRefresh
            updateTrackers().invokeOnCompletion {
                _isNaturalLoading.value = null
            }
        }
    }

    fun refreshTrackers() {
        if (!isRefreshing.value) {
            _isRefreshing.value = true
            updateTrackers().invokeOnCompletion {
                viewModelScope.launch {
                    delay(25)
                    _isRefreshing.value = false
                }
            }
        }
    }

    fun addTrackers(urls: List<String>) = viewModelScope.launch {
        when (val result = repository.addTrackers(serverId, torrentHash, urls)) {
            is RequestResult.Success -> {
                eventChannel.send(Event.TrackersAdded)
                loadTrackers()
            }
            is RequestResult.Error.ApiError if result.code == 404 -> {
                eventChannel.send(Event.TorrentNotFound)
            }
            is RequestResult.Error -> {
                eventChannel.send(Event.Error(result))
            }
        }
    }

    fun deleteTrackers(urls: List<String>) = viewModelScope.launch {
        when (val result = repository.deleteTrackers(serverId, torrentHash, urls)) {
            is RequestResult.Success -> {
                eventChannel.send(Event.TrackersDeleted)
                loadTrackers()
            }
            is RequestResult.Error.ApiError if result.code == 404 -> {
                eventChannel.send(Event.TorrentNotFound)
            }
            is RequestResult.Error -> {
                eventChannel.send(Event.Error(result))
            }
        }
    }

    fun editTracker(tracker: String, newUrl: String) = viewModelScope.launch {
        when (val result = repository.editTrackers(serverId, torrentHash, tracker, newUrl)) {
            is RequestResult.Success -> {
                eventChannel.send(Event.TrackerEdited)
                loadTrackers()
            }
            is RequestResult.Error.ApiError if result.code == 404 -> {
                eventChannel.send(Event.TorrentNotFound)
            }
            is RequestResult.Error -> {
                eventChannel.send(Event.Error(result))
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
