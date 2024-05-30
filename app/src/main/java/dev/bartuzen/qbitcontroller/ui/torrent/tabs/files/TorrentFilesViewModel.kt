package dev.bartuzen.qbitcontroller.ui.torrent.tabs.files

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.bartuzen.qbitcontroller.data.SettingsManager
import dev.bartuzen.qbitcontroller.data.repositories.torrent.TorrentFilesRepository
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
import javax.inject.Inject

@HiltViewModel
class TorrentFilesViewModel @Inject constructor(
    settingsManager: SettingsManager,
    private val repository: TorrentFilesRepository
) : ViewModel() {
    private val _torrentFiles = MutableStateFlow<TorrentFileNode?>(null)
    val torrentFiles = _torrentFiles.asStateFlow()

    private val _nodeStack = MutableStateFlow(ArrayDeque<String>())
    val nodeStack = _nodeStack.asStateFlow()

    private val eventChannel = Channel<Event>()
    val eventFlow = eventChannel.receiveAsFlow()

    private val _isNaturalLoading = MutableStateFlow<Boolean?>(null)
    val isNaturalLoading = _isNaturalLoading.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    var isInitialLoadStarted = false

    val autoRefreshInterval = settingsManager.autoRefreshInterval.flow
    val autoRefreshHideLoadingBar = settingsManager.autoRefreshHideLoadingBar.flow

    private fun updateFiles(serverId: Int, torrentHash: String) = viewModelScope.launch {
        when (val result = repository.getFiles(serverId, torrentHash)) {
            is RequestResult.Success -> {
                // Older API versions do not return file index, so we are creating them manually
                val files = result.data.let { files ->
                    if (files.size > 1 && files[1].index == 0) {
                        files.mapIndexed { index, file ->
                            file.copy(index = index)
                        }
                    } else {
                        files
                    }
                }

                _torrentFiles.value = TorrentFileNode.fromFileList(files)
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

    fun loadFiles(serverId: Int, torrentHash: String, autoRefresh: Boolean = false) {
        if (isNaturalLoading.value == null) {
            _isNaturalLoading.value = !autoRefresh
            updateFiles(serverId, torrentHash).invokeOnCompletion {
                _isNaturalLoading.value = null
            }
        }
    }

    fun refreshFiles(serverId: Int, torrentHash: String) {
        if (!isRefreshing.value) {
            _isRefreshing.value = true
            updateFiles(serverId, torrentHash).invokeOnCompletion {
                _isRefreshing.value = false
            }
        }
    }

    fun setFilePriority(serverId: Int, hash: String, files: List<TorrentFile>, priority: TorrentFilePriority) =
        viewModelScope.launch {
            when (val result = repository.setFilePriority(serverId, hash, files.map { it.index }, priority)) {
                is RequestResult.Success -> {
                    eventChannel.send(Event.FilePriorityUpdated)
                }
                is RequestResult.Error -> {
                    eventChannel.send(Event.Error(result))
                }
            }
        }

    fun renameFile(serverId: Int, hash: String, file: String, newName: String) = viewModelScope.launch {
        when (val result = repository.renameFile(serverId, hash, file, newName)) {
            is RequestResult.Success -> {
                eventChannel.send(Event.FileRenamed)
            }
            is RequestResult.Error -> {
                if (result is RequestResult.Error.ApiError && result.code == 409) {
                    eventChannel.send(Event.PathIsInvalidOrInUse)
                } else {
                    eventChannel.send(Event.Error(result))
                }
            }
        }
    }

    fun renameFolder(serverId: Int, hash: String, folder: String, newName: String) = viewModelScope.launch {
        when (val result = repository.renameFolder(serverId, hash, folder, newName)) {
            is RequestResult.Success -> {
                eventChannel.send(Event.FolderRenamed)
            }
            is RequestResult.Error -> {
                if (result is RequestResult.Error.ApiError && result.code == 409) {
                    eventChannel.send(Event.PathIsInvalidOrInUse)
                } else {
                    eventChannel.send(Event.Error(result))
                }
            }
        }
    }

    fun goToFolder(node: String) {
        _nodeStack.update { stack ->
            ArrayDeque(stack).apply {
                addLast(node)
            }
        }
    }

    fun goBack() {
        _nodeStack.update { stack ->
            ArrayDeque(stack).apply {
                removeLastOrNull()
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
        data object TorrentNotFound : Event()
        data object PathIsInvalidOrInUse : Event()
        data object FilePriorityUpdated : Event()
        data object FileRenamed : Event()
        data object FolderRenamed : Event()
    }
}
