package dev.bartuzen.qbitcontroller.ui.torrentlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.bartuzen.qbitcontroller.data.SettingsManager
import dev.bartuzen.qbitcontroller.data.TorrentSort
import dev.bartuzen.qbitcontroller.data.repositories.TorrentListRepository
import dev.bartuzen.qbitcontroller.model.ServerConfig
import dev.bartuzen.qbitcontroller.model.Torrent
import dev.bartuzen.qbitcontroller.network.RequestError
import dev.bartuzen.qbitcontroller.network.RequestResult
import dev.bartuzen.qbitcontroller.ui.common.SettableLiveData
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TorrentListViewModel @Inject constructor(
    private val repository: TorrentListRepository,
    private val settingsManager: SettingsManager
) : ViewModel() {
    val torrentList = SettableLiveData<List<Torrent>>()

    val torrentSort = settingsManager.sortFlow.asLiveData()

    private val torrentListEventChannel = Channel<TorrentListEvent>()
    val torrentListEvent = torrentListEventChannel.receiveAsFlow()

    fun updateTorrentList(serverConfig: ServerConfig) = viewModelScope.launch {
        when (val result =
            repository.getTorrentList(serverConfig, torrentSort.value ?: TorrentSort.NAME)) {
            is RequestResult.Success -> torrentList.value = result.data
            is RequestResult.Error -> torrentListEventChannel.send(
                TorrentListEvent.OnError(result.error)
            )
        }
    }

    fun setTorrentSort(torrentSort: TorrentSort) = viewModelScope.launch {
        settingsManager.setTorrentSort(torrentSort)
    }

    sealed class TorrentListEvent {
        data class OnError(val result: RequestError) : TorrentListEvent()
    }
}