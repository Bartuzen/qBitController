package dev.bartuzen.qbitcontroller.ui.search.plugins

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.bartuzen.qbitcontroller.data.repositories.search.SearchPluginsRepository
import dev.bartuzen.qbitcontroller.model.Plugin
import dev.bartuzen.qbitcontroller.network.RequestResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import javax.inject.Inject

@HiltViewModel
class SearchPluginsViewModel @Inject constructor(
    private val repository: SearchPluginsRepository
) : ViewModel() {
    private val _plugins = MutableStateFlow<List<Plugin>?>(null)
    val plugins = _plugins.asStateFlow()

    private val eventChannel = Channel<Event>()
    val eventFlow = eventChannel.receiveAsFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    var isInitialLoadStarted = false

    private fun updatePlugins(serverId: Int) = viewModelScope.launch {
        when (val result = repository.getPlugins(serverId)) {
            is RequestResult.Success -> {
                _plugins.value = result.data.sortedBy { it.fullName }
            }
            is RequestResult.Error -> {
                eventChannel.send(Event.Error(result))
            }
        }
    }

    fun loadPlugins(serverId: Int) {
        if (!isLoading.value) {
            _isLoading.value = true
            updatePlugins(serverId).invokeOnCompletion {
                _isLoading.value = false
            }
        }
    }

    fun refreshPlugins(serverId: Int) {
        if (!isRefreshing.value) {
            _isRefreshing.value = true
            updatePlugins(serverId).invokeOnCompletion {
                _isRefreshing.value = false
            }
        }
    }

    fun updatePluginStates(serverId: Int, newStates: Map<String, Boolean>) = viewModelScope.launch {
        val plugins = plugins.value ?: return@launch

        val pluginsToEnable = plugins.filter { newStates[it.name] == true }.map { it.name }
        val pluginsToDisable = plugins.filter { newStates[it.name] == false }.map { it.name }

        val job = Job()

        viewModelScope.launch(job) {
            val enablePluginsDeferred = launch enable@{
                if (pluginsToEnable.isEmpty()) {
                    return@enable
                }

                when (val result = repository.enablePlugins(serverId, pluginsToEnable, true)) {
                    is RequestResult.Success -> {}
                    is RequestResult.Error -> {
                        yield()
                        eventChannel.send(Event.Error(result))
                        job.cancel()
                    }
                }
            }
            val disablePluginsDeferred = launch disable@{
                if (pluginsToDisable.isEmpty()) {
                    return@disable
                }

                when (val result = repository.enablePlugins(serverId, pluginsToDisable, false)) {
                    is RequestResult.Success -> {}
                    is RequestResult.Error -> {
                        yield()
                        eventChannel.send(Event.Error(result))
                        job.cancel()
                    }
                }
            }

            enablePluginsDeferred.join()
            disablePluginsDeferred.join()

            eventChannel.send(Event.PluginsStateUpdated)
        }
    }

    sealed class Event {
        data class Error(val error: RequestResult.Error) : Event()
        object PluginsStateUpdated : Event()
    }
}