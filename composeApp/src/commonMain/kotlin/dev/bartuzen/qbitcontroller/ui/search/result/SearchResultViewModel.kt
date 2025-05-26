package dev.bartuzen.qbitcontroller.ui.search.result

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.bartuzen.qbitcontroller.data.SearchSort
import dev.bartuzen.qbitcontroller.data.SettingsManager
import dev.bartuzen.qbitcontroller.data.repositories.search.SearchResultRepository
import dev.bartuzen.qbitcontroller.model.Search
import dev.bartuzen.qbitcontroller.network.RequestResult
import dev.bartuzen.qbitcontroller.utils.getSerializableStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class SearchResultViewModel(
    private val serverId: Int,
    private val searchQuery: String,
    private val category: String,
    private val plugins: String,
    private val repository: SearchResultRepository,
    private val settingsManager: SettingsManager,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val searchResult = MutableStateFlow<Search?>(null)

    private val filterQuery = savedStateHandle.getStateFlow("filterQuery", "")

    val filter = savedStateHandle.getSerializableStateFlow(viewModelScope, "filter", Filter())

    val searchSort = settingsManager.searchSort.flow
    val isReverseSearchSorting = settingsManager.isReverseSearchSorting.flow

    private val sortedResults =
        combine(searchResult, searchSort, isReverseSearchSorting) { searchResult, searchSort, isReverseSearchSorting ->
            if (searchResult != null) {
                Triple(searchResult.results, searchSort, isReverseSearchSorting)
            } else {
                null
            }
        }.filterNotNull().map { (searchResults, searchSort, isReverseSearchSorting) ->
            withContext(Dispatchers.Default) {
                searchResults.run {
                    val comparator = when (searchSort) {
                        SearchSort.NAME -> {
                            compareBy(String.CASE_INSENSITIVE_ORDER, Search.Result::fileName)
                        }
                        SearchSort.SIZE -> {
                            compareBy(Search.Result::fileSize)
                                .thenBy(String.CASE_INSENSITIVE_ORDER, Search.Result::fileName)
                        }
                        SearchSort.SEEDERS -> {
                            compareBy(Search.Result::seeders)
                                .thenBy(String.CASE_INSENSITIVE_ORDER, Search.Result::fileName)
                        }
                        SearchSort.LEECHERS -> {
                            compareBy(Search.Result::leechers)
                                .thenBy(String.CASE_INSENSITIVE_ORDER, Search.Result::fileName)
                        }
                        SearchSort.SEARCH_ENGINE -> {
                            compareBy(String.CASE_INSENSITIVE_ORDER, Search.Result::siteUrl)
                                .thenBy(String.CASE_INSENSITIVE_ORDER, Search.Result::siteUrl)
                        }
                    }
                    sortedWith(comparator)
                }.run {
                    if (isReverseSearchSorting) {
                        reversed()
                    } else {
                        this
                    }
                }.distinct()
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val searchResults = combine(sortedResults, filterQuery, filter) { searchResults, filterQuery, filter ->
        Triple(searchResults, filterQuery, filter)
    }.filterNotNull().map { (searchResults, filterQuery, filter) ->
        searchResults?.filter { result ->
            if (filterQuery.isNotEmpty()) {
                val matchesFilterQuery = filterQuery
                    .split(" ")
                    .filter { it.isNotEmpty() && it != "-" }
                    .all { term ->
                        val isExclusion = term.startsWith("-")
                        val cleanTerm = term.removePrefix("-")
                        val containsTerm = result.fileName.contains(cleanTerm, ignoreCase = true)

                        if (isExclusion) !containsTerm else containsTerm
                    }
                if (!matchesFilterQuery) {
                    return@filter false
                }
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
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val searchCount = sortedResults
        .map { it?.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val eventChannel = Channel<Event>()
    val eventFlow = eventChannel.receiveAsFlow()

    private val isLoading = MutableStateFlow(false)

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    private val _isSearchContinuing = MutableStateFlow(true)
    val isSearchContinuing = _isSearchContinuing.asStateFlow()

    private var searchId: Int? = savedStateHandle["searchId"]
        set(value) {
            savedStateHandle["searchId"] = value
            field = value
        }

    init {
        if (searchId == null) {
            startSearch()
        }

        loadResults()
        viewModelScope.launch {
            combine(isSearchContinuing, isLoading) { isSearchContinuing, isLoading ->
                isSearchContinuing to isLoading
            }.collectLatest { (isSearchContinuing, isLoading) ->
                if (isSearchContinuing && !isLoading) {
                    delay(1000)
                    loadResults()
                }
            }
        }
    }

    override fun onCleared() {
        deleteSearch()
    }

    private fun startSearch() = viewModelScope.launch {
        when (val result = repository.startSearch(serverId, searchQuery, category, plugins)) {
            is RequestResult.Success -> {
                searchId = result.data.id
            }
            is RequestResult.Error -> {
                eventChannel.send(Event.Error(result))
                _isSearchContinuing.value = false
            }
        }
    }

    fun stopSearch() = viewModelScope.launch {
        if (!isSearchContinuing.value) {
            return@launch
        }
        val searchId = searchId ?: return@launch

        when (val result = repository.stopSearch(serverId, searchId)) {
            is RequestResult.Success -> {
                eventChannel.send(Event.SearchStopped)
                updateResults().invokeOnCompletion {
                    _isSearchContinuing.value = false
                }
            }
            is RequestResult.Error -> {
                eventChannel.send(Event.Error(result))
            }
        }
    }

    private fun deleteSearch() = CoroutineScope(Dispatchers.Main).launch {
        val searchId = searchId ?: return@launch
        repository.deleteSearch(serverId, searchId)
    }

    private fun updateResults() = viewModelScope.launch {
        searchId?.let { searchId ->
            when (val result = repository.getSearchResults(serverId, searchId)) {
                is RequestResult.Success -> {
                    searchResult.value = result.data
                    if (result.data.status == Search.Status.STOPPED) {
                        _isSearchContinuing.value = false
                    }
                }
                is RequestResult.Error -> {
                    eventChannel.send(Event.Error(result))
                    _isSearchContinuing.value = false
                }
            }
        }
    }

    private fun loadResults() {
        if (!isLoading.value) {
            isLoading.value = true
            updateResults().invokeOnCompletion {
                isLoading.value = false
            }
        }
    }

    fun refresh() = viewModelScope.launch {
        _isRefreshing.value = true
        val searchId = searchId

        if (searchId == null) {
            when (val result = repository.startSearch(serverId, searchQuery, category, plugins)) {
                is RequestResult.Success -> {
                    this@SearchResultViewModel.searchId = result.data.id
                    _isSearchContinuing.value = true
                }
                is RequestResult.Error -> {
                    eventChannel.send(Event.Error(result))
                }
            }
        } else {
            when (val result = repository.getSearchResults(serverId, searchId)) {
                is RequestResult.Success -> {
                    searchResult.value = result.data
                    _isSearchContinuing.value = result.data.status != Search.Status.STOPPED
                }
                is RequestResult.Error -> {
                    eventChannel.send(Event.Error(result))
                }
            }
        }
        delay(25)
        _isRefreshing.value = false
    }

    fun setFilterQuery(filterQuery: String) {
        savedStateHandle["filterQuery"] = filterQuery
    }

    fun setFilter(filter: Filter) {
        savedStateHandle["filter"] = Json.encodeToString(filter)
    }

    fun resetFilter() {
        setFilter(Filter())
    }

    fun setSearchSort(searchSort: SearchSort) {
        settingsManager.searchSort.value = searchSort
    }

    fun changeReverseSorting() {
        settingsManager.isReverseSearchSorting.value = !isReverseSearchSorting.value
    }

    @Serializable
    data class Filter(
        val seedsMin: Int? = null,
        val seedsMax: Int? = null,
        val sizeMin: Long? = null,
        val sizeMax: Long? = null,
        val sizeMinUnit: Int = 2,
        val sizeMaxUnit: Int = 2,
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
        data object SearchStopped : Event()
    }
}
