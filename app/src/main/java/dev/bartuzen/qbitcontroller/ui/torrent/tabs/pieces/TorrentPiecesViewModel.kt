package dev.bartuzen.qbitcontroller.ui.torrent.tabs.pieces

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.bartuzen.qbitcontroller.data.SettingsManager
import dev.bartuzen.qbitcontroller.data.repositories.torrent.TorrentPiecesRepository
import dev.bartuzen.qbitcontroller.model.PieceState
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
    settingsManager: SettingsManager,
    private val repository: TorrentPiecesRepository,
) : ViewModel() {
    private val _torrentPieces = MutableStateFlow<List<PieceState>?>(null)
    val torrentPieces = _torrentPieces.asStateFlow()

    private val _torrentProperties = MutableStateFlow<TorrentProperties?>(null)
    val torrentProperties = _torrentProperties.asStateFlow()

    private val eventChannel = Channel<Event>()
    val eventFlow = eventChannel.receiveAsFlow()

    private val _isNaturalLoading = MutableStateFlow<Boolean?>(null)
    val isNaturalLoading = _isNaturalLoading.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    var isInitialLoadStarted = false

    val autoRefreshInterval = settingsManager.autoRefreshInterval.flow

    private fun updatePieces(serverId: Int, torrentHash: String) = viewModelScope.launch {
        val piecesDeferred = async {
            when (val result = repository.getPieces(serverId, torrentHash)) {
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
            when (val result = repository.getProperties(serverId, torrentHash)) {
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

    fun loadPieces(serverId: Int, torrentHash: String, autoRefresh: Boolean = false) {
        if (isNaturalLoading.value == null) {
            _isNaturalLoading.value = !autoRefresh
            updatePieces(serverId, torrentHash).invokeOnCompletion {
                _isNaturalLoading.value = null
            }
        }
    }

    fun refreshPieces(serverId: Int, torrentHash: String) {
        if (!isRefreshing.value) {
            _isRefreshing.value = true
            updatePieces(serverId, torrentHash).invokeOnCompletion {
                _isRefreshing.value = false
            }
        }
    }

    sealed class Event {
        data class Error(val error: RequestResult.Error) : Event()
        data object TorrentNotFound : Event()
    }
}
