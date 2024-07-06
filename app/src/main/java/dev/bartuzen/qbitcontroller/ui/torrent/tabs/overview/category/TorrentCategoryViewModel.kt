package dev.bartuzen.qbitcontroller.ui.torrent.tabs.overview.category

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.bartuzen.qbitcontroller.data.repositories.torrent.TorrentCategoryRepository
import dev.bartuzen.qbitcontroller.network.RequestResult
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TorrentCategoryViewModel @Inject constructor(
    private val repository: TorrentCategoryRepository,
) : ViewModel() {
    private val eventChannel = Channel<Event>()
    val eventFlow = eventChannel.receiveAsFlow()

    private val _categories = MutableStateFlow<List<String>?>(null)
    val categories = _categories.asStateFlow()

    var isInitialLoadStarted = false

    fun updateCategories(serverId: Int) = viewModelScope.launch {
        when (val result = repository.getCategories(serverId)) {
            is RequestResult.Success -> {
                _categories.value = result.data.values
                    .toList()
                    .map { it.name }
                    .sorted()
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
