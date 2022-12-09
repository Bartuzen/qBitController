package dev.bartuzen.qbitcontroller.ui.torrent.tabs.pieces

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.bartuzen.qbitcontroller.data.repositories.TorrentRepository
import dev.bartuzen.qbitcontroller.model.PieceState
import dev.bartuzen.qbitcontroller.model.ServerConfig
import dev.bartuzen.qbitcontroller.model.TorrentProperties
import dev.bartuzen.qbitcontroller.network.RequestResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TorrentPiecesViewModel @Inject constructor(
    private val repository: TorrentRepository
) : ViewModel() {
    private val _torrentPieces = MutableStateFlow<List<PieceState>?>(null)
    val torrentPieces = _torrentPieces.asStateFlow()

    private val _torrentProperties = MutableStateFlow<TorrentProperties?>(null)
    val torrentProperties = _torrentProperties.asStateFlow()

    private val eventChannel = Channel<Event>()
    val eventFlow = eventChannel.receiveAsFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    var isInitialLoadStarted = false

    private fun updatePieces(serverConfig: ServerConfig, torrentHash: String) =
        viewModelScope.launch {
            val piecesDeferred = async {
                when (val result = repository.getPieces(serverConfig, torrentHash)) {
                    is RequestResult.Success -> {
                        result.data
                    }
                    is RequestResult.Error -> {
                        if (result is RequestResult.Error.ApiError && result.code == 404) {
                            eventChannel.send(Event.TorrentNotFound)
                        } else {
                            eventChannel.send(Event.Error(result))
                        }
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
                        if (result is RequestResult.Error.ApiError && result.code == 404) {
                            eventChannel.send(Event.TorrentNotFound)
                        } else {
                            eventChannel.send(Event.Error(result))
                        }
                        throw CancellationException()
                    }
                }
            }

            val pieces = piecesDeferred.await()
            val properties = propertiesDeferred.await()

            _torrentPieces.value = pieces
            _torrentProperties.value = properties
        }

    fun loadPieces(serverConfig: ServerConfig, torrentHash: String) {
        if (!isLoading.value) {
            _isLoading.value = true
            updatePieces(serverConfig, torrentHash).invokeOnCompletion {
                _isLoading.value = false
            }
        }
    }

    fun refreshPieces(serverConfig: ServerConfig, torrentHash: String) {
        if (!isRefreshing.value) {
            _isRefreshing.value = true
            updatePieces(serverConfig, torrentHash).invokeOnCompletion {
                _isRefreshing.value = false
            }
        }
    }

    sealed class Event {
        data class Error(val error: RequestResult.Error) : Event()
        object TorrentNotFound : Event()
    }
}
