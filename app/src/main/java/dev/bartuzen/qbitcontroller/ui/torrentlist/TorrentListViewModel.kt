package dev.bartuzen.qbitcontroller.ui.torrentlist

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.bartuzen.qbitcontroller.data.SettingsManager
import dev.bartuzen.qbitcontroller.data.TorrentSort
import dev.bartuzen.qbitcontroller.data.repositories.TorrentListRepository
import dev.bartuzen.qbitcontroller.di.ApplicationScope
import dev.bartuzen.qbitcontroller.model.ServerConfig
import dev.bartuzen.qbitcontroller.model.Torrent
import dev.bartuzen.qbitcontroller.network.RequestHelper
import dev.bartuzen.qbitcontroller.network.RequestResult
import dev.bartuzen.qbitcontroller.ui.base.BaseLoadingViewModel
import dev.bartuzen.qbitcontroller.ui.common.PersistentMutableState
import dev.bartuzen.qbitcontroller.utils.first
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TorrentListViewModel @Inject constructor(
    private val requestHelper: RequestHelper,
    private val settingsManager: SettingsManager,
    private val repository: TorrentListRepository,
    @ApplicationScope private val appScope: CoroutineScope,
    state: SavedStateHandle
) : BaseLoadingViewModel() {
    private var _currentServer = mutableStateOf(null as ServerConfig?)

    var currentServer by PersistentMutableState(
        state = state,
        key = "server_id",
        mutableState = _currentServer
    )

    var torrentList by mutableStateOf(emptyList<Torrent>())

    val themeFlow = settingsManager.themeFlow
    val serversFlow = settingsManager.serversFlow
    val sortFlow = settingsManager.sortFlow

    val eventFlow = MutableSharedFlow<TorrentListEvent>()

    init {
        viewModelScope.launch {
            serversFlow.collectLatest { serverList ->
                val currentServerId = currentServer?.id ?: -1

                if (!serverList.containsKey(currentServerId)) {
                    torrentList = emptyList()
                    currentServer = if (serverList.size != 0) serverList.first() else null
                } else if (serverList[currentServerId] != currentServer) {
                    torrentList = emptyList()
                    currentServer = serverList[currentServerId]
                }
            }
        }
    }

    fun updateTorrentList() = viewModelScope.launch {
        val server = currentServer
        if (server != null) {
            val result = requestHelper.request(server) {
                repository.getTorrentList(server, sortFlow.first())
            }

            if (result.first == RequestResult.SUCCESS) {
                torrentList = result.second?.body() ?: emptyList()
            } else {
                eventFlow.emit(TorrentListEvent.ShowError(result.first))
            }
        } else {
            torrentList = emptyList()
        }
    }

    fun setTorrentSort(torrentSort: TorrentSort) = appScope.launch {
        settingsManager.setTorrentSort(torrentSort)
    }

    sealed class TorrentListEvent {
        data class ShowError(val message: RequestResult) : TorrentListEvent()
    }
}