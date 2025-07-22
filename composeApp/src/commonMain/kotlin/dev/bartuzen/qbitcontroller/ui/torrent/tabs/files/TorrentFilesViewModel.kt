package dev.bartuzen.qbitcontroller.ui.torrent.tabs.files

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.bartuzen.qbitcontroller.data.SettingsManager
import dev.bartuzen.qbitcontroller.data.repositories.torrent.TorrentFilesRepository
import dev.bartuzen.qbitcontroller.model.TorrentFileNode
import dev.bartuzen.qbitcontroller.model.TorrentFilePriority
import dev.bartuzen.qbitcontroller.network.RequestResult
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

class TorrentFilesViewModel(
    private val serverId: Int,
    private val torrentHash: String,
    settingsManager: SettingsManager,
    private val repository: TorrentFilesRepository,
) : ViewModel() {
    private val _filesNode = MutableStateFlow<TorrentFileNode.Folder?>(null)
    val filesNode = _filesNode.asStateFlow()

    private val eventChannel = Channel<Event>()
    val eventFlow = eventChannel.receiveAsFlow()

    private val _isNaturalLoading = MutableStateFlow<Boolean?>(null)
    val isNaturalLoading = _isNaturalLoading.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    val autoRefreshInterval = settingsManager.autoRefreshInterval.flow

    private val isScreenActive = MutableStateFlow(false)

    init {
        loadFiles()

        viewModelScope.launch {
            combine(
                autoRefreshInterval,
                isNaturalLoading,
                isScreenActive,
            ) { autoRefreshInterval, isNaturalLoading, isScreenActive ->
                Triple(autoRefreshInterval, isNaturalLoading, isScreenActive)
            }.collectLatest { (autoRefreshInterval, isNaturalLoading, isScreenActive) ->
                if (isScreenActive && isNaturalLoading == null && autoRefreshInterval != 0) {
                    delay(autoRefreshInterval.seconds)
                    loadFiles(autoRefresh = true)
                }
            }
        }
    }

    fun setScreenActive(isScreenActive: Boolean) {
        this.isScreenActive.value = isScreenActive
    }

    private fun updateFiles() = viewModelScope.launch {
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

                _filesNode.value = TorrentFileNode.fromFileList(files)
            }
            is RequestResult.Error.ApiError if result.code == 404 -> {
                eventChannel.send(Event.TorrentNotFound)
            }
            is RequestResult.Error -> {
                eventChannel.send(Event.Error(result))
            }
        }
    }

    private fun loadFiles(autoRefresh: Boolean = false) {
        if (isNaturalLoading.value == null) {
            _isNaturalLoading.value = !autoRefresh
            updateFiles().invokeOnCompletion {
                _isNaturalLoading.value = null
            }
        }
    }

    fun refreshFiles() {
        if (!isRefreshing.value) {
            _isRefreshing.value = true
            updateFiles().invokeOnCompletion {
                viewModelScope.launch {
                    delay(25)
                    _isRefreshing.value = false
                }
            }
        }
    }

    fun setFilePriority(filePaths: List<String>, priority: TorrentFilePriority) = viewModelScope.launch {
        val rootNode = filesNode.value ?: return@launch
        val fileIndices = rootNode.findAllFiles(filePaths).map { it.index }
        when (val result = repository.setFilePriority(serverId, torrentHash, fileIndices, priority)) {
            is RequestResult.Success -> {
                eventChannel.send(Event.FilePriorityUpdated)
                loadFiles()
            }
            is RequestResult.Error -> {
                eventChannel.send(Event.Error(result))
            }
        }
    }

    fun renameFile(file: String, newName: String) = viewModelScope.launch {
        when (val result = repository.renameFile(serverId, torrentHash, file, newName)) {
            is RequestResult.Success -> {
                eventChannel.send(Event.FileRenamed)
                launch {
                    delay(1000)
                    loadFiles()
                }
            }
            is RequestResult.Error.ApiError if result.code == 409 -> {
                eventChannel.send(Event.PathIsInvalidOrInUse)
            }
            is RequestResult.Error -> {
                eventChannel.send(Event.Error(result))
            }
        }
    }

    fun renameFolder(folder: String, newName: String) = viewModelScope.launch {
        when (val result = repository.renameFolder(serverId, torrentHash, folder, newName)) {
            is RequestResult.Success -> {
                eventChannel.send(Event.FolderRenamed)
                launch {
                    delay(1000)
                    loadFiles()
                }
            }
            is RequestResult.Error.ApiError if result.code == 409 -> {
                eventChannel.send(Event.PathIsInvalidOrInUse)
            }
            is RequestResult.Error -> {
                eventChannel.send(Event.Error(result))
            }
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
