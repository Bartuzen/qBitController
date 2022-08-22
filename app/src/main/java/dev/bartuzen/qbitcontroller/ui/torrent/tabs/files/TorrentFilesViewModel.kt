package dev.bartuzen.qbitcontroller.ui.torrent.tabs.files

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.bartuzen.qbitcontroller.data.repositories.TorrentRepository
import dev.bartuzen.qbitcontroller.model.ServerConfig
import dev.bartuzen.qbitcontroller.model.TorrentFile
import dev.bartuzen.qbitcontroller.network.RequestError
import dev.bartuzen.qbitcontroller.network.RequestResult
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TorrentFilesViewModel @Inject constructor(
    private val repository: TorrentRepository
) : ViewModel() {
    val torrentFiles = MutableStateFlow<List<TorrentFile>?>(null)

    private val eventChannel = Channel<Event>()
    val eventFlow = eventChannel.receiveAsFlow()

    val isLoading = MutableStateFlow(true)

    fun updateFiles(serverConfig: ServerConfig, torrentHash: String) = viewModelScope.launch {
        when (val result = repository.getFiles(serverConfig, torrentHash)) {
            is RequestResult.Success -> {
                torrentFiles.value = result.data
            }
            is RequestResult.Error -> {
                eventChannel.send(Event.OnError(result.error))
            }
        }
    }

    sealed class Event {
        data class OnError(val error: RequestError) : Event()
    }
}