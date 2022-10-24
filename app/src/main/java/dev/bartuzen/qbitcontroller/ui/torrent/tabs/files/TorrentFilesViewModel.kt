package dev.bartuzen.qbitcontroller.ui.torrent.tabs.files

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.bartuzen.qbitcontroller.data.repositories.TorrentRepository
import dev.bartuzen.qbitcontroller.model.ServerConfig
import dev.bartuzen.qbitcontroller.model.TorrentFileNode
import dev.bartuzen.qbitcontroller.network.RequestError
import dev.bartuzen.qbitcontroller.network.RequestResult
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.ArrayDeque
import javax.inject.Inject

@HiltViewModel
class TorrentFilesViewModel @Inject constructor(
    private val repository: TorrentRepository
) : ViewModel() {
    val torrentFiles = MutableStateFlow<TorrentFileNode?>(null)

    private val _nodeStack = MutableStateFlow(ArrayDeque<String>())
    val nodeStack: StateFlow<ArrayDeque<String>> = _nodeStack

    private val eventChannel = Channel<Event>()
    val eventFlow = eventChannel.receiveAsFlow()

    val isLoading = MutableStateFlow(true)
    var isInitialLoadStarted = false

    fun updateFiles(serverConfig: ServerConfig, torrentHash: String) = viewModelScope.launch {
        when (val result = repository.getFiles(serverConfig, torrentHash)) {
            is RequestResult.Success -> {
                torrentFiles.value = TorrentFileNode.fromFileList(result.data.map { it.name })
            }
            is RequestResult.Error -> {
                eventChannel.send(Event.Error(result.error))
            }
        }
    }

    fun goToFolder(node: String) {
        _nodeStack.update { stack ->
            stack.clone().apply {
                push(node)
            }
        }
    }

    fun goBack() {
        _nodeStack.update { stack ->
            stack.clone().apply {
                pop()
            }
        }
    }

    sealed class Event {
        data class Error(val error: RequestError) : Event()
    }
}