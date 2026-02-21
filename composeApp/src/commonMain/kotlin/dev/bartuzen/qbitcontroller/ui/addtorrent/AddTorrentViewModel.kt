package dev.bartuzen.qbitcontroller.ui.addtorrent

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.bartuzen.qbitcontroller.data.ServerManager
import dev.bartuzen.qbitcontroller.data.repositories.AddTorrentRepository
import dev.bartuzen.qbitcontroller.model.Category
import dev.bartuzen.qbitcontroller.model.QBittorrentVersion
import dev.bartuzen.qbitcontroller.network.RequestManager
import dev.bartuzen.qbitcontroller.network.RequestResult
import dev.bartuzen.qbitcontroller.utils.getSerializableStateFlow
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.readBytes
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.io.files.FileNotFoundException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.math.max

class AddTorrentViewModel(
    initialServerId: Int?,
    private val savedStateHandle: SavedStateHandle,
    private val repository: AddTorrentRepository,
    private val serverManager: ServerManager,
    private val requestManager: RequestManager,
) : ViewModel() {
    private val eventChannel = Channel<Event>()
    val eventFlow = eventChannel.receiveAsFlow()

    val servers = serverManager.serversFlow

    val serverId = savedStateHandle.getStateFlow("serverId", initialServerId)

    val serverData = savedStateHandle.getSerializableStateFlow<ServerData?>("serverData", null)

    val isLoading = savedStateHandle.getStateFlow("isLoading", false)

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    private val _isAdding = MutableStateFlow(false)
    val isAdding = _isAdding.asStateFlow()

    private val savePath = MutableStateFlow<String?>("")

    private val directoryInfo = MutableStateFlow<DirectoryInfo?>(null)

    val directorySuggestions = directoryInfo
        .map { it?.content ?: emptyList() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var loadCategoryTagJob: Job? = null
    private var searchDirectoriesJob: Job? = null

    init {
        viewModelScope.launch {
            if (serverId.value == null) {
                val firstServerId = serverManager.serversFlow.value.firstOrNull()?.id
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

        viewModelScope.launch {
            combine(savePath, serverId) { path, serverId ->
                Pair(path, serverId)
            }.collectLatest { (path, serverId) ->
                if (serverId != null && path != null) {
                    searchDirectories(serverId, path)
                }
            }
        }
    }

    fun addTorrent(
        serverId: Int,
        links: List<String>?,
        files: List<PlatformFile>?,
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

            val filesWithContent = try {
                files?.map { it.name to it.readBytes() }
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
                    filesWithContent,
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
                is RequestResult.Error.ApiError if result.code == 415 -> {
                    eventChannel.send(Event.InvalidTorrentFile)
                }
                is RequestResult.Error -> {
                    eventChannel.send(Event.Error(result))
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
                        .sortedWith(Category.comparator)
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
                _isRefreshing.value = false
            }
        }
    }

    private fun searchDirectories(serverId: Int, path: String) {
        searchDirectoriesJob?.cancel()

        val currentDirectoryInfo = directoryInfo.value
        if (serverId != currentDirectoryInfo?.serverId) {
            directoryInfo.value = null
        }

        if (path.isBlank()) {
            directoryInfo.value = currentDirectoryInfo?.copy(content = emptyList())
            return
        }

        val version = requestManager.getQBittorrentVersion(serverId)
        if (version < QBittorrentVersion(5, 0, 0)) {
            return
        }

        searchDirectoriesJob = viewModelScope.launch {
            if (directoryInfo.value != null) {
                delay(300)
            }

            val pathDeferred = async {
                when (val result = repository.getDirectoryContent(serverId, path)) {
                    is RequestResult.Success -> {
                        result.data
                    }
                    is RequestResult.Error -> {
                        emptyList()
                    }
                }
            }

            val parentDeferred = async {
                if (path.endsWith("/") || path.endsWith("\\")) {
                    return@async emptyList()
                }

                val lastSeparatorIndex = max(
                    path.lastIndexOf('/'),
                    path.lastIndexOf('\\'),
                )
                if (lastSeparatorIndex == -1) {
                    return@async emptyList()
                }

                val parent = path.take(lastSeparatorIndex + 1)
                when (val result = repository.getDirectoryContent(serverId, parent)) {
                    is RequestResult.Success -> {
                        result.data
                    }
                    is RequestResult.Error -> {
                        emptyList()
                    }
                }
            }

            directoryInfo.value = DirectoryInfo(
                serverId = serverId,
                content = (pathDeferred.await() + parentDeferred.await())
                    .distinct()
                    .filter { it.startsWith(path, ignoreCase = true) }
                    .sortedWith(String.CASE_INSENSITIVE_ORDER),
            )
        }
    }

    fun setServerId(serverId: Int?) {
        savedStateHandle["serverId"] = serverId
    }

    private fun setServerData(serverData: ServerData?) {
        savedStateHandle["serverData"] = Json.encodeToString(serverData)
    }

    private fun setLoading(isLoading: Boolean) {
        savedStateHandle["isLoading"] = isLoading
    }

    fun setSavePath(path: String) {
        savePath.value = path
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

@Serializable
data class ServerData(
    val categoryList: List<Category>,
    val tagList: List<String>,
    val defaultSavePath: String,
)

private data class DirectoryInfo(
    val serverId: Int,
    val content: List<String>,
)
