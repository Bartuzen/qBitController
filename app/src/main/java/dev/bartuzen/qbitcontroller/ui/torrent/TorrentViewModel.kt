package dev.bartuzen.qbitcontroller.ui.torrent

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.bartuzen.qbitcontroller.data.repositories.TorrentRepository
import dev.bartuzen.qbitcontroller.model.*
import dev.bartuzen.qbitcontroller.network.RequestError
import dev.bartuzen.qbitcontroller.network.RequestResult
import dev.bartuzen.qbitcontroller.ui.common.StateDelegate
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TorrentViewModel @Inject constructor(
    private val repository: TorrentRepository,
    state: SavedStateHandle
) : ViewModel() {
    val torrent = MutableStateFlow<Torrent?>(null)
    val torrentFiles = MutableStateFlow<List<TorrentFile>?>(null)
    val torrentPieces = MutableStateFlow<List<PieceState>?>(null)
    val torrentProperties = MutableStateFlow<TorrentProperties?>(null)

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

    val isTorrentLoading = MutableStateFlow(true)
    val isTorrentFilesLoading = MutableStateFlow(true)
    val isTorrentPiecesLoading = MutableStateFlow(true)

    fun updateTorrent() = viewModelScope.launch {
        serverConfig?.let { config ->
            when (val result = repository.getTorrent(config, torrentHash ?: "")) {
                is RequestResult.Success -> if (result.data.size == 1) {
                    torrent.value = result.data.first()
                }
                is RequestResult.Error -> torrentOverviewEventChannel.send(
                    TorrentOverviewEvent.OnError(result.error)
                )
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

    fun updatePiecesAndProperties() = viewModelScope.launch {
        serverConfig?.let { config ->
            when (val piecesResult = repository.getPieces(config, torrentHash ?: "")) {
                is RequestResult.Success -> {
                    when (val propertiesResult =
                        repository.getProperties(config, torrentHash ?: "")) {
                        is RequestResult.Success -> {
                            torrentPieces.value = piecesResult.data
                            torrentProperties.value = propertiesResult.data
                        }
                        is RequestResult.Error -> torrentPiecesEventChannel.send(
                            TorrentPiecesEvent.OnError(propertiesResult.error)
                        )
                    }
                }
                is RequestResult.Error -> torrentPiecesEventChannel.send(
                    TorrentPiecesEvent.OnError(piecesResult.error)
                )
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
            when (val result = repository.resumeTorrent(config, torrentHash ?: "")) {
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