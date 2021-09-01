package dev.bartuzen.qbitcontroller.ui.torrent

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.bartuzen.qbitcontroller.R
import dev.bartuzen.qbitcontroller.data.repositories.TorrentRepository
import dev.bartuzen.qbitcontroller.model.*
import dev.bartuzen.qbitcontroller.network.RequestHelper
import dev.bartuzen.qbitcontroller.network.RequestResult
import dev.bartuzen.qbitcontroller.ui.common.SettableLiveData
import dev.bartuzen.qbitcontroller.ui.common.StateDelegate
import dev.bartuzen.qbitcontroller.utils.getErrorMessage
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TorrentViewModel @Inject constructor(
    private val repository: TorrentRepository,
    private val requestHelper: RequestHelper,
    state: SavedStateHandle
) : ViewModel() {
    val torrent = SettableLiveData<Torrent>()
    val fileList = SettableLiveData<List<TorrentFile>>()
    val torrentPieces = SettableLiveData<List<PieceState>>()
    val torrentProperties = SettableLiveData<TorrentProperties>()

    var torrentHash: String? by StateDelegate(state, "torrent_hash")
    var serverConfig: ServerConfig? by StateDelegate(state, "server_config")

    private val torrentActivityEventChannel = Channel<TorrentActivityEvent>()
    val torrentActivityEvent = torrentActivityEventChannel.receiveAsFlow()

    private val torrentOverviewEventChannel = Channel<TorrentOverviewEvent>()
    val torrentOverviewEvent = torrentOverviewEventChannel.receiveAsFlow()

    private val torrentFileListEventChannel = Channel<TorrentFileListEvent>()
    val torrentFileListEvent = torrentFileListEventChannel.receiveAsFlow()

    private val torrentPiecesEventChannel = Channel<TorrentPiecesEvent>()
    val torrentPiecesEvent = torrentPiecesEventChannel.receiveAsFlow()

    fun updateTorrent() = viewModelScope.launch {
        serverConfig?.let { config ->
            val result = requestHelper.request(config) {
                repository.getTorrent(config, torrentHash ?: "")
            }
            if (result.second?.body()?.size == 1) {
                torrent.value = result.second?.body()?.first()

                torrentOverviewEventChannel.send(TorrentOverviewEvent.OnRequestComplete(result.first))
            } else if (torrent.value == null) {
                torrent.isSet = true
            }
        }
    }

    fun updateFileList() = viewModelScope.launch {
        serverConfig?.let { config ->
            val result = requestHelper.request(config) {
                repository.getFileList(config, torrentHash ?: "")
            }

            fileList.value = result.second?.body()

            torrentFileListEventChannel.send(TorrentFileListEvent.OnRequestComplete(result.first))
        }
    }

    fun updatePieces() = viewModelScope.launch {
        serverConfig?.let { config ->
            val result = requestHelper.request(config) {
                repository.getPieces(config, torrentHash ?: "")
            }

            torrentPieces.value = result.second?.body()

            torrentPiecesEventChannel.send(TorrentPiecesEvent.OnRequestComplete(result.first))
        }
    }

    fun updateProperties() = viewModelScope.launch {
        serverConfig?.let { config ->
            val result = requestHelper.request(config) {
                repository.getProperties(config, torrentHash ?: "")
            }

            torrentProperties.value = result.second?.body()
        }
    }

    fun pauseTorrent(context: Context) = viewModelScope.launch {
        serverConfig?.let { config ->
            val result = requestHelper.request(config) {
                repository.pauseTorrent(config, torrentHash ?: "")
            }

            val message = if (result.first == RequestResult.SUCCESS) {
                context.getString(R.string.torrent_paused_success)
            } else {
                context.getErrorMessage(result.first)
            }
            torrentActivityEventChannel.send(TorrentActivityEvent.ShowMessage(message))
        }
    }

    fun resumeTorrent(context: Context) = viewModelScope.launch {
        serverConfig?.let { config ->
            val result = requestHelper.request(config) {
                repository.resumeTorrent(config, torrentHash ?: "")
            }

            val message = if (result.first == RequestResult.SUCCESS) {
                context.getString(R.string.torrent_resumed_success)
            } else {
                context.getErrorMessage(result.first)
            }
            torrentActivityEventChannel.send(TorrentActivityEvent.ShowMessage(message))
        }
    }

    sealed class TorrentActivityEvent {
        data class ShowMessage(val message: String) : TorrentActivityEvent()
    }

    sealed class TorrentOverviewEvent {
        data class OnRequestComplete(val result: RequestResult) : TorrentOverviewEvent()
    }

    sealed class TorrentFileListEvent {
        data class OnRequestComplete(val result: RequestResult) : TorrentFileListEvent()
    }

    sealed class TorrentPiecesEvent {
        data class OnRequestComplete(val result: RequestResult) : TorrentPiecesEvent()
    }
}