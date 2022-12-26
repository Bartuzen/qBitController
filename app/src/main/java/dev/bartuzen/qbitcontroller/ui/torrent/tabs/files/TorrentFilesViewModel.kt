package dev.bartuzen.qbitcontroller.ui.torrent.tabs.files

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.bartuzen.qbitcontroller.data.repositories.TorrentRepository
import dev.bartuzen.qbitcontroller.model.ServerConfig
import dev.bartuzen.qbitcontroller.model.TorrentFile
import dev.bartuzen.qbitcontroller.model.TorrentFileNode
import dev.bartuzen.qbitcontroller.model.TorrentFilePriority
import dev.bartuzen.qbitcontroller.network.RequestResult
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.ArrayDeque
import javax.inject.Inject

@HiltViewModel
class TorrentFilesViewModel @Inject constructor(
    private val repository: TorrentRepository
) : ViewModel() {
    private val _torrentFiles = MutableStateFlow<TorrentFileNode?>(null)
    val torrentFiles = _torrentFiles.asStateFlow()

    private val _nodeStack = MutableStateFlow(ArrayDeque<String>())
    val nodeStack = _nodeStack.asStateFlow()

    private val eventChannel = Channel<Event>()
    val eventFlow = eventChannel.receiveAsFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    var isInitialLoadStarted = false

    private fun updateFiles(serverConfig: ServerConfig, torrentHash: String) = viewModelScope.launch {
        when (val result = repository.getFiles(serverConfig, torrentHash)) {
            is RequestResult.Success -> {
                _torrentFiles.value = TorrentFileNode.fromFileList(result.data)
            }
            is RequestResult.Error -> {
                if (result is RequestResult.Error.ApiError && result.code == 404) {
                    eventChannel.send(Event.TorrentNotFound)
                } else {
                    eventChannel.send(Event.Error(result))
                }
            }
        }
    }

    fun loadFiles(serverConfig: ServerConfig, torrentHash: String) {
        if (!isLoading.value) {
            _isLoading.value = true
            updateFiles(serverConfig, torrentHash).invokeOnCompletion {
                _isLoading.value = false
            }
        }
    }

    fun refreshFiles(serverConfig: ServerConfig, torrentHash: String) {
        if (!isRefreshing.value) {
            _isRefreshing.value = true
            updateFiles(serverConfig, torrentHash).invokeOnCompletion {
                _isRefreshing.value = false
            }
        }
    }

    fun setFilePriority(serverConfig: ServerConfig, hash: String, files: List<TorrentFile>, priority: TorrentFilePriority) =
        viewModelScope.launch {
            when (val result = repository.setFilePriority(serverConfig, hash, files.map { it.index }, priority)) {
                is RequestResult.Success -> {
                    eventChannel.send(Event.FilePriorityUpdated)
                }
                is RequestResult.Error -> {
                    eventChannel.send(Event.Error(result))
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

    fun goToRoot() {
        _nodeStack.update {
            ArrayDeque()
        }
    }

    sealed class Event {
        data class Error(val error: RequestResult.Error) : Event()
        object TorrentNotFound : Event()
        object FilePriorityUpdated : Event()
    }
}
