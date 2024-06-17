package dev.bartuzen.qbitcontroller.ui.torrent.tabs.webseeds

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.bartuzen.qbitcontroller.data.SettingsManager
import dev.bartuzen.qbitcontroller.data.repositories.torrent.TorrentWebSeedsRepository
import dev.bartuzen.qbitcontroller.network.RequestResult
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TorrentWebSeedsViewModel @Inject constructor(
    settingsManager: SettingsManager,
    private val repository: TorrentWebSeedsRepository
) : ViewModel() {
    private val _webSeeds = MutableStateFlow<List<String>?>(null)
    val webSeeds = _webSeeds.asStateFlow()

    private val eventChannel = Channel<Event>()
    val eventFlow = eventChannel.receiveAsFlow()

    private val _isNaturalLoading = MutableStateFlow<Boolean?>(null)
    val isNaturalLoading = _isNaturalLoading.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    var isInitialLoadStarted = false

    val autoRefreshInterval = settingsManager.autoRefreshInterval.flow

    private fun updateWebSeeds(serverId: Int, torrentHash: String) = viewModelScope.launch {
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

    fun loadWebSeeds(serverId: Int, torrentHash: String, autoRefresh: Boolean = false) {
        if (isNaturalLoading.value == null) {
            _isNaturalLoading.value = !autoRefresh
            updateWebSeeds(serverId, torrentHash).invokeOnCompletion {
                _isNaturalLoading.value = null
            }
        }
    }

    fun refreshWebSeeds(serverId: Int, torrentHash: String) {
        if (!isRefreshing.value) {
            _isRefreshing.value = true
            updateWebSeeds(serverId, torrentHash).invokeOnCompletion {
                _isRefreshing.value = false
            }
        }
    }

    sealed class Event {
        data class Error(val error: RequestResult.Error) : Event()
        data object TorrentNotFound : Event()
    }
}
