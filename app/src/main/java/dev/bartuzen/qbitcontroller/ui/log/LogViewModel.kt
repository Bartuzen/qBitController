package dev.bartuzen.qbitcontroller.ui.log

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.bartuzen.qbitcontroller.data.repositories.log.LogRepository
import dev.bartuzen.qbitcontroller.model.Log
import dev.bartuzen.qbitcontroller.network.RequestResult
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

@HiltViewModel(assistedFactory = LogViewModel.Factory::class)
class LogViewModel @AssistedInject constructor(
    @Assisted private val serverId: Int,
    private val repository: LogRepository,
) : ViewModel() {
    @AssistedFactory
    interface Factory {
        fun create(serverId: Int): LogViewModel
    }

    private val _logs = MutableStateFlow<List<Log>?>(null)
    val logs = _logs.asStateFlow()

    private val eventChannel = Channel<Event>()
    val eventFlow = eventChannel.receiveAsFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    var isInitialLoadStarted = false

    init {
        loadLogs()
    }

    private fun updateLogs() = viewModelScope.launch {
        when (val result = repository.getLog(serverId)) {
            is RequestResult.Success -> {
                _logs.value = result.data.reversed()
            }
            is RequestResult.Error -> {
                eventChannel.send(Event.Error(result))
            }
        }
    }

    fun loadLogs() {
        if (!isLoading.value) {
            _isLoading.value = true
            updateLogs().invokeOnCompletion {
                _isLoading.value = false
            }
        }
    }

    fun refreshLogs() {
        if (!isRefreshing.value) {
            _isRefreshing.value = true
            updateLogs().invokeOnCompletion {
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
