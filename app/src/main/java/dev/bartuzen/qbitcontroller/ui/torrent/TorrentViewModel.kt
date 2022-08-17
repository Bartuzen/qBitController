package dev.bartuzen.qbitcontroller.ui.torrent

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.bartuzen.qbitcontroller.data.repositories.TorrentRepository
import dev.bartuzen.qbitcontroller.model.*
import dev.bartuzen.qbitcontroller.network.RequestError
import dev.bartuzen.qbitcontroller.network.RequestResult
import dev.bartuzen.qbitcontroller.ui.common.SettableLiveData
import dev.bartuzen.qbitcontroller.ui.common.StateDelegate
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TorrentViewModel @Inject constructor(
    private val repository: TorrentRepository,
    state: SavedStateHandle
) : ViewModel() {
    val torrent = SettableLiveData<Torrent>()
    val torrentFiles = SettableLiveData<List<TorrentFile>>()
    val torrentPieces = SettableLiveData<List<PieceState>>()
    val torrentProperties = SettableLiveData<TorrentProperties>()

    var torrentHash: String? by StateDelegate(state, "torrent_hash")
    var serverConfig: ServerConfig? by StateDelegate(state, "server_config")

    private val torrentActivityEventChannel = Channel<TorrentActivityEvent>()
    val torrentActivityEvent = torrentActivityEventChannel.receiveAsFlow()

    private val torrentOverviewEventChannel = Channel<TorrentOverviewEvent>()
    val torrentOverviewEvent = torrentOverviewEventChannel.receiveAsFlow()

    private val torrentFilesEventChannel = Channel<TorrentFilesEvent>()
    val torrentFilesEvent = torrentFilesEventChannel.receiveAsFlow()

    private val torrentPiecesEventChannel = Channel<TorrentPiecesEvent>()
    val torrentPiecesEvent = torrentPiecesEventChannel.receiveAsFlow()

    fun updateTorrent() = viewModelScope.launch {
        serverConfig?.let { config ->
            when (val result = repository.getTorrent(config, torrentHash ?: "")) {
                is RequestResult.Success -> {
                    if (result.data.size == 1) {
                        torrent.value = result.data.first()
                    } else if (torrent.value == null) {
                        torrent.isSet = true
                    }
                }
                is RequestResult.Error -> {
                    torrentOverviewEventChannel.send(TorrentOverviewEvent.OnError(result.error))
                }
            }
        }
    }

    fun updateFiles() = viewModelScope.launch {
        serverConfig?.let { config ->
            when (val result = repository.getFiles(config, torrentHash ?: "")) {
                is RequestResult.Success -> torrentFiles.value = result.data
                is RequestResult.Error -> torrentFilesEventChannel.send(
                    TorrentFilesEvent.OnError(result.error)
                )
            }
        }
    }

    fun updatePieces() = viewModelScope.launch {
        serverConfig?.let { config ->
            when (val result = repository.getPieces(config, torrentHash ?: "")) {
                is RequestResult.Success -> torrentPieces.value = result.data
                is RequestResult.Error -> torrentPiecesEventChannel.send(
                    TorrentPiecesEvent.OnError(result.error)
                )
            }
        }
    }

    fun updateProperties() = viewModelScope.launch {
        serverConfig?.let { config ->
            when (val result = repository.getProperties(config, torrentHash ?: "")) {
                is RequestResult.Success -> torrentProperties.value = result.data
                is RequestResult.Error -> {}
            }
        }
    }

    fun pauseTorrent() = viewModelScope.launch {
        serverConfig?.let { config ->
            when (val result = repository.pauseTorrent(config, torrentHash ?: "")) {
                is RequestResult.Success -> torrentActivityEventChannel.send(
                    TorrentActivityEvent.OnTorrentPause
                )
                is RequestResult.Error -> torrentActivityEventChannel.send(
                    TorrentActivityEvent.OnError(result.error)
                )
            }
        }
    }

    fun resumeTorrent() = viewModelScope.launch {
        serverConfig?.let { config ->
            when(val result = repository.resumeTorrent(config, torrentHash ?: "")) {
                is RequestResult.Success -> torrentActivityEventChannel.send(
                    TorrentActivityEvent.OnTorrentResume
                )
                is RequestResult.Error -> torrentActivityEventChannel.send(
                    TorrentActivityEvent.OnError(result.error)
                )
            }
        }
    }

    sealed class TorrentActivityEvent {
        data class OnError(val error: RequestError) : TorrentActivityEvent()
        object OnTorrentPause : TorrentActivityEvent()
        object OnTorrentResume : TorrentActivityEvent()
    }

    sealed class TorrentOverviewEvent {
        data class OnError(val error: RequestError) : TorrentOverviewEvent()
    }

    sealed class TorrentFilesEvent {
        data class OnError(val error: RequestError) : TorrentFilesEvent()
    }

    sealed class TorrentPiecesEvent {
        data class OnError(val error: RequestError) : TorrentPiecesEvent()
    }
}