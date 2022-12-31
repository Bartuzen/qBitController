package dev.bartuzen.qbitcontroller.ui.torrent.tabs.overview.category

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.bartuzen.qbitcontroller.data.repositories.TorrentRepository
import dev.bartuzen.qbitcontroller.model.ServerConfig
import dev.bartuzen.qbitcontroller.network.RequestResult
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TorrentCategoryViewModel @Inject constructor(
    private val repository: TorrentRepository
) : ViewModel() {
    private val eventChannel = Channel<Event>()
    val eventFlow = eventChannel.receiveAsFlow()

    private val _categories = MutableStateFlow<List<String>?>(null)
    val categories = _categories.asStateFlow()

    var isInitialLoadStarted = false

    fun updateCategories(serverConfig: ServerConfig) = viewModelScope.launch {
        when (val result = repository.getCategories(serverConfig)) {
            is RequestResult.Success -> {
                _categories.value = result.data.values
                    .toList()
                    .map { it.name }
                    .sortedBy { it }
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
