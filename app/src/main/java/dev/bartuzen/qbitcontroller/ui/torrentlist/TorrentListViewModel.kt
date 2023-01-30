package dev.bartuzen.qbitcontroller.ui.torrentlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.bartuzen.qbitcontroller.data.SettingsManager
import dev.bartuzen.qbitcontroller.data.TorrentSort
import dev.bartuzen.qbitcontroller.data.repositories.TorrentListRepository
import dev.bartuzen.qbitcontroller.model.MainData
import dev.bartuzen.qbitcontroller.model.Torrent
import dev.bartuzen.qbitcontroller.model.deserializers.parseMainData
import dev.bartuzen.qbitcontroller.network.RequestResult
import dev.bartuzen.qbitcontroller.utils.Quintuple
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class TorrentListViewModel @Inject constructor(
    private val repository: TorrentListRepository,
    private val settingsManager: SettingsManager
) : ViewModel() {
    private val _mainData = MutableStateFlow<MainData?>(null)
    val mainData = _mainData.asStateFlow()

    private val searchQuery = MutableStateFlow("")
    private val selectedCategory = MutableStateFlow<CategoryTag.ICategory>(CategoryTag.AllCategories)
    private val selectedTag = MutableStateFlow<CategoryTag.ITag>(CategoryTag.AllTags)
    private val selectedFilter = MutableStateFlow(TorrentFilter.ALL)

    val torrentSort = settingsManager.sort.flow
    val isReverseSorting = settingsManager.isReverseSorting.flow
    val autoRefreshInterval = settingsManager.autoRefreshInterval.flow
    val autoRefreshHideLoadingBar = settingsManager.autoRefreshHideLoadingBar.flow

    private val eventChannel = Channel<Event>()
    val eventFlow = eventChannel.receiveAsFlow()

    private val _isNaturalLoading = MutableStateFlow<Boolean?>(null)
    val isNaturalLoading = _isNaturalLoading.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    private val sortedTorrentList =
        combine(mainData, torrentSort, isReverseSorting) { mainData, torrentSort, isReverseSorting ->
            if (mainData != null) {
                Triple(mainData.torrents, torrentSort, isReverseSorting)
            } else {
                null
            }
        }.filterNotNull().map { (torrentList, torrentSort, isReverseSorting) ->
            withContext(Dispatchers.IO) {
                torrentList.run {
                    when (torrentSort) {
                        TorrentSort.NAME -> sortedWith(compareBy({ it.name.lowercase() }, { it.hash }))
                        TorrentSort.HASH -> sortedBy { it.hash }
                        TorrentSort.DOWNLOAD_SPEED -> sortedWith(compareBy({ it.downloadSpeed }, { it.hash }))
                        TorrentSort.UPLOAD_SPEED -> sortedWith(compareBy({ it.uploadSpeed }, { it.hash }))
                        TorrentSort.PRIORITY -> sortedWith(
                            compareBy<Torrent, Int?>(nullsLast()) { it.priority }.thenBy { it.hash }
                        )
                        TorrentSort.ETA -> sortedWith(
                            compareBy<Torrent, Int?>(nullsLast()) { it.eta }.thenBy { it.hash }
                        )
                        TorrentSort.SIZE -> sortedWith(compareBy({ it.size }, { it.hash }))
                        TorrentSort.PROGRESS -> sortedWith(compareBy({ it.progress }, { it.hash }))
                        TorrentSort.CONNECTED_SEEDS -> sortedWith(compareBy({ it.connectedSeeds }, { it.hash }))
                        TorrentSort.TOTAL_SEEDS -> sortedWith(compareBy({ it.totalSeeds }, { it.hash }))
                        TorrentSort.CONNECTED_LEECHES -> sortedWith(compareBy({ it.connectedLeeches }, { it.hash }))
                        TorrentSort.TOTAL_LEECHES -> sortedWith(compareBy({ it.totalLeeches }, { it.hash }))
                        TorrentSort.ADDITION_DATE -> sortedWith(compareBy({ it.additionDate }, { it.hash }))
                        TorrentSort.COMPLETION_DATE -> sortedWith(
                            compareBy<Torrent, Long?>(nullsLast()) { it.completionDate }.thenBy { it.hash }
                        )
                    }
                }.run {
                    if (isReverseSorting) {
                        reversed()
                    } else {
                        this
                    }
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    val filteredTorrentList = combine(
        sortedTorrentList,
        searchQuery,
        selectedCategory,
        selectedTag,
        selectedFilter
    ) { torrentList, searchQuery, selectedCategory, selectedTag, selectedFilter ->
        if (torrentList != null) {
            Quintuple(torrentList, searchQuery, selectedCategory, selectedTag, selectedFilter)
        } else {
            null
        }
    }.filterNotNull().map { (torrentList, searchQuery, selectedCategory, selectedTag, selectedFilter) ->
        withContext(Dispatchers.IO) {
            torrentList.filter { torrent ->
                if (searchQuery.isNotEmpty()) {
                    if (!torrent.name.contains(searchQuery, ignoreCase = true)) {
                        return@filter false
                    }
                }

                when (selectedCategory) {
                    CategoryTag.AllCategories -> {}
                    CategoryTag.Uncategorized -> {
                        if (torrent.category != null) {
                            return@filter false
                        }
                    }
                    is CategoryTag.Category -> {
                        if (torrent.category != selectedCategory.name) {
                            return@filter false
                        }
                    }
                }

                when (selectedTag) {
                    CategoryTag.AllTags -> {}
                    CategoryTag.Untagged -> {
                        if (torrent.tags.isNotEmpty()) {
                            return@filter false
                        }
                    }
                    is CategoryTag.Tag -> {
                        if (selectedTag.name !in torrent.tags) {
                            return@filter false
                        }
                    }
                }

                val selectedStates = selectedFilter.states
                if (selectedStates != null && torrent.state !in selectedStates) {
                    return@filter false
                }
                true
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    var isInitialLoadStarted = false

    private fun updateMainData(serverId: Int) = viewModelScope.launch {
        when (val result = repository.getMainData(serverId)) {
            is RequestResult.Success -> {
                _mainData.value = parseMainData(result.data)
            }
            is RequestResult.Error -> {
                eventChannel.send(Event.Error(result))
            }
        }
    }

    fun loadMainData(serverId: Int, autoRefresh: Boolean = false) {
        if (isNaturalLoading.value == null) {
            _isNaturalLoading.value = !autoRefresh
            updateMainData(serverId).invokeOnCompletion {
                _isNaturalLoading.value = null
            }
        }
    }

    fun refreshMainData(serverId: Int) {
        if (!isRefreshing.value) {
            _isRefreshing.value = true
            updateMainData(serverId).invokeOnCompletion {
                _isRefreshing.value = false
            }
        }
    }

    fun deleteTorrents(serverId: Int, hashes: List<String>, deleteFiles: Boolean) = viewModelScope.launch {
        when (val result = repository.deleteTorrents(serverId, hashes, deleteFiles)) {
            is RequestResult.Success -> {
                eventChannel.send(Event.TorrentsDeleted(hashes.size))
            }
            is RequestResult.Error -> {
                eventChannel.send(Event.Error(result))
            }
        }
    }

    fun pauseTorrents(serverId: Int, hashes: List<String>) = viewModelScope.launch {
        when (val result = repository.pauseTorrents(serverId, hashes)) {
            is RequestResult.Success -> {
                eventChannel.send(Event.TorrentsPaused(hashes.size))
            }
            is RequestResult.Error -> {
                eventChannel.send(Event.Error(result))
            }
        }
    }

    fun resumeTorrents(serverId: Int, hashes: List<String>) = viewModelScope.launch {
        when (val result = repository.resumeTorrents(serverId, hashes)) {
            is RequestResult.Success -> {
                eventChannel.send(Event.TorrentsResumed(hashes.size))
            }
            is RequestResult.Error -> {
                eventChannel.send(Event.Error(result))
            }
        }
    }

    fun deleteCategory(serverId: Int, category: String) = viewModelScope.launch {
        when (val result = repository.deleteCategory(serverId, category)) {
            is RequestResult.Success -> {
                eventChannel.send(Event.CategoryDeleted(category))
            }
            is RequestResult.Error -> {
                eventChannel.send(Event.Error(result))
            }
        }
    }

    fun deleteTag(serverId: Int, tag: String) = viewModelScope.launch {
        when (val result = repository.deleteTag(serverId, tag)) {
            is RequestResult.Success -> {
                eventChannel.send(Event.TagDeleted(tag))
            }
            is RequestResult.Error -> {
                eventChannel.send(Event.Error(result))
            }
        }
    }

    fun increaseTorrentPriority(serverId: Int, hashes: List<String>) = viewModelScope.launch {
        when (val result = repository.increaseTorrentPriority(serverId, hashes)) {
            is RequestResult.Success -> {
                eventChannel.send(Event.TorrentsPriorityIncreased)
            }
            is RequestResult.Error -> {
                if (result is RequestResult.Error.ApiError && result.code == 409) {
                    eventChannel.send(Event.QueueingNotEnabled)
                } else {
                    eventChannel.send(Event.Error(result))
                }
            }
        }
    }

    fun decreaseTorrentPriority(serverId: Int, hashes: List<String>) = viewModelScope.launch {
        when (val result = repository.decreaseTorrentPriority(serverId, hashes)) {
            is RequestResult.Success -> {
                eventChannel.send(Event.TorrentsPriorityDecreased)
            }
            is RequestResult.Error -> {
                if (result is RequestResult.Error.ApiError && result.code == 409) {
                    eventChannel.send(Event.QueueingNotEnabled)
                } else {
                    eventChannel.send(Event.Error(result))
                }
            }
        }
    }

    fun maximizeTorrentPriority(serverId: Int, hashes: List<String>) = viewModelScope.launch {
        when (val result = repository.maximizeTorrentPriority(serverId, hashes)) {
            is RequestResult.Success -> {
                eventChannel.send(Event.TorrentsPriorityMaximized)
            }
            is RequestResult.Error -> {
                if (result is RequestResult.Error.ApiError && result.code == 409) {
                    eventChannel.send(Event.QueueingNotEnabled)
                } else {
                    eventChannel.send(Event.Error(result))
                }
            }
        }
    }

    fun minimizeTorrentPriority(serverId: Int, hashes: List<String>) = viewModelScope.launch {
        when (val result = repository.minimizeTorrentPriority(serverId, hashes)) {
            is RequestResult.Success -> {
                eventChannel.send(Event.TorrentsPriorityMinimized)
            }
            is RequestResult.Error -> {
                if (result is RequestResult.Error.ApiError && result.code == 409) {
                    eventChannel.send(Event.QueueingNotEnabled)
                } else {
                    eventChannel.send(Event.Error(result))
                }
            }
        }
    }

    fun setLocation(serverId: Int, hashes: List<String>, location: String) = viewModelScope.launch {
        when (val result = repository.setLocation(serverId, hashes, location)) {
            is RequestResult.Success -> {
                eventChannel.send(Event.LocationUpdated)
            }
            is RequestResult.Error -> {
                eventChannel.send(Event.Error(result))
            }
        }
    }

    fun createCategory(serverId: Int, name: String, savePath: String) = viewModelScope.launch {
        when (val result = repository.createCategory(serverId, name, savePath)) {
            is RequestResult.Success -> {
                eventChannel.send(Event.CategoryCreated)
            }
            is RequestResult.Error -> {
                eventChannel.send(Event.Error(result))
            }
        }
    }

    fun editCategory(serverId: Int, name: String, savePath: String) = viewModelScope.launch {
        when (val result = repository.editCategory(serverId, name, savePath)) {
            is RequestResult.Success -> {
                eventChannel.send(Event.CategoryEdited)
            }
            is RequestResult.Error -> {
                if (result is RequestResult.Error.ApiError && result.code == 409) {
                    eventChannel.send(Event.CategoryEditingFailed)
                } else {
                    eventChannel.send(Event.Error(result))
                }
            }
        }
    }

    fun createTags(serverId: Int, names: List<String>) = viewModelScope.launch {
        when (val result = repository.createTags(serverId, names)) {
            is RequestResult.Success -> {
                eventChannel.send(Event.TagCreated)
            }
            is RequestResult.Error -> {
                eventChannel.send(Event.Error(result))
            }
        }
    }

    fun setTorrentSort(torrentSort: TorrentSort) {
        settingsManager.sort.value = torrentSort
    }

    fun changeReverseSorting() {
        settingsManager.isReverseSorting.value = !isReverseSorting.value
    }

    fun setSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun setSelectedCategory(category: CategoryTag.ICategory) {
        selectedCategory.value = category
    }

    fun setSelectedTag(tag: CategoryTag.ITag) {
        selectedTag.value = tag
    }

    fun setSelectedFilter(filter: TorrentFilter) {
        selectedFilter.value = filter
    }

    sealed class Event {
        data class Error(val error: RequestResult.Error) : Event()
        object QueueingNotEnabled : Event()
        object CategoryEditingFailed : Event()
        data class TorrentsDeleted(val count: Int) : Event()
        data class TorrentsPaused(val count: Int) : Event()
        data class TorrentsResumed(val count: Int) : Event()
        data class CategoryDeleted(val name: String) : Event()
        data class TagDeleted(val name: String) : Event()
        object TorrentsPriorityIncreased : Event()
        object TorrentsPriorityDecreased : Event()
        object TorrentsPriorityMaximized : Event()
        object TorrentsPriorityMinimized : Event()
        object LocationUpdated : Event()
        object CategoryCreated : Event()
        object CategoryEdited : Event()
        object TagCreated : Event()
    }
}
