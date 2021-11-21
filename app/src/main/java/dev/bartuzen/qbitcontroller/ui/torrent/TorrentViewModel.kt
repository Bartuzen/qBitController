package dev.bartuzen.qbitcontroller.ui.torrent

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.bartuzen.qbitcontroller.R
import dev.bartuzen.qbitcontroller.data.SettingsManager
import dev.bartuzen.qbitcontroller.data.repositories.TorrentRepository
import dev.bartuzen.qbitcontroller.di.ApplicationScope
import dev.bartuzen.qbitcontroller.model.ServerConfig
import dev.bartuzen.qbitcontroller.model.Torrent
import dev.bartuzen.qbitcontroller.network.RequestHelper
import dev.bartuzen.qbitcontroller.network.RequestResult
import dev.bartuzen.qbitcontroller.ui.common.PersistentState
import dev.bartuzen.qbitcontroller.ui.torrent.tabs.base.TorrentTabViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TorrentViewModel @Inject constructor(
    private val repository: TorrentRepository,
    private val requestHelper: RequestHelper,
    @ApplicationScope private val applicationScope: CoroutineScope,
    settingsManager: SettingsManager,
    state: SavedStateHandle
) : TorrentTabViewModel() {
    var torrent by mutableStateOf<Torrent?>(null)

    var torrentHash: String by PersistentState(state, "torrent_hash", "")
    var serverConfig: ServerConfig? by PersistentState(state, "server_config", null)

    val themeFlow = settingsManager.themeFlow

    val customEventFlow = MutableSharedFlow<TorrentActivityEvent>()

    suspend fun updateOverview(serverConfig: ServerConfig, torrentHash: String) {
        val result = requestHelper.request(serverConfig) {
            repository.getTorrent(serverConfig, torrentHash)
        }

        if (result.first == RequestResult.SUCCESS) {
            torrent = result.second?.body()?.first()
        } else {
            eventFlow.emit(TorrentEvent.ShowError(result.first))
        }
    }

    fun pauseTorrent() = applicationScope.launch {
        serverConfig?.let { config ->
            val result = requestHelper.request(config) {
                repository.pauseTorrent(config, torrentHash)
            }

            val message = if (result.first == RequestResult.SUCCESS) {
                R.string.torrent_paused_success
            } else {
                result.first
            }

            customEventFlow.emit(TorrentActivityEvent.ShowError(message))
        }
    }

    fun resumeTorrent() = applicationScope.launch {
        serverConfig?.let { config ->
            val result = requestHelper.request(config) {
                repository.resumeTorrent(config, torrentHash)
            }

            val message = if (result.first == RequestResult.SUCCESS) {
                R.string.torrent_resumed_success
            } else {
                result.first
            }

            customEventFlow.emit(TorrentActivityEvent.ShowError(message))
        }
    }

    sealed class TorrentActivityEvent {
        data class ShowError(val message: Any) : TorrentActivityEvent()
    }
}