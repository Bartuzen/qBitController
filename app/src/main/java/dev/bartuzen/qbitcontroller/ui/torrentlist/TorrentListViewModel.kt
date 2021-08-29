package dev.bartuzen.qbitcontroller.ui.torrentlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.bartuzen.qbitcontroller.data.repositories.TorrentListRepository
import dev.bartuzen.qbitcontroller.model.ServerConfig
import dev.bartuzen.qbitcontroller.model.Torrent
import dev.bartuzen.qbitcontroller.network.RequestHelper
import dev.bartuzen.qbitcontroller.network.RequestResult
import dev.bartuzen.qbitcontroller.ui.common.SettableLiveData
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TorrentListViewModel @Inject constructor(
    private val repository: TorrentListRepository,
    private val requestHelper: RequestHelper
) : ViewModel() {
    val torrentList = SettableLiveData<List<Torrent>>()

    private val torrentListEventChannel = Channel<TorrentListEvent>()
    val torrentListEvent = torrentListEventChannel.receiveAsFlow()

    fun updateTorrentList(serverConfig: ServerConfig) = viewModelScope.launch {
        val result = requestHelper.request(serverConfig) {
            repository.getTorrentList(serverConfig)
        }

        torrentList.value = result.second?.body()

        torrentListEventChannel.send(TorrentListEvent.OnRequestComplete(result.first))
    }

    sealed class TorrentListEvent {
        data class OnRequestComplete(val result: RequestResult) : TorrentListEvent()
    }
}