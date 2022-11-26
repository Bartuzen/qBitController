package dev.bartuzen.qbitcontroller.ui.addtorrent

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.bartuzen.qbitcontroller.data.SettingsManager
import dev.bartuzen.qbitcontroller.data.repositories.AddTorrentRepository
import dev.bartuzen.qbitcontroller.model.ServerConfig
import dev.bartuzen.qbitcontroller.network.RequestError
import dev.bartuzen.qbitcontroller.network.RequestResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
@SuppressLint("StaticFieldLeak")
class AddTorrentViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: AddTorrentRepository,
    settingsManager: SettingsManager
) : ViewModel() {
    private val eventChannel = Channel<Event>()
    val eventFlow = eventChannel.receiveAsFlow()

    val serversFlow = settingsManager.serversFlow

    private val _categoryList = MutableStateFlow<List<String>?>(null)
    val categoryList = _categoryList.asStateFlow()

    private val _tagList = MutableStateFlow<List<String>?>(null)
    val tagList = _tagList.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()

    private val _isCreating = MutableStateFlow(false)
    val isCreating = _isCreating.asStateFlow()

    var isInitialLoadStarted = false

    fun createTorrent(
        serverConfig: ServerConfig,
        links: List<String>?,
        fileUri: Uri?,
        category: String?,
        tags: List<String>,
        torrentName: String?,
        downloadSpeedLimit: Int?,
        uploadSpeedLimit: Int?,
        ratioLimit: Double?,
        seedingTimeLimit: Int?,
        isPaused: Boolean,
        skipHashChecking: Boolean,
        isAutoTorrentManagementEnabled: Boolean,
        isSequentialDownloadEnabled: Boolean,
        isFirstLastPiecePrioritized: Boolean
    ) = viewModelScope.launch {
        if (!isCreating.value) {
            _isCreating.value = true

            val fileBytes = withContext(Dispatchers.IO) {
                if (fileUri != null) {
                    context.contentResolver.openInputStream(fileUri).use { stream ->
                        stream?.readBytes()
                    }
                } else null
            }

            when (val result = repository.createTorrent(
                serverConfig,
                links,
                fileBytes,
                category,
                tags,
                torrentName,
                downloadSpeedLimit,
                uploadSpeedLimit,
                ratioLimit,
                seedingTimeLimit,
                isPaused,
                skipHashChecking,
                isAutoTorrentManagementEnabled,
                isSequentialDownloadEnabled,
                isFirstLastPiecePrioritized
            )) {
                is RequestResult.Success -> {
                    eventChannel.send(Event.TorrentCreated)
                }
                is RequestResult.Error -> {
                    eventChannel.send(Event.Error(result.error))
                }
            }
            _isCreating.value = false
        }
    }

    private fun updateCategoryAndTags(serverConfig: ServerConfig) = viewModelScope.launch {
        val categoriesDeferred = async {
            when (val result = repository.getCategories(serverConfig)) {
                is RequestResult.Success -> {
                    result.data.values
                        .toList()
                        .map { it.name }
                        .sortedBy { it }
                }
                is RequestResult.Error -> {
                    eventChannel.send(Event.Error(result.error))
                    throw CancellationException()
                }
            }
        }
        val tagsDeferred = async {
            when (val result = repository.getTags(serverConfig)) {
                is RequestResult.Success -> {
                    result.data.sortedBy { it }
                }
                is RequestResult.Error -> {
                    eventChannel.send(Event.Error(result.error))
                    throw CancellationException()
                }
            }
        }

        val categories = categoriesDeferred.await()
        val tags = tagsDeferred.await()

        _categoryList.value = categories
        _tagList.value = tags
    }

    fun loadCategoryAndTags(serverConfig: ServerConfig) {
        if (isLoading.value) {
            _isLoading.value = true
            updateCategoryAndTags(serverConfig).invokeOnCompletion {
                _isLoading.value = false
            }
        }
    }

    sealed class Event {
        data class Error(val error: RequestError) : Event()
        object TorrentCreated : Event()
    }
}
