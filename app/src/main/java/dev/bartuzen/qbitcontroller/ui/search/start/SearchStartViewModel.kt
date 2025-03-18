package dev.bartuzen.qbitcontroller.ui.search.start

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.bartuzen.qbitcontroller.data.repositories.search.SearchStartRepository
import dev.bartuzen.qbitcontroller.model.Plugin
import dev.bartuzen.qbitcontroller.network.RequestResult
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

@HiltViewModel(assistedFactory = SearchStartViewModel.Factory::class)
class SearchStartViewModel @AssistedInject constructor(
    @Assisted private val serverId: Int,
    private val repository: SearchStartRepository,
) : ViewModel() {
    @AssistedFactory
    interface Factory {
        fun create(serverId: Int): SearchStartViewModel
    }

    private val _plugins = MutableStateFlow<List<Plugin>?>(null)
    val plugins = _plugins.asStateFlow()

    private val eventChannel = Channel<Event>()
    val eventFlow = eventChannel.receiveAsFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    init {
        loadPlugins()
    }

    private fun updatePlugins() = viewModelScope.launch {
        when (val result = repository.getPlugins(serverId)) {
            is RequestResult.Success -> {
                _plugins.value = result.data.sortedBy { it.fullName }
            }
            is RequestResult.Error -> {
                eventChannel.send(Event.Error(result))
            }
        }
    }

    fun loadPlugins() {
        if (!isLoading.value) {
            _isLoading.value = true
            updatePlugins().invokeOnCompletion {
                _isLoading.value = false
            }
        }
    }

    fun refreshPlugins() {
        if (!isRefreshing.value) {
            _isRefreshing.value = true
            updatePlugins().invokeOnCompletion {
                viewModelScope.launch {
                    delay(25)
                    _isRefreshing.value = false
                }
            }
        }
    }

    sealed class Event {
        data class Error(val error: RequestResult.Error) : Event()
    }
}
