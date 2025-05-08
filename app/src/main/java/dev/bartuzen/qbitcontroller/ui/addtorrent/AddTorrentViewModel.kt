package dev.bartuzen.qbitcontroller.ui.addtorrent

import android.content.Context
import android.os.Parcelable
import android.provider.MediaStore
import androidx.core.net.toUri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.bartuzen.qbitcontroller.data.ServerManager
import dev.bartuzen.qbitcontroller.data.repositories.AddTorrentRepository
import dev.bartuzen.qbitcontroller.network.RequestResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import java.io.FileNotFoundException

class AddTorrentViewModel(
    initialServerId: Int?,
    private val context: Context,
    private val savedStateHandle: SavedStateHandle,
    private val repository: AddTorrentRepository,
    private val serverManager: ServerManager,
) : ViewModel() {
    private val eventChannel = Channel<Event>()
    val eventFlow = eventChannel.receiveAsFlow()

    val servers = serverManager.serversFlow

    val serverId = savedStateHandle.getStateFlow<Int?>("serverId", initialServerId)

    val serverData = savedStateHandle.getStateFlow<ServerData?>("serverData", null)

    val isLoading = savedStateHandle.getStateFlow("isLoading", false)

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    private val _isAdding = MutableStateFlow(false)
    val isAdding = _isAdding.asStateFlow()

    private var loadCategoryTagJob: Job? = null

    init {
        viewModelScope.launch {
            if (serverId.value == null) {
                val firstServerId = serverManager.serversFlow.value.keys.firstOrNull()
                if (firstServerId != null) {
                    setServerId(firstServerId)
                } else {
                    eventChannel.send(Event.NoServersFound)
                    return@launch
                }
            }

            serverId
                .drop(if (serverData.value != null) 1 else 0)
                .collectLatest { serverId ->
                    setServerData(null)
                    if (serverId != null) {
                        loadData(serverId)
                    }
                }
        }
    }

    fun addTorrent(
        serverId: Int,
        links: List<String>?,
        fileUris: List<String>?,
        savePath: String?,
        category: String?,
        tags: List<String>,
        stopCondition: String?,
        contentLayout: String?,
        torrentName: String?,
        downloadSpeedLimit: Int?,
        uploadSpeedLimit: Int?,
        ratioLimit: Double?,
        seedingTimeLimit: Int?,
        isPaused: Boolean,
        skipHashChecking: Boolean,
        isAutoTorrentManagementEnabled: Boolean?,
        isSequentialDownloadEnabled: Boolean,
        isFirstLastPiecePrioritized: Boolean,
    ) = viewModelScope.launch {
        if (!isAdding.value) {
            _isAdding.value = true

            val files = try {
                fileUris?.mapNotNull {
                    withContext(Dispatchers.IO) {
                        val projection = arrayOf(MediaStore.MediaColumns.DISPLAY_NAME)
                        val uri = it.toUri()
                        val fileName = context.contentResolver.query(uri, projection, null, null, null)?.use { metaCursor ->
                            if (metaCursor.moveToFirst()) {
                                metaCursor.getString(0)
                            } else {
                                null
                            }
                        }

                        val content = context.contentResolver.openInputStream(uri).use { stream ->
                            stream?.readBytes()
                        }

                        if (fileName != null && content != null) {
                            fileName to content
                        } else {
                            null
                        }
                    }
                }
            } catch (_: FileNotFoundException) {
                eventChannel.send(Event.FileNotFound)
                _isAdding.value = false
                return@launch
            } catch (e: Exception) {
                eventChannel.send(Event.FileReadError("${e::class.simpleName} ${e.message}"))
                _isAdding.value = false
                return@launch
            }

            when (
                val result = repository.addTorrent(
                    serverId,
                    links,
                    files,
                    savePath,
                    category,
                    tags,
                    stopCondition,
                    contentLayout,
                    torrentName,
                    downloadSpeedLimit,
                    uploadSpeedLimit,
                    ratioLimit,
                    seedingTimeLimit,
                    isPaused,
                    skipHashChecking,
                    isAutoTorrentManagementEnabled,
                    isSequentialDownloadEnabled,
                    isFirstLastPiecePrioritized,
                )
            ) {
                is RequestResult.Success -> {
                    if (result.data == "Ok.") {
                        eventChannel.send(Event.TorrentAdded(serverId))
                    } else {
                        eventChannel.send(Event.TorrentAddError)
                    }
                }
                is RequestResult.Error -> {
                    if (result is RequestResult.Error.ApiError && result.code == 415) {
                        eventChannel.send(Event.InvalidTorrentFile)
                    } else {
                        eventChannel.send(Event.Error(result))
                    }
                }
            }
            _isAdding.value = false
        }
    }

    private fun updateData(serverId: Int) = viewModelScope.launch {
        val categoriesDeferred = async {
            when (val result = repository.getCategories(serverId)) {
                is RequestResult.Success -> {
                    result.data.values
                        .toList()
                        .map { it.name }
                        .sorted()
                }
                is RequestResult.Error -> {
                    eventChannel.send(Event.Error(result))
                    throw CancellationException()
                }
            }
        }
        val tagsDeferred = async {
            when (val result = repository.getTags(serverId)) {
                is RequestResult.Success -> {
                    result.data.sorted()
                }
                is RequestResult.Error -> {
                    eventChannel.send(Event.Error(result))
                    throw CancellationException()
                }
            }
        }
        val defaultSavePathDeferred = async {
            when (val result = repository.getDefaultSavePath(serverId)) {
                is RequestResult.Success -> {
                    result.data
                }
                is RequestResult.Error -> {
                    eventChannel.send(Event.Error(result))
                    throw CancellationException()
                }
            }
        }

        try {
            val categories = categoriesDeferred.await()
            val tags = tagsDeferred.await()
            val defaultSavePath = defaultSavePathDeferred.await()

            setServerData(ServerData(categories, tags, defaultSavePath))
        } catch (_: CancellationException) {
        }
    }

    fun loadData(serverId: Int) {
        loadCategoryTagJob?.cancel()

        setLoading(true)
        val job = updateData(serverId)
        job.invokeOnCompletion { e ->
            if (e !is CancellationException) {
                setLoading(false)
                loadCategoryTagJob = null
            }
        }
        loadCategoryTagJob = job
    }

    fun refreshData(serverId: Int) {
        if (!isRefreshing.value) {
            _isRefreshing.value = true
            updateData(serverId).invokeOnCompletion {
                viewModelScope.launch {
                    delay(25)
                    _isRefreshing.value = false
                }
            }
        }
    }

    fun setServerId(serverId: Int?) {
        savedStateHandle["serverId"] = serverId
    }

    private fun setServerData(serverData: ServerData?) {
        savedStateHandle["serverData"] = serverData
    }

    private fun setLoading(isLoading: Boolean) {
        savedStateHandle["isLoading"] = isLoading
    }

    sealed class Event {
        data class Error(val error: RequestResult.Error) : Event()
        data object NoServersFound : Event()
        data object FileNotFound : Event()
        data class FileReadError(val error: String) : Event()
        data object InvalidTorrentFile : Event()
        data object TorrentAddError : Event()
        data class TorrentAdded(val serverId: Int) : Event()
    }
}

@Parcelize
data class ServerData(
    val categoryList: List<String>,
    val tagList: List<String>,
    val defaultSavePath: String,
) : Parcelable
