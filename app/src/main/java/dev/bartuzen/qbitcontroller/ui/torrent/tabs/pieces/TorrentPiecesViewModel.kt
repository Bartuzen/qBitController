package dev.bartuzen.qbitcontroller.ui.torrent.tabs.pieces

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.bartuzen.qbitcontroller.data.repositories.TorrentRepository
import dev.bartuzen.qbitcontroller.model.PieceState
import dev.bartuzen.qbitcontroller.model.ServerConfig
import dev.bartuzen.qbitcontroller.model.TorrentProperties
import dev.bartuzen.qbitcontroller.network.RequestHelper
import dev.bartuzen.qbitcontroller.network.RequestResult
import dev.bartuzen.qbitcontroller.ui.torrent.tabs.base.TorrentTabViewModel
import javax.inject.Inject

@HiltViewModel
class TorrentPiecesViewModel @Inject constructor(
    private val requestHelper: RequestHelper,
    private val repository: TorrentRepository,
) : TorrentTabViewModel() {
    var pieces by mutableStateOf(emptyList<PieceState>())
    var properties by mutableStateOf<TorrentProperties?>(null)

    suspend fun updatePieces(serverConfig: ServerConfig, torrentHash: String) {
        val piecesResult = requestHelper.request(serverConfig) {
            repository.getPieces(serverConfig, torrentHash)
        }

        val propertiesResult = requestHelper.request(serverConfig) {
            repository.getProperties(serverConfig, torrentHash)
        }

        if (piecesResult.first == RequestResult.SUCCESS &&
            propertiesResult.first == RequestResult.SUCCESS
        ) {
            pieces = piecesResult.second?.body() ?: emptyList()
            properties = propertiesResult.second?.body()
        } else {
            eventFlow.emit(TorrentEvent.ShowError(piecesResult.first))
        }
    }
}

