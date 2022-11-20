package dev.bartuzen.qbitcontroller.ui.addtorrent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.bartuzen.qbitcontroller.data.repositories.AddTorrentRepository
import dev.bartuzen.qbitcontroller.model.ServerConfig
import dev.bartuzen.qbitcontroller.network.RequestError
import dev.bartuzen.qbitcontroller.network.RequestResult
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddTorrentViewModel @Inject constructor(
    private val repository: AddTorrentRepository
) : ViewModel() {
    private val _eventChannel = Channel<Event>()
    val eventFlow = _eventChannel.receiveAsFlow()

    private var isCreating = false

    fun createTorrent(
        serverConfig: ServerConfig,
        links: List<String>,
        isPaused: Boolean,
        skipHashChecking: Boolean,
        isAutoTorrentManagementEnabled: Boolean,
        isSequentialDownloadEnabled: Boolean,
        isFirstLastPiecePrioritized: Boolean
    ) = viewModelScope.launch {
        if (!isCreating) {
            isCreating = true
            when (
                val result = repository.createTorrent(
                    serverConfig,
                    links,
                    isPaused,
                    skipHashChecking,
                    isAutoTorrentManagementEnabled,
                    isSequentialDownloadEnabled,
                    isFirstLastPiecePrioritized
                )
            ) {
                is RequestResult.Success -> {
                    _eventChannel.send(Event.TorrentCreated)
                }
                is RequestResult.Error -> {
                    _eventChannel.send(Event.Error(result.error))
                }
            }
            isCreating = false
        }
    }

    sealed class Event {
        data class Error(val error: RequestError) : Event()
        object TorrentCreated : Event()
    }
}
