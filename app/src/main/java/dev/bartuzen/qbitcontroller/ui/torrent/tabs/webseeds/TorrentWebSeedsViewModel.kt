package dev.bartuzen.qbitcontroller.ui.torrent.tabs.webseeds

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.bartuzen.qbitcontroller.data.SettingsManager
import dev.bartuzen.qbitcontroller.data.repositories.torrent.TorrentWebSeedsRepository
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

class TorrentWebSeedsViewModel(
    private val serverId: Int,
    private val torrentHash: String,
    settingsManager: SettingsManager,
    private val repository: TorrentWebSeedsRepository,
) : ViewModel() {
    private val _webSeeds = MutableStateFlow<List<String>?>(null)
    val webSeeds = _webSeeds.asStateFlow()

    private val eventChannel = Channel<Event>()
    val eventFlow = eventChannel.receiveAsFlow()

    private val _isNaturalLoading = MutableStateFlow<Boolean?>(null)
    val isNaturalLoading = _isNaturalLoading.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    val autoRefreshInterval = settingsManager.autoRefreshInterval.flow

    private val isScreenActive = MutableStateFlow(false)

    init {
        loadWebSeeds()

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
                    loadWebSeeds(autoRefresh = true)
                }
            }
        }
    }

    fun setScreenActive(isScreenActive: Boolean) {
        this.isScreenActive.value = isScreenActive
    }

    private fun updateWebSeeds() = viewModelScope.launch {
        when (val result = repository.getWebSeeds(serverId, torrentHash)) {
            is RequestResult.Success -> {
                _webSeeds.value = result.data.map { it.url }
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

    private fun loadWebSeeds(autoRefresh: Boolean = false) {
        if (isNaturalLoading.value == null) {
            _isNaturalLoading.value = !autoRefresh
            updateWebSeeds().invokeOnCompletion {
                _isNaturalLoading.value = null
            }
        }
    }

    fun refreshWebSeeds() {
        if (!isRefreshing.value) {
            _isRefreshing.value = true
            updateWebSeeds().invokeOnCompletion {
                viewModelScope.launch {
                    delay(25)
                    _isRefreshing.value = false
                }
            }
        }
    }

    sealed class Event {
        data class Error(val error: RequestResult.Error) : Event()
        data object TorrentNotFound : Event()
    }
}
