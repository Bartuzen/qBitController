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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchResultViewModel @Inject constructor(
    private val repository: SearchResultRepository,
    private val state: SavedStateHandle
) : ViewModel() {
    private val searchResult = MutableStateFlow<Search?>(null)

    private val searchQuery = MutableStateFlow("")

    private val _filter = MutableStateFlow(
        Filter(
            seedsMin = null,
            seedsMax = null,
            sizeMin = null,
            sizeMax = null,
            sizeMinUnit = 2,
            sizeMaxUnit = 2
        )
    )
    val filter = _filter.asStateFlow()

    val searchResults = combine(searchResult, searchQuery, filter) { searchResult, searchQuery, filter ->
        if (searchResult != null) {
            Triple(searchResult, searchQuery, filter)
        } else {
            null
        }
    }.filterNotNull().map { (searchResult, searchQuery, filter) ->
        searchResult.results.filter { result ->
            if (searchQuery.isNotEmpty() && !result.fileName.contains(searchQuery, ignoreCase = true)) {
                return@filter false
            }

            if (filter.seedsMin != null && (result.seeders ?: -1) < filter.seedsMin) {
                return@filter false
            }
            if (filter.seedsMax != null && (result.seeders ?: Int.MAX_VALUE) > filter.seedsMax) {
                return@filter false
            }

            if (filter.sizeMinBytes != null && (result.fileSize ?: -1) < filter.sizeMinBytes) {
                return@filter false
            }
            if (filter.sizeMaxBytes != null && (result.fileSize ?: Long.MAX_VALUE) > filter.sizeMaxBytes) {
                return@filter false
            }

            return@filter true
        }
    }

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
                    searchResult.value = result.data
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

    fun setSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun setFilter(filter: Filter) {
        _filter.value = filter
    }

    data class Filter(
        val seedsMin: Int?,
        val seedsMax: Int?,
        val sizeMin: Long?,
        val sizeMax: Long?,
        val sizeMinUnit: Int,
        val sizeMaxUnit: Int
    ) {
        private fun Int.pow(x: Int): Long {
            var number = 1L
            repeat(x) {
                number *= this
            }
            return number
        }

        val sizeMinBytes = if (sizeMin != null) {
            sizeMin * 1024.pow(sizeMinUnit)
        } else {
            null
        }

        val sizeMaxBytes = if (sizeMax != null) {
            sizeMax * 1024.pow(sizeMaxUnit)
        } else {
            null
        }
    }

    sealed class Event {
        data class Error(val error: RequestResult.Error) : Event()
        object SearchStopped : Event()
    }
}
