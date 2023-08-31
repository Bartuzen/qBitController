package dev.bartuzen.qbitcontroller.ui.torrent.tabs.peers

import android.annotation.SuppressLint
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.ImageLoader
import coil.imageLoader
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.bartuzen.qbitcontroller.data.ServerManager
import dev.bartuzen.qbitcontroller.data.SettingsManager
import dev.bartuzen.qbitcontroller.data.repositories.torrent.TorrentPeersRepository
import dev.bartuzen.qbitcontroller.model.TorrentPeer
import dev.bartuzen.qbitcontroller.network.RequestManager
import dev.bartuzen.qbitcontroller.network.RequestResult
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
@SuppressLint("StaticFieldLeak")
class TorrentPeersViewModel
@Inject constructor(
    settingsManager: SettingsManager,
    private val repository: TorrentPeersRepository,
    private val serverManager: ServerManager,
    private val requestManager: RequestManager,
    @ApplicationContext private val context: Context
) : ViewModel() {
    private val _torrentPeers = MutableStateFlow<List<TorrentPeer>?>(null)
    val torrentPeers = _torrentPeers.asStateFlow()

    private val eventChannel = Channel<Event>()
    val eventFlow = eventChannel.receiveAsFlow()

    private val _isNaturalLoading = MutableStateFlow<Boolean?>(null)
    val isNaturalLoading = _isNaturalLoading.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    var isInitialLoadStarted = false

    val autoRefreshInterval = settingsManager.autoRefreshInterval.flow
    val autoRefreshHideLoadingBar = settingsManager.autoRefreshHideLoadingBar.flow

    private var imageLoader: ImageLoader? = null

    private fun updatePeers(serverId: Int, torrentHash: String) = viewModelScope.launch {
        when (val result = repository.getPeers(serverId, torrentHash)) {
            is RequestResult.Success -> {
                _torrentPeers.value = result.data.peers.values.toList()
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

    fun loadPeers(serverId: Int, torrentHash: String, autoRefresh: Boolean = false) {
        if (isNaturalLoading.value == null) {
            _isNaturalLoading.value = !autoRefresh
            updatePeers(serverId, torrentHash).invokeOnCompletion {
                _isNaturalLoading.value = null
            }
        }
    }

    fun refreshPeers(serverId: Int, torrentHash: String) {
        if (!isRefreshing.value) {
            _isRefreshing.value = true
            updatePeers(serverId, torrentHash).invokeOnCompletion {
                _isRefreshing.value = false
            }
        }
    }

    fun addPeers(serverId: Int, torrentHash: String, peers: List<String>) = viewModelScope.launch {
        when (val result = repository.addPeers(serverId, torrentHash, peers)) {
            is RequestResult.Success -> {
                eventChannel.send(Event.PeersAdded)
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

    fun banPeers(serverId: Int, peers: List<String>) = viewModelScope.launch {
        when (val result = repository.banPeers(serverId, peers)) {
            is RequestResult.Success -> {
                eventChannel.send(Event.PeersBanned)
            }
            is RequestResult.Error -> {
                eventChannel.send(Event.Error(result))
            }
        }
    }

    fun getFlagUrl(serverId: Int, countryCode: String) =
        "${serverManager.getServer(serverId).url}images/flags/$countryCode.svg"

    fun getImageLoader(serverId: Int) = imageLoader.let { imageLoader ->
        if (imageLoader == null) {
            val loader = context.imageLoader.newBuilder()
                .okHttpClient(requestManager.getOkHttpClient(serverId))
                .build()
            this@TorrentPeersViewModel.imageLoader = loader
            loader
        } else {
            imageLoader
        }
    }

    sealed class Event {
        data class Error(val error: RequestResult.Error) : Event()
        data object TorrentNotFound : Event()
        data object PeersInvalid : Event()
        data object PeersAdded : Event()
        data object PeersBanned : Event()
    }
}
