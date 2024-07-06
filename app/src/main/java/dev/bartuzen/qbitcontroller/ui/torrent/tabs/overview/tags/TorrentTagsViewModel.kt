package dev.bartuzen.qbitcontroller.ui.torrent.tabs.overview.tags

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.bartuzen.qbitcontroller.data.repositories.torrent.TorrentTagsRepository
import dev.bartuzen.qbitcontroller.network.RequestResult
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TorrentTagsViewModel @Inject constructor(
    private val repository: TorrentTagsRepository,
) : ViewModel() {
    private val eventChannel = Channel<Event>()
    val eventFlow = eventChannel.receiveAsFlow()

    private val _tags = MutableStateFlow<List<String>?>(null)
    val tags = _tags.asStateFlow()

    var isInitialLoadStarted = false

    fun updateTags(serverId: Int) = viewModelScope.launch {
        when (val result = repository.getTags(serverId)) {
            is RequestResult.Success -> {
                _tags.value = result.data.sorted()
            }
            is RequestResult.Error -> {
                eventChannel.send(Event.Error(result))
            }
        }
    }

    sealed class Event {
        data class Error(val result: RequestResult.Error) : Event()
    }
}
