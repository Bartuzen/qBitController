package dev.bartuzen.qbitcontroller.ui.search.start

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.bartuzen.qbitcontroller.data.repositories.search.SearchStartRepository
import dev.bartuzen.qbitcontroller.model.Plugin
import dev.bartuzen.qbitcontroller.network.RequestResult
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchStartViewModel @Inject constructor(
    private val repository: SearchStartRepository,
    private val state: SavedStateHandle
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

    fun saveState(
        searchQuery: String,
        selectedCategoryPosition: Int,
        selectedPluginOption: SearchStartAdapter.PluginSelection,
        selectedPlugins: List<String>
    ) {
        state["searchQuery"] = searchQuery
        state["selectedCategoryPosition"] = selectedCategoryPosition
        state["selectedPluginOption"] = selectedPluginOption
        state["selectedPlugins"] = selectedPlugins
    }

    fun getState(): State? {
        val searchQuery: String = state["searchQuery"] ?: return null
        val selectedCategoryPosition: Int = state["selectedCategoryPosition"] ?: return null
        val selectedPluginOption: SearchStartAdapter.PluginSelection = state["selectedPluginOption"] ?: return null
        val selectedPlugins: List<String> = state["selectedPlugins"] ?: return null

        return State(
            searchQuery = searchQuery,
            selectedCategoryPosition = selectedCategoryPosition,
            selectedPluginOption = selectedPluginOption,
            selectedPlugins = selectedPlugins
        )
    }

    data class State(
        val searchQuery: String,
        val selectedCategoryPosition: Int,
        val selectedPluginOption: SearchStartAdapter.PluginSelection,
        val selectedPlugins: List<String>
    )

    sealed class Event {
        data class Error(val error: RequestResult.Error) : Event()
    }
}
