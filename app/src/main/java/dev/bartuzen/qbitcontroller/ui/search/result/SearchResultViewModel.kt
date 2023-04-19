package dev.bartuzen.qbitcontroller.ui.search.result

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.bartuzen.qbitcontroller.data.repositories.search.SearchResultRepository
import dev.bartuzen.qbitcontroller.model.Search
import dev.bartuzen.qbitcontroller.network.RequestResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchResultViewModel @Inject constructor(
    private val repository: SearchResultRepository,
    private val state: SavedStateHandle
) : ViewModel() {
    private val _searchResult = MutableStateFlow<Search?>(null)
    val searchResult = _searchResult.asStateFlow()

    private val eventChannel = Channel<Event>()
    val eventFlow = eventChannel.receiveAsFlow()

    private val isLoading = MutableStateFlow(false)

    private val _isSearchContinuing = MutableStateFlow(true)
    val isSearchContinuing = _isSearchContinuing.asStateFlow()

    private var searchId: Int? = state["searchId"]
        set(value) {
            state["searchId"] = value
            field = value
        }

    fun startSearch(serverId: Int, pattern: String, category: String, plugins: String) = viewModelScope.launch {
        when (val result = repository.startSearch(serverId, pattern, category, plugins)) {
            is RequestResult.Success -> {
                searchId = result.data.id
            }
            is RequestResult.Error -> {
                eventChannel.send(Event.Error(result))
            }
        }
    }

    fun stopSearch(serverId: Int) = viewModelScope.launch {
        if (!isSearchContinuing.value) {
            return@launch
        }
        val searchId = searchId ?: return@launch

        when (val result = repository.stopSearch(serverId, searchId)) {
            is RequestResult.Success -> {
                updateResults(serverId).invokeOnCompletion {
                    _isSearchContinuing.value = false
                }
            }
            is RequestResult.Error -> {
                eventChannel.send(Event.Error(result))
            }
        }
    }

    fun deleteSearch(serverId: Int) = CoroutineScope(Dispatchers.Main).launch {
        val searchId = searchId ?: return@launch
        repository.deleteSearch(serverId, searchId)
    }

    private fun updateResults(serverId: Int) = viewModelScope.launch {
        searchId?.let { searchId ->
            when (val result = repository.getSearchResults(serverId, searchId)) {
                is RequestResult.Success -> {
                    _searchResult.value = result.data
                    if (result.data.status == Search.Status.STOPPED) {
                        _isSearchContinuing.value = false
                        eventChannel.send(Event.SearchStopped)
                    }
                }
                is RequestResult.Error -> {
                    eventChannel.send(Event.Error(result))
                }
            }
        }
    }

    fun loadResults(serverId: Int) {
        if (!isLoading.value) {
            isLoading.value = true
            updateResults(serverId).invokeOnCompletion {
                isLoading.value = false
            }
        }
    }

    sealed class Event {
        data class Error(val error: RequestResult.Error) : Event()
        object SearchStopped : Event()
    }
}
