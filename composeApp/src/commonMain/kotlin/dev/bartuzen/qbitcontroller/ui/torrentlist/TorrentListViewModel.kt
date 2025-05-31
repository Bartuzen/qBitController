package dev.bartuzen.qbitcontroller.ui.torrentlist

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.bartuzen.qbitcontroller.data.ServerManager
import dev.bartuzen.qbitcontroller.data.SettingsManager
import dev.bartuzen.qbitcontroller.data.TorrentSort
import dev.bartuzen.qbitcontroller.data.notification.TorrentDownloadedNotifier
import dev.bartuzen.qbitcontroller.data.repositories.TorrentListRepository
import dev.bartuzen.qbitcontroller.model.MainData
import dev.bartuzen.qbitcontroller.model.ServerConfig
import dev.bartuzen.qbitcontroller.model.Torrent
import dev.bartuzen.qbitcontroller.model.TorrentState
import dev.bartuzen.qbitcontroller.network.RequestResult
import dev.bartuzen.qbitcontroller.utils.getSerializableStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.time.Duration.Companion.seconds

class TorrentListViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val serverManager: ServerManager,
    private val repository: TorrentListRepository,
    private val settingsManager: SettingsManager,
    private val notifier: TorrentDownloadedNotifier,
) : ViewModel() {
    private var serverScope = CoroutineScope(viewModelScope.coroutineContext + SupervisorJob())

    private val currentServer =
        savedStateHandle.getSerializableStateFlow<ServerConfig?>(viewModelScope, "currentServer", null)

    val serversFlow = serverManager.serversFlow

    private val isScreenActive = MutableStateFlow(false)

    val torrentSort = settingsManager.sort.flow
    val isReverseSorting = settingsManager.isReverseSorting.flow
    val areTorrentSwipeActionsEnabled = settingsManager.areTorrentSwipeActionsEnabled.flow
    private val autoRefreshInterval = settingsManager.autoRefreshInterval.flow

    val areStatesCollapsed = settingsManager.areStatesCollapsed.flow
    val areCategoriesCollapsed = settingsManager.areCategoriesCollapsed.flow
    val areTagsCollapsed = settingsManager.areTagsCollapsed.flow
    val areTrackersCollapsed = settingsManager.areTrackersCollapsed.flow

    private val eventChannel = Channel<Event>()
    val eventFlow = eventChannel.receiveAsFlow()

    private val _isNaturalLoading = MutableStateFlow<Boolean?>(null)
    val isNaturalLoading = _isNaturalLoading.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    private val _mainData = MutableStateFlow<MainData?>(null)
    val mainData = _mainData.asStateFlow()

    val searchQuery = savedStateHandle.getStateFlow("searchQuery", "")
    val selectedCategory =
        savedStateHandle.getSerializableStateFlow<CategoryTag>(viewModelScope, "selectedCategory", CategoryTag.All)
    val selectedTag = savedStateHandle.getSerializableStateFlow<CategoryTag>(viewModelScope, "selectedTag", CategoryTag.All)
    val selectedFilter =
        savedStateHandle.getSerializableStateFlow<TorrentFilter>(viewModelScope, "selectedFilter", TorrentFilter.ALL)
    val selectedTracker = savedStateHandle.getSerializableStateFlow<Tracker>(viewModelScope, "selectedTracker", Tracker.All)

    fun setCurrentServer(serverConfig: ServerConfig?) {
        savedStateHandle["currentServer"] = Json.encodeToString(serverConfig)
    }

    private fun startAutoRefresh() {
        serverScope.cancel()
        serverScope = CoroutineScope(viewModelScope.coroutineContext + SupervisorJob())

        loadMainData()
        serverScope.launch {
            combine(
                autoRefreshInterval,
                isNaturalLoading,
                isScreenActive,
            ) { autoRefreshInterval, isNaturalLoading, isScreenActive ->
                Triple(autoRefreshInterval, isNaturalLoading, isScreenActive)
            }.collectLatest { (autoRefreshInterval, isNaturalLoading, isScreenActive) ->
                if (isScreenActive && isNaturalLoading == null && autoRefreshInterval != 0) {
                    delay(autoRefreshInterval.seconds)
                    loadMainData(autoRefresh = true)
                }
            }
        }
    }

    init {
        viewModelScope.launch {
            currentServer.collectLatest { serverConfig ->
                _mainData.value = null
                resetFilters(setFilterToDefault = true)

                viewModelScope.launch {
                    eventChannel.send(Event.ServerChanged)
                }

                if (serverConfig != null) {
                    startAutoRefresh()
                }
            }
        }

        viewModelScope.launch {
            mainData.collect { mainData ->
                selectedCategory.value.let { selectedCategory ->
                    if (selectedCategory is CategoryTag.Item &&
                        mainData?.categories?.map { it.name }?.contains(selectedCategory.name) != true
                    ) {
                        setSelectedCategory(CategoryTag.All)
                    }
                }

                selectedTag.value.let { selectedTag ->
                    if (selectedTag is CategoryTag.Item && mainData?.tags?.contains(selectedTag.name) != true) {
                        setSelectedTag(CategoryTag.All)
                    }
                }

                selectedTracker.value.let { selectedTracker ->
                    if (selectedTracker is Tracker.Named && mainData?.trackers?.contains(selectedTracker.name) != true) {
                        setSelectedTracker(Tracker.All)
                    }
                }
            }
        }
    }

    override fun onCleared() {
        serverScope.cancel()
    }

    fun setScreenActive(isScreenActive: Boolean) {
        this.isScreenActive.value = isScreenActive
    }

    private val sortedTorrentList =
        combine(mainData, torrentSort, isReverseSorting) { mainData, torrentSort, isReverseSorting ->
            Triple(mainData?.torrents, torrentSort, isReverseSorting)
        }.map { (torrentList, torrentSort, isReverseSorting) ->
            if (torrentList == null) {
                return@map null
            }

            withContext(Dispatchers.Default) {
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
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val filteredTorrentList = combine(
        sortedTorrentList,
        searchQuery,
        selectedCategory,
        selectedTag,
        selectedFilter,
        selectedTracker,
        mainData,
    ) { filters -> filters }.map { filters ->
        if (filters[0] == null || filters[6] == null) {
            return@map null
        }

        @Suppress("UNCHECKED_CAST")
        val torrentList = filters[0] as List<Torrent>
        val searchQuery = filters[1] as String
        val selectedCategory = filters[2] as CategoryTag
        val selectedTag = filters[3] as CategoryTag
        val selectedFilter = filters[4] as TorrentFilter
        val selectedTracker = filters[5] as Tracker
        val mainData = filters[6] as MainData

        withContext(Dispatchers.Default) {
            torrentList.filter { torrent ->
                if (searchQuery.isNotEmpty()) {
                    val matchesSearchQuery = searchQuery
                        .split(" ")
                        .filter { it.isNotEmpty() && it != "-" }
                        .all { term ->
                            val isExclusion = term.startsWith("-")
                            val cleanTerm = term.removePrefix("-")
                            val containsTerm = torrent.name.contains(cleanTerm, ignoreCase = true)

                            if (isExclusion) !containsTerm else containsTerm
                        }
                    if (!matchesSearchQuery) {
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
                        if (torrent.hash !in mainData.trackers.getOrElse(selectedTracker.name) { emptyList() }) {
                            return@filter false
                        }
                    }
                }

                true
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val counts = mainData.map { mainData ->
        if (mainData == null) {
            return@map null
        }

        val stateCountMap = mutableMapOf<TorrentFilter, Int>()
        val categoryMap = mainData.categories.associateBy({ it.name }, { 0 }).toMutableMap()
        val tagMap = mainData.tags.associateBy({ it }, { 0 }).toMutableMap()

        var uncategorizedCount = 0
        var untaggedCount = 0

        mainData.torrents.forEach { torrent ->
            TorrentFilter.entries.forEach { filter ->
                when (filter) {
                    TorrentFilter.ACTIVE -> {
                        if (torrent.downloadSpeed != 0L || torrent.uploadSpeed != 0L) {
                            stateCountMap[filter] = (stateCountMap[filter] ?: 0) + 1
                        }
                    }
                    TorrentFilter.INACTIVE -> {
                        if (torrent.downloadSpeed == 0L && torrent.uploadSpeed == 0L) {
                            stateCountMap[filter] = (stateCountMap[filter] ?: 0) + 1
                        }
                    }
                    else -> {
                        if (filter.states == null || torrent.state in filter.states) {
                            stateCountMap[filter] = (stateCountMap[filter] ?: 0) + 1
                        }
                    }
                }
            }

            if (torrent.category != null) {
                categoryMap[torrent.category] = (categoryMap[torrent.category] ?: 0) + 1
            } else {
                uncategorizedCount++
            }

            if (torrent.tags.isNotEmpty()) {
                torrent.tags.forEach { tag ->
                    tagMap[tag] = (tagMap[tag] ?: 0) + 1
                }
            } else {
                untaggedCount++
            }
        }

        if (mainData.serverState.areSubcategoriesEnabled) {
            val categories = categoryMap.keys.toList()
            categories.forEachIndexed { index, category ->
                for (i in index + 1 until categories.size) {
                    if (categories[i].startsWith("$category/")) {
                        categoryMap[category] = (categoryMap[category] ?: 0) + (categoryMap[categories[i]] ?: 0)
                    } else {
                        break
                    }
                }
            }
        }

        Counts(
            stateCountMap = stateCountMap,
            categoryMap = categoryMap.toList(),
            tagMap = tagMap.toList(),
            allCount = mainData.torrents.size,
            uncategorizedCount = uncategorizedCount,
            untaggedCount = untaggedCount,
            trackerlessCount = mainData.torrents.count { it.trackerCount == 0 },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    data class Counts(
        val stateCountMap: Map<TorrentFilter, Int>,
        val categoryMap: List<Pair<String, Int>>,
        val tagMap: List<Pair<String, Int>>,
        val allCount: Int,
        val uncategorizedCount: Int,
        val untaggedCount: Int,
        val trackerlessCount: Int,
    )

    private fun updateMainData() = serverScope.launch {
        val serverId = currentServer.value?.id ?: return@launch
        when (val result = repository.getMainData(serverId)) {
            is RequestResult.Success -> {
                if (isActive && currentServer.value?.id == serverId) {
                    _mainData.value = result.data
                    eventChannel.send(Event.UpdateMainDataSuccess)
                }
                notifier.checkCompleted(serverId, result.data.torrents)
            }
            is RequestResult.Error -> {
                eventChannel.send(Event.Error(result))
            }
        }
    }

    fun loadMainData(autoRefresh: Boolean = false) {
        if (isNaturalLoading.value == null) {
            _isNaturalLoading.value = !autoRefresh
            updateMainData().invokeOnCompletion {
                _isNaturalLoading.value = null
            }
        }
    }

    fun refreshMainData() {
        if (!isRefreshing.value) {
            _isRefreshing.value = true
            updateMainData().invokeOnCompletion {
                viewModelScope.launch {
                    delay(25)
                    _isRefreshing.value = false
                }
            }
        }
    }

    fun deleteTorrents(hashes: List<String>, deleteFiles: Boolean) = serverScope.launch {
        val serverId = currentServer.value?.id ?: return@launch
        when (val result = repository.deleteTorrents(serverId, hashes, deleteFiles)) {
            is RequestResult.Success -> {
                eventChannel.send(Event.TorrentsDeleted(hashes.size))
                loadMainData()
            }
            is RequestResult.Error -> {
                eventChannel.send(Event.Error(result))
            }
        }
    }

    fun pauseTorrents(hashes: List<String>) = serverScope.launch {
        val serverId = currentServer.value?.id ?: return@launch
        when (val result = repository.pauseTorrents(serverId, hashes)) {
            is RequestResult.Success -> {
                eventChannel.send(Event.TorrentsPaused(hashes.size))
                launch {
                    delay(1000)
                    loadMainData()
                }
            }
            is RequestResult.Error -> {
                eventChannel.send(Event.Error(result))
            }
        }
    }

    fun resumeTorrents(hashes: List<String>) = serverScope.launch {
        val serverId = currentServer.value?.id ?: return@launch
        when (val result = repository.resumeTorrents(serverId, hashes)) {
            is RequestResult.Success -> {
                eventChannel.send(Event.TorrentsResumed(hashes.size))
                launch {
                    delay(1000)
                    loadMainData()
                }
            }
            is RequestResult.Error -> {
                eventChannel.send(Event.Error(result))
            }
        }
    }

    fun deleteCategory(category: String) = serverScope.launch {
        val serverId = currentServer.value?.id ?: return@launch
        when (val result = repository.deleteCategory(serverId, category)) {
            is RequestResult.Success -> {
                eventChannel.send(Event.CategoryDeleted(category))
                loadMainData()
            }
            is RequestResult.Error -> {
                eventChannel.send(Event.Error(result))
            }
        }
    }

    fun deleteTag(tag: String) = serverScope.launch {
        val serverId = currentServer.value?.id ?: return@launch
        when (val result = repository.deleteTag(serverId, tag)) {
            is RequestResult.Success -> {
                eventChannel.send(Event.TagDeleted(tag))
                loadMainData()
            }
            is RequestResult.Error -> {
                eventChannel.send(Event.Error(result))
            }
        }
    }

    fun increaseTorrentPriority(hashes: List<String>) = serverScope.launch {
        val serverId = currentServer.value?.id ?: return@launch
        when (val result = repository.increaseTorrentPriority(serverId, hashes)) {
            is RequestResult.Success -> {
                eventChannel.send(Event.TorrentsPriorityIncreased)
                launch {
                    delay(1000)
                    loadMainData()
                }
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

    fun decreaseTorrentPriority(hashes: List<String>) = serverScope.launch {
        val serverId = currentServer.value?.id ?: return@launch
        when (val result = repository.decreaseTorrentPriority(serverId, hashes)) {
            is RequestResult.Success -> {
                eventChannel.send(Event.TorrentsPriorityDecreased)
                launch {
                    delay(1000)
                    loadMainData()
                }
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

    fun maximizeTorrentPriority(hashes: List<String>) = serverScope.launch {
        val serverId = currentServer.value?.id ?: return@launch
        when (val result = repository.maximizeTorrentPriority(serverId, hashes)) {
            is RequestResult.Success -> {
                eventChannel.send(Event.TorrentsPriorityMaximized)
                launch {
                    delay(1000)
                    loadMainData()
                }
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

    fun minimizeTorrentPriority(hashes: List<String>) = serverScope.launch {
        val serverId = currentServer.value?.id ?: return@launch
        when (val result = repository.minimizeTorrentPriority(serverId, hashes)) {
            is RequestResult.Success -> {
                eventChannel.send(Event.TorrentsPriorityMinimized)
                launch {
                    delay(1000)
                    loadMainData()
                }
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

    fun setLocation(hashes: List<String>, location: String) = serverScope.launch {
        val serverId = currentServer.value?.id ?: return@launch
        when (val result = repository.setLocation(serverId, hashes, location)) {
            is RequestResult.Success -> {
                eventChannel.send(Event.LocationUpdated)
                loadMainData()
            }
            is RequestResult.Error -> {
                eventChannel.send(Event.Error(result))
            }
        }
    }

    fun createCategory(name: String, savePath: String, downloadPathEnabled: Boolean?, downloadPath: String) =
        serverScope.launch {
            val serverId = currentServer.value?.id ?: return@launch
            when (val result = repository.createCategory(serverId, name, savePath, downloadPathEnabled, downloadPath)) {
                is RequestResult.Success -> {
                    eventChannel.send(Event.CategoryCreated)
                    loadMainData()
                }
                is RequestResult.Error -> {
                    eventChannel.send(Event.Error(result))
                }
            }
        }

    fun editCategory(name: String, savePath: String, downloadPathEnabled: Boolean?, downloadPath: String) =
        serverScope.launch {
            val serverId = currentServer.value?.id ?: return@launch
            when (val result = repository.editCategory(serverId, name, savePath, downloadPathEnabled, downloadPath)) {
                is RequestResult.Success -> {
                    eventChannel.send(Event.CategoryEdited)
                    loadMainData()
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

    fun createTags(names: List<String>) = serverScope.launch {
        val serverId = currentServer.value?.id ?: return@launch
        when (val result = repository.createTags(serverId, names)) {
            is RequestResult.Success -> {
                eventChannel.send(Event.TagCreated)
                loadMainData()
            }
            is RequestResult.Error -> {
                eventChannel.send(Event.Error(result))
            }
        }
    }

    fun toggleSpeedLimitsMode(isCurrentLimitAlternative: Boolean) = serverScope.launch {
        val serverId = currentServer.value?.id ?: return@launch
        when (val result = repository.toggleSpeedLimitsMode(serverId)) {
            is RequestResult.Success -> {
                eventChannel.send(Event.SpeedLimitsToggled(!isCurrentLimitAlternative))
                loadMainData()
            }
            is RequestResult.Error -> {
                eventChannel.send(Event.Error(result))
            }
        }
    }

    fun shutdown() = serverScope.launch {
        val serverId = currentServer.value?.id ?: return@launch
        when (val result = repository.shutdown(serverId)) {
            is RequestResult.Success -> {
                eventChannel.send(Event.Shutdown)
            }
            is RequestResult.Error -> {
                eventChannel.send(Event.Error(result))
            }
        }
    }

    fun setCategory(hashes: List<String>, category: String?) = serverScope.launch {
        val serverId = currentServer.value?.id ?: return@launch
        when (val result = repository.setCategory(serverId, hashes, category)) {
            is RequestResult.Success -> {
                eventChannel.send(Event.TorrentCategoryUpdated)
                loadMainData()
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
        savedStateHandle["searchQuery"] = query
    }

    fun setSelectedCategory(category: CategoryTag) {
        savedStateHandle["selectedCategory"] = Json.encodeToString(category)
    }

    fun setSelectedTag(tag: CategoryTag) {
        savedStateHandle["selectedTag"] = Json.encodeToString(tag)
    }

    fun setSelectedFilter(filter: TorrentFilter) {
        savedStateHandle["selectedFilter"] = Json.encodeToString(filter)
    }

    fun setSelectedTracker(tracker: Tracker) {
        savedStateHandle["selectedTracker"] = Json.encodeToString(tracker)
    }

    fun setDefaultTorrentStatus(status: TorrentFilter) {
        settingsManager.defaultTorrentStatus.value = status
    }

    fun setFiltersCollapseState(isCollapsed: Boolean) {
        settingsManager.areStatesCollapsed.value = isCollapsed
    }

    fun setCategoriesCollapseState(isCollapsed: Boolean) {
        settingsManager.areCategoriesCollapsed.value = isCollapsed
    }

    fun setTagsCollapseState(isCollapsed: Boolean) {
        settingsManager.areTagsCollapsed.value = isCollapsed
    }

    fun setTrackersCollapseState(isCollapsed: Boolean) {
        settingsManager.areTrackersCollapsed.value = isCollapsed
    }

    fun resetFilters(setFilterToDefault: Boolean = false) {
        setSearchQuery("")
        setSelectedCategory(CategoryTag.All)
        setSelectedTag(CategoryTag.All)
        setSelectedFilter(if (setFilterToDefault) TorrentFilter.ALL else settingsManager.defaultTorrentStatus.value)
        setSelectedTracker(Tracker.All)
    }

    fun setSpeedLimits(download: Int?, upload: Int?) {
        val serverId = currentServer.value?.id ?: return
        val job = Job()

        serverScope.launch(job) {
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
            loadMainData()
        }
    }

    sealed class Event {
        data class Error(val error: RequestResult.Error) : Event()
        data object UpdateMainDataSuccess : Event()
        data object ServerChanged : Event()
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

@Serializable
enum class TorrentFilter(val states: List<TorrentState>?) {
    ALL(null),
    DOWNLOADING(
        listOf(
            TorrentState.DOWNLOADING,
            TorrentState.CHECKING_DL,
            TorrentState.STALLED_DL,
            TorrentState.FORCED_DL,
            TorrentState.QUEUED_DL,
            TorrentState.META_DL,
            TorrentState.FORCED_META_DL,
            TorrentState.PAUSED_DL,
        ),
    ),
    SEEDING(
        listOf(
            TorrentState.UPLOADING,
            TorrentState.CHECKING_UP,
            TorrentState.STALLED_UP,
            TorrentState.FORCED_UP,
            TorrentState.QUEUED_UP,
        ),
    ),
    COMPLETED(
        listOf(
            TorrentState.UPLOADING,
            TorrentState.CHECKING_UP,
            TorrentState.STALLED_UP,
            TorrentState.FORCED_UP,
            TorrentState.QUEUED_UP,
            TorrentState.PAUSED_UP,
        ),
    ),
    RESUMED(
        listOf(
            TorrentState.DOWNLOADING,
            TorrentState.CHECKING_DL,
            TorrentState.STALLED_DL,
            TorrentState.QUEUED_DL,
            TorrentState.META_DL,
            TorrentState.FORCED_META_DL,
            TorrentState.FORCED_DL,
            TorrentState.UPLOADING,
            TorrentState.CHECKING_UP,
            TorrentState.STALLED_UP,
            TorrentState.QUEUED_UP,
            TorrentState.FORCED_UP,
        ),
    ),
    PAUSED(listOf(TorrentState.PAUSED_DL, TorrentState.PAUSED_UP)),
    ACTIVE(null),
    INACTIVE(null),
    STALLED(listOf(TorrentState.STALLED_DL, TorrentState.STALLED_UP)),
    CHECKING(listOf(TorrentState.CHECKING_DL, TorrentState.CHECKING_UP, TorrentState.CHECKING_RESUME_DATA)),
    MOVING(listOf(TorrentState.MOVING)),
    ERROR(listOf(TorrentState.ERROR, TorrentState.MISSING_FILES)),
}

@Serializable
sealed interface CategoryTag {
    @Serializable
    data object All : CategoryTag

    @Serializable
    data object Uncategorized : CategoryTag

    @Serializable
    data class Item(val name: String) : CategoryTag
}

@Serializable
sealed interface Tracker {
    @Serializable
    data object All : Tracker

    @Serializable
    data object Trackerless : Tracker

    @Serializable
    data class Named(val name: String) : Tracker
}
