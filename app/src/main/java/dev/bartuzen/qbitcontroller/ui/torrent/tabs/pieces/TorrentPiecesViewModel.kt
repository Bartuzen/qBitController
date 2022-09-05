package dev.bartuzen.qbitcontroller.ui.torrent.tabs.pieces

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.bartuzen.qbitcontroller.data.repositories.TorrentRepository
import dev.bartuzen.qbitcontroller.model.PieceState
import dev.bartuzen.qbitcontroller.model.ServerConfig
import dev.bartuzen.qbitcontroller.model.TorrentProperties
import dev.bartuzen.qbitcontroller.network.RequestError
import dev.bartuzen.qbitcontroller.network.RequestResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TorrentPiecesViewModel @Inject constructor(
    private val repository: TorrentRepository
) : ViewModel() {
    val torrentPieces = MutableStateFlow<List<PieceState>?>(null)
    val torrentProperties = MutableStateFlow<TorrentProperties?>(null)

    private val eventChannel = Channel<Event>()
    val eventFlow = eventChannel.receiveAsFlow()

    val isLoading = MutableStateFlow(true)
    var isInitialLoadStarted = false

    fun updatePieces(serverConfig: ServerConfig, torrentHash: String) = viewModelScope.launch {
        val piecesDeferred = async {
            when (val result = repository.getPieces(serverConfig, torrentHash)) {
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

        val pieces = piecesDeferred.await()
        val properties = propertiesDeferred.await()

        torrentPieces.value = pieces
        torrentProperties.value = properties
    }

    sealed class Event {
        data class Error(val error: RequestError) : Event()
    }
}