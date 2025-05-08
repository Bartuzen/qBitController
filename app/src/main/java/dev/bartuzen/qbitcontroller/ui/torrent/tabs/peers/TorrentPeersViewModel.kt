package dev.bartuzen.qbitcontroller.ui.torrent.tabs.peers

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil3.ImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.bartuzen.qbitcontroller.data.ServerManager
import dev.bartuzen.qbitcontroller.data.SettingsManager
import dev.bartuzen.qbitcontroller.data.repositories.torrent.TorrentPeersRepository
import dev.bartuzen.qbitcontroller.model.TorrentPeer
import dev.bartuzen.qbitcontroller.network.RequestManager
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

@HiltViewModel(assistedFactory = TorrentPeersViewModel.Factory::class)
class TorrentPeersViewModel @AssistedInject constructor(
    @Assisted private val serverId: Int,
    @Assisted private val torrentHash: String,
    settingsManager: SettingsManager,
    private val repository: TorrentPeersRepository,
    private val serverManager: ServerManager,
    private val requestManager: RequestManager,
    @ApplicationContext private val context: Context,
) : ViewModel() {
    @AssistedFactory
    interface Factory {
        fun create(serverId: Int, torrentHash: String): TorrentPeersViewModel
    }

    private val _peers = MutableStateFlow<List<TorrentPeer>?>(null)
    val peers = _peers.asStateFlow()

    private val eventChannel = Channel<Event>()
    val eventFlow = eventChannel.receiveAsFlow()

    private val _isNaturalLoading = MutableStateFlow<Boolean?>(null)
    val isNaturalLoading = _isNaturalLoading.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    private val autoRefreshInterval = settingsManager.autoRefreshInterval.flow

    private val isScreenActive = MutableStateFlow(false)

    val imageLoader: ImageLoader = ImageLoader.Builder(context)
        .components {
            add(
                OkHttpNetworkFetcherFactory(
                    callFactory = requestManager.getOkHttpClient(serverId),
                ),
            )
        }.build()

    init {
        loadPeers()

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
                    loadPeers(autoRefresh = true)
                }
            }
        }
    }

    fun setScreenActive(isScreenActive: Boolean) {
        this.isScreenActive.value = isScreenActive
    }

    private fun updatePeers() = viewModelScope.launch {
        when (val result = repository.getPeers(serverId, torrentHash)) {
            is RequestResult.Success -> {
                _peers.value = result.data.peers.values.toList()
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

    private fun loadPeers(autoRefresh: Boolean = false) {
        if (isNaturalLoading.value == null) {
            _isNaturalLoading.value = !autoRefresh
            updatePeers().invokeOnCompletion {
                _isNaturalLoading.value = null
            }
        }
    }

    fun refreshPeers() {
        if (!isRefreshing.value) {
            _isRefreshing.value = true
            updatePeers().invokeOnCompletion {
                viewModelScope.launch {
                    delay(25)
                    _isRefreshing.value = false
                }
            }
        }
    }

    fun addPeers(peers: List<String>) = viewModelScope.launch {
        when (val result = repository.addPeers(serverId, torrentHash, peers)) {
            is RequestResult.Success -> {
                eventChannel.send(Event.PeersAdded)
                launch {
                    delay(1000)
                    loadPeers()
                }
            }
            is RequestResult.Error -> {
                if (result is RequestResult.Error.ApiError && result.code == 400) {
                    eventChannel.send(Event.PeersInvalid)
                } else {
                    eventChannel.send(Event.Error(result))
                }
            }
        }
    }

    fun banPeers(peers: List<String>) = viewModelScope.launch {
        when (val result = repository.banPeers(serverId, peers)) {
            is RequestResult.Success -> {
                eventChannel.send(Event.PeersBanned)
                loadPeers()
            }
            is RequestResult.Error -> {
                eventChannel.send(Event.Error(result))
            }
        }
    }

    fun getFlagUrl(countryCode: String) = "${serverManager.getServer(serverId).requestUrl}images/flags/$countryCode.svg"

    sealed class Event {
        data class Error(val error: RequestResult.Error) : Event()
        data object TorrentNotFound : Event()
        data object PeersInvalid : Event()
        data object PeersAdded : Event()
        data object PeersBanned : Event()
    }
}
