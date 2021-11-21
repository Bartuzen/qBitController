package dev.bartuzen.qbitcontroller.ui.torrent.tabs.files

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.bartuzen.qbitcontroller.data.repositories.TorrentRepository
import dev.bartuzen.qbitcontroller.model.ServerConfig
import dev.bartuzen.qbitcontroller.model.TorrentFile
import dev.bartuzen.qbitcontroller.network.RequestHelper
import dev.bartuzen.qbitcontroller.network.RequestResult
import dev.bartuzen.qbitcontroller.ui.torrent.tabs.base.TorrentTabViewModel
import javax.inject.Inject

@HiltViewModel
class TorrentFilesViewModel @Inject constructor(
    private val requestHelper: RequestHelper,
    private val repository: TorrentRepository,
) : TorrentTabViewModel() {
    var files by mutableStateOf(emptyList<TorrentFile>())

    suspend fun updateFiles(serverConfig: ServerConfig, torrentHash: String) {
        val result = requestHelper.request(serverConfig) {
            repository.getFiles(serverConfig, torrentHash)
        }

        if (result.first == RequestResult.SUCCESS) {
            files = result.second?.body() ?: emptyList()
        } else {
            eventFlow.emit(TorrentEvent.ShowError(result.first))
        }
    }
}