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
import kotlinx.coroutines.Dispatchers
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
    private val _eventChannel = Channel<Event>()
    val eventFlow = _eventChannel.receiveAsFlow()

    val serversFlow = settingsManager.serversFlow

    private val _isCreating = MutableStateFlow(false)
    val isCreating = _isCreating.asStateFlow()

    fun createTorrent(
        serverConfig: ServerConfig,
        links: List<String>?,
        fileUri: Uri?,
        torrentName: String?,
        downloadSpeedLimit: Int?,
        uploadSpeedLimit: Int?,
        ratioLimit: Double?,
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
                torrentName,
                downloadSpeedLimit,
                uploadSpeedLimit,
                ratioLimit,
                isPaused,
                skipHashChecking,
                isAutoTorrentManagementEnabled,
                isSequentialDownloadEnabled,
                isFirstLastPiecePrioritized
            )) {
                is RequestResult.Success -> {
                    _eventChannel.send(Event.TorrentCreated)
                }
                is RequestResult.Error -> {
                    _eventChannel.send(Event.Error(result.error))
                }
            }
            _isCreating.value = false
        }
    }

    sealed class Event {
        data class Error(val error: RequestError) : Event()
        object TorrentCreated : Event()
    }
}
