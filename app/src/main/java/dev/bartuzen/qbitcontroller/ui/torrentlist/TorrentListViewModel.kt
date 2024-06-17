package dev.bartuzen.qbitcontroller.ui.torrentlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.bartuzen.qbitcontroller.data.SettingsManager
import dev.bartuzen.qbitcontroller.data.TorrentSort
import dev.bartuzen.qbitcontroller.data.notification.TorrentDownloadedNotifier
import dev.bartuzen.qbitcontroller.data.repositories.TorrentListRepository
import dev.bartuzen.qbitcontroller.model.MainData
import dev.bartuzen.qbitcontroller.model.Torrent
import dev.bartuzen.qbitcontroller.network.RequestResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
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
import kotlinx.coroutines.yield
import javax.inject.Inject

@HiltViewModel
class TorrentListViewModel @Inject constructor(
    private val repository: TorrentListRepository,
    private val settingsManager: SettingsManager,
    private val notifier: TorrentDownloadedNotifier
) : ViewModel() {
    private val _mainData = MutableStateFlow<MainData?>(null)
    val mainData = _mainData.asStateFlow()

    private val searchQuery = MutableStateFlow("")
    private val selectedCategory = MutableStateFlow<CategoryTag>(CategoryTag.All)
    private val selectedTag = MutableStateFlow<CategoryTag>(CategoryTag.All)
    private val selectedFilter = MutableStateFlow(TorrentFilter.ALL)
    private val selectedTracker = MutableStateFlow<Tracker>(Tracker.All)

    val torrentSort = settingsManager.sort.flow
    val isReverseSorting = settingsManager.isReverseSorting.flow
    val autoRefreshInterval = settingsManager.autoRefreshInterval.flow
    val areTorrentSwipeActionsEnabled = settingsManager.areTorrentSwipeActionsEnabled.flow

    val areStatesCollapsed = settingsManager.areStatesCollapsed
    val areCategoriesCollapsed = settingsManager.areCategoriesCollapsed
    val areTagsCollapsed = settingsManager.areTagsCollapsed
    val areTrackersCollapsed = settingsManager.areTrackersCollapsed

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
                    val comparator = when (torrentSort) {
                        TorrentSort.NAME -> {
                            compareBy(String.CASE_INSENSITIVE_ORDER, Torrent::name)
                                .thenBy(Torrent::hash)
                        }
                        TorrentSort.STATUS -> {
                            compareBy(Torrent::state)
                                .thenBy(String.CASE_INSENSITIVE_ORDER, Torrent::name)
                                .thenBy(Torrent::hash)
                        }
                        TorrentSort.HASH -> {
                            compareBy(Torrent::hash)
                        }
                        TorrentSort.DOWNLOAD_SPEED -> {
                            compareBy(Torrent::downloadSpeed)
                                .thenBy(String.CASE_INSENSITIVE_ORDER, Torrent::name)
                                .thenBy(Torrent::hash)
                        }
                        TorrentSort.UPLOAD_SPEED -> {
                            compareBy(Torrent::uploadSpeed)
                                .thenBy(String.CASE_INSENSITIVE_ORDER, Torrent::name)
                                .thenBy(Torrent::hash)
                        }
                        TorrentSort.PRIORITY -> {
                            compareBy(nullsLast(), Torrent::priority)
                                .thenBy(String.CASE_INSENSITIVE_ORDER, Torrent::name)
                                .thenBy(Torrent::hash)
                        }
                        TorrentSort.ETA -> {
                            compareBy(nullsLast(), Torrent::eta)
                                .thenBy(String.CASE_INSENSITIVE_ORDER, Torrent::name)
                                .thenBy(Torrent::hash)
                        }
                        TorrentSort.SIZE -> {
                            compareBy(Torrent::size)
                                .thenBy(String.CASE_INSENSITIVE_ORDER, Torrent::name)
                                .thenBy(Torrent::hash)
                        }
                        TorrentSort.PROGRESS -> {
                            compareBy(Torrent::progress)
                                .thenBy(String.CASE_INSENSITIVE_ORDER, Torrent::name)
                                .thenBy(Torrent::hash)
                        }
                        TorrentSort.RATIO -> {
                            compareBy(Torrent::ratio)
                                .thenBy(String.CASE_INSENSITIVE_ORDER, Torrent::name)
                                .thenBy(Torrent::hash)
                        }
                        TorrentSort.CONNECTED_SEEDS -> {
                            compareBy(Torrent::connectedSeeds)
                                .thenBy(String.CASE_INSENSITIVE_ORDER, Torrent::name)
                                .thenBy(Torrent::hash)
                        }
                        TorrentSort.TOTAL_SEEDS -> {
                            compareBy(Torrent::totalSeeds)
                                .thenBy(String.CASE_INSENSITIVE_ORDER, Torrent::name)
                                .thenBy(Torrent::hash)
                        }
                        TorrentSort.CONNECTED_LEECHES -> {
                            compareBy(Torrent::connectedLeeches)
                                .thenBy(String.CASE_INSENSITIVE_ORDER, Torrent::name)
                                .thenBy(Torrent::hash)
                        }
                        TorrentSort.TOTAL_LEECHES -> {
                            compareBy(Torrent::totalLeeches)
                                .thenBy(String.CASE_INSENSITIVE_ORDER, Torrent::name)
                                .thenBy(Torrent::hash)
                        }
                        TorrentSort.ADDITION_DATE -> {
                            compareBy(Torrent::additionDate)
                                .thenBy(String.CASE_INSENSITIVE_ORDER, Torrent::name)
                                .thenBy(Torrent::hash)
                        }
                        TorrentSort.COMPLETION_DATE -> {
                            compareBy(nullsLast(), Torrent::completionDate)
                                .thenBy(String.CASE_INSENSITIVE_ORDER, Torrent::name)
                                .thenBy(Torrent::hash)
                        }
                        TorrentSort.LAST_ACTIVITY -> {
                            compareByDescending(Torrent::lastActivity)
                                .thenBy(String.CASE_INSENSITIVE_ORDER, Torrent::name)
                                .thenBy(Torrent::hash)
                        }
                    }
                    sortedWith(comparator)
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
        selectedFilter,
        selectedTracker,
        mainData
    ) { filters ->
        if (filters[0] != null && filters[6] != null) {
            filters
        } else {
            null
        }
    }.filterNotNull().map { filters ->
        @Suppress("UNCHECKED_CAST")
        val torrentList = filters[0] as List<Torrent>
        val searchQuery = filters[1] as String
        val selectedCategory = filters[2] as CategoryTag
        val selectedTag = filters[3] as CategoryTag
        val selectedFilter = filters[4] as TorrentFilter
        val selectedTracker = filters[5] as Tracker
        val mainData = filters[6] as MainData

        withContext(Dispatchers.IO) {
            torrentList.filter { torrent ->
                if (searchQuery.isNotEmpty()) {
                    if (!torrent.name.contains(searchQuery, ignoreCase = true)) {
                        return@filter false
                    }
                }

                when (selectedCategory) {
                    CategoryTag.All -> {}
                    CategoryTag.Uncategorized -> {
                        if (torrent.category != null) {
                            return@filter false
                        }
                    }
                    is CategoryTag.Item -> {
                        if (torrent.category == null) {
                            return@filter false
                        }

                        if (mainData.serverState.areSubcategoriesEnabled) {
                            if (!"${torrent.category}/".startsWith("${selectedCategory.name}/")) {
                                return@filter false
                            }
                        } else {
                            if (torrent.category != selectedCategory.name) {
                                return@filter false
                            }
                        }
                    }
                }

                when (selectedTag) {
                    CategoryTag.All -> {}
                    CategoryTag.Uncategorized -> {
                        if (torrent.tags.isNotEmpty()) {
                            return@filter false
                        }
                    }
                    is CategoryTag.Item -> {
                        if (selectedTag.name !in torrent.tags) {
                            return@filter false
                        }
                    }
                }

                when (selectedFilter) {
                    TorrentFilter.ACTIVE -> {
                        if (torrent.downloadSpeed == 0L && torrent.uploadSpeed == 0L) {
                            return@filter false
                        }
                    }
                    TorrentFilter.INACTIVE -> {
                        if (torrent.downloadSpeed != 0L || torrent.uploadSpeed != 0L) {
                            return@filter false
                        }
                    }
                    else -> {
                        val selectedStates = selectedFilter.states
                        if (selectedStates != null && torrent.state !in selectedStates) {
                            return@filter false
                        }
                    }
                }

                when (selectedTracker) {
                    Tracker.All -> {}
                    Tracker.Trackerless -> {
                        if (torrent.trackerCount != 0) {
                            return@filter false
                        }
                    }
                    is Tracker.Named -> {
                        if (torrent.hash !in selectedTracker.torrentHashes) {
                            return@filter false
                        }
                    }
                }

                true
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    var isInitialLoadStarted = false

    private fun updateMainData(serverId: Int) = viewModelScope.launch {
        when (val result = repository.getMainData(serverId)) {
            is RequestResult.Success -> {
                _mainData.value = result.data
                notifier.checkCompleted(serverId, result.data.torrents)
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

    fun createCategory(serverId: Int, name: String, savePath: String, downloadPathEnabled: Boolean?, downloadPath: String) =
        viewModelScope.launch {
            when (val result = repository.createCategory(serverId, name, savePath, downloadPathEnabled, downloadPath)) {
                is RequestResult.Success -> {
                    eventChannel.send(Event.CategoryCreated)
                }
                is RequestResult.Error -> {
                    eventChannel.send(Event.Error(result))
                }
            }
        }

    fun editCategory(serverId: Int, name: String, savePath: String, downloadPathEnabled: Boolean?, downloadPath: String) =
        viewModelScope.launch {
            when (val result = repository.editCategory(serverId, name, savePath, downloadPathEnabled, downloadPath)) {
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

    fun toggleSpeedLimitsMode(serverId: Int, isCurrentLimitAlternative: Boolean) = viewModelScope.launch {
        when (val result = repository.toggleSpeedLimitsMode(serverId)) {
            is RequestResult.Success -> {
                eventChannel.send(Event.SpeedLimitsToggled(!isCurrentLimitAlternative))
            }
            is RequestResult.Error -> {
                eventChannel.send(Event.Error(result))
            }
        }
    }

    fun shutdown(serverId: Int) = viewModelScope.launch {
        when (val result = repository.shutdown(serverId)) {
            is RequestResult.Success -> {
                eventChannel.send(Event.Shutdown)
            }
            is RequestResult.Error -> {
                eventChannel.send(Event.Error(result))
            }
        }
    }

    fun setCategory(serverId: Int, hashes: List<String>, category: String?) = viewModelScope.launch {
        when (val result = repository.setCategory(serverId, hashes, category)) {
            is RequestResult.Success -> {
                eventChannel.send(Event.TorrentCategoryUpdated)
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

    fun setSelectedCategory(category: CategoryTag) {
        selectedCategory.value = category
    }

    fun setSelectedTag(tag: CategoryTag) {
        selectedTag.value = tag
    }

    fun setSelectedFilter(filter: TorrentFilter) {
        selectedFilter.value = filter
    }

    fun setSelectedTracker(tracker: Tracker) {
        selectedTracker.value = tracker
    }

    fun setSpeedLimits(serverId: Int, download: Int?, upload: Int?) {
        val job = Job()

        viewModelScope.launch(job) {
            val downloadDeferred = launch download@{
                if (download == null) {
                    return@download
                }

                when (val result = repository.setDownloadSpeedLimit(serverId, download)) {
                    is RequestResult.Success -> {}
                    is RequestResult.Error -> {
                        yield()
                        eventChannel.send(Event.Error(result))
                        job.cancel()
                    }
                }
            }
            val uploadDeferred = launch upload@{
                if (upload == null) {
                    return@upload
                }

                when (val result = repository.setUploadSpeedLimit(serverId, upload)) {
                    is RequestResult.Success -> {}
                    is RequestResult.Error -> {
                        yield()
                        eventChannel.send(Event.Error(result))
                        job.cancel()
                    }
                }
            }

            downloadDeferred.join()
            uploadDeferred.join()

            eventChannel.send(Event.SpeedLimitsUpdated)
        }
    }

    sealed class Event {
        data class Error(val error: RequestResult.Error) : Event()
        data object QueueingNotEnabled : Event()
        data object CategoryEditingFailed : Event()
        data class TorrentsDeleted(val count: Int) : Event()
        data class TorrentsPaused(val count: Int) : Event()
        data class TorrentsResumed(val count: Int) : Event()
        data class CategoryDeleted(val name: String) : Event()
        data class TagDeleted(val name: String) : Event()
        data object TorrentsPriorityIncreased : Event()
        data object TorrentsPriorityDecreased : Event()
        data object TorrentsPriorityMaximized : Event()
        data object TorrentsPriorityMinimized : Event()
        data object LocationUpdated : Event()
        data object CategoryCreated : Event()
        data object CategoryEdited : Event()
        data object TagCreated : Event()
        data class SpeedLimitsToggled(val switchedToAlternativeLimit: Boolean) : Event()
        data object SpeedLimitsUpdated : Event()
        data object Shutdown : Event()
        data object TorrentCategoryUpdated : Event()
    }
}
