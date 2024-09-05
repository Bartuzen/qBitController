package dev.bartuzen.qbitcontroller.ui.addtorrent

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.bartuzen.qbitcontroller.data.ServerManager
import dev.bartuzen.qbitcontroller.data.repositories.AddTorrentRepository
import dev.bartuzen.qbitcontroller.network.RequestResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.FileNotFoundException
import javax.inject.Inject

@HiltViewModel
class AddTorrentViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: AddTorrentRepository,
    private val serverManager: ServerManager,
) : ViewModel() {
    private val eventChannel = Channel<Event>()
    val eventFlow = eventChannel.receiveAsFlow()

    private val _categoryList = MutableStateFlow<List<String>?>(null)
    val categoryList = _categoryList.asStateFlow()

    private val _tagList = MutableStateFlow<List<String>?>(null)
    val tagList = _tagList.asStateFlow()

    private val _defaultSavePath = MutableStateFlow<String?>(null)
    val defaultSavePath = _defaultSavePath.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    private val _isCreating = MutableStateFlow(false)
    val isCreating = _isCreating.asStateFlow()

    private var loadCategoryTagJob: Job? = null

    fun getServers() = serverManager.serversFlow.value.values.toList()

    fun createTorrent(
        serverId: Int,
        links: List<String>?,
        fileUris: List<Uri>?,
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
        if (!isCreating.value) {
            _isCreating.value = true

            val files = try {
                fileUris?.mapNotNull {
                    withContext(Dispatchers.IO) {
                        val projection = arrayOf(MediaStore.MediaColumns.DISPLAY_NAME)
                        val fileName = context.contentResolver.query(it, projection, null, null, null)?.use { metaCursor ->
                            if (metaCursor.moveToFirst()) {
                                metaCursor.getString(0)
                            } else {
                                null
                            }
                        }

                        val content = context.contentResolver.openInputStream(it).use { stream ->
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
                _isCreating.value = false
                return@launch
            } catch (e: Exception) {
                eventChannel.send(Event.FileReadError("${e::class.simpleName} ${e.message}"))
                _isCreating.value = false
                return@launch
            }

            when (
                val result = repository.createTorrent(
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
                        eventChannel.send(Event.TorrentAdded)
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
            _isCreating.value = false
        }
    }

    private fun updateCategoryAndTags(serverId: Int) = viewModelScope.launch {
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

            _categoryList.value = categories
            _tagList.value = tags
            _defaultSavePath.value = defaultSavePath
        } catch (_: CancellationException) {
        }
    }

    fun removeCategoriesAndTags() {
        _categoryList.value = null
        _tagList.value = null
    }

    fun loadCategoryAndTags(serverId: Int) {
        loadCategoryTagJob?.cancel()

        _isLoading.value = true
        val job = updateCategoryAndTags(serverId)
        job.invokeOnCompletion { e ->
            if (e !is CancellationException) {
                _isLoading.value = false
                loadCategoryTagJob = null
            }
        }
        loadCategoryTagJob = job
    }

    fun refreshCategoryAndTags(serverId: Int) {
        if (!isRefreshing.value) {
            _isRefreshing.value = true
            updateCategoryAndTags(serverId).invokeOnCompletion {
                _isRefreshing.value = false
            }
        }
    }

    sealed class Event {
        data class Error(val error: RequestResult.Error) : Event()
        data object FileNotFound : Event()
        data class FileReadError(val error: String) : Event()
        data object InvalidTorrentFile : Event()
        data object TorrentAddError : Event()
        data object TorrentAdded : Event()
    }
}
