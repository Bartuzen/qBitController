package dev.bartuzen.qbitcontroller.ui.torrentlist

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
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
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

@HiltViewModel
class TorrentListViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val serverManager: ServerManager,
    private val repository: TorrentListRepository,
    private val settingsManager: SettingsManager,
    private val notifier: TorrentDownloadedNotifier,
) : ViewModel() {
    private var serverScope = CoroutineScope(viewModelScope.coroutineContext + SupervisorJob())

    val currentServerId = savedStateHandle.getStateFlow<Int?>("current_server_id", null)

    private val _currentServer = MutableStateFlow(null as ServerConfig?)
    val currentServer = _currentServer.asStateFlow()

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

    private val _searchQuery = MutableStateFlow("")
    private val _selectedCategory = MutableStateFlow<CategoryTag>(CategoryTag.All)
    private val _selectedTag = MutableStateFlow<CategoryTag>(CategoryTag.All)
    private val _selectedFilter = MutableStateFlow(TorrentFilter.ALL)
    private val _selectedTracker = MutableStateFlow<Tracker>(Tracker.All)

    val searchQuery = _searchQuery.asStateFlow()
    val selectedFilter = _selectedFilter.asStateFlow()
    val selectedCategory = _selectedCategory.asStateFlow()
    val selectedTag = _selectedTag.asStateFlow()
    val selectedTracker = _selectedTracker.asStateFlow()

    private val serverListener = object : ServerManager.ServerListener {
        override fun onServerAddedListener(serverConfig: ServerConfig) {
            if (serversFlow.value.size == 1) {
                setCurrentServer(serverConfig.id)
            }
        }

        override fun onServerRemovedListener(serverConfig: ServerConfig) {
            if (currentServerId.value == serverConfig.id) {
                setCurrentServer(getFirstServer()?.id)
            }
        }

        override fun onServerChangedListener(serverConfig: ServerConfig) {
            if (currentServerId.value == serverConfig.id) {
                setCurrentServer(serverConfig.id, forceReset = true)
            }
        }
    }

    private fun getFirstServer(): ServerConfig? = serversFlow.value.let { serverList ->
        serverList.firstNotNullOfOrNull { it.value }
    }

    fun setCurrentServer(id: Int?, forceReset: Boolean = false) {
        val oldServerId = currentServerId.value
        if (oldServerId != id || forceReset) {
            serverScope.cancel()
            serverScope = CoroutineScope(viewModelScope.coroutineContext + SupervisorJob())

            savedStateHandle["current_server_id"] = id
            _currentServer.value = if (id != null) serverManager.getServerOrNull(id) else null

            _mainData.value = null
            _searchQuery.value = ""
            _selectedCategory.value = CategoryTag.All
            _selectedTag.value = CategoryTag.All
            _selectedFilter.value = TorrentFilter.ALL
            _selectedTracker.value = Tracker.All

            viewModelScope.launch {
                eventChannel.send(Event.SeverChanged)

                if (id != null) {
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
            }
        }
    }

    init {
        setCurrentServer(getFirstServer()?.id, forceReset = true)
        serverManager.addServerListener(serverListener)

        viewModelScope.launch {
            mainData.filterNotNull().collect { mainData ->
                selectedCategory.value.let { selectedCategory ->
                    if (selectedCategory is CategoryTag.Item &&
                        selectedCategory.name !in mainData.categories.map { it.name }
                    ) {
                        _selectedCategory.value = CategoryTag.All
                    }
                }

                selectedTag.value.let { selectedTag ->
                    if (selectedTag is CategoryTag.Item && selectedTag.name !in mainData.tags) {
                        _selectedTag.value = CategoryTag.All
                    }
                }

                selectedTracker.value.let { selectedTracker ->
                    if (selectedTracker is Tracker.Named && selectedTracker.name !in mainData.trackers) {
                        _selectedTracker.value = Tracker.All
                    }
                }
            }
        }
    }

    override fun onCleared() {
        serverManager.removeServerListener(serverListener)
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
                        if (torrent.hash !in mainData.trackers.getOrDefault(selectedTracker.name, emptyList())) {
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

    private fun updateMainData(serverId: Int) = serverScope.launch {
        when (val result = repository.getMainData(serverId)) {
            is RequestResult.Success -> {
                if (isActive && currentServerId.value == serverId) {
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
        val serverId = currentServerId.value ?: return

        if (isNaturalLoading.value == null) {
            _isNaturalLoading.value = !autoRefresh
            updateMainData(serverId).invokeOnCompletion {
                _isNaturalLoading.value = null
            }
        }
    }

    fun refreshMainData() {
        val serverId = currentServerId.value ?: return

        if (!isRefreshing.value) {
            _isRefreshing.value = true
            updateMainData(serverId).invokeOnCompletion {
                viewModelScope.launch {
                    delay(25)
                    _isRefreshing.value = false
                }
            }
        }
    }

    fun deleteTorrents(serverId: Int, hashes: List<String>, deleteFiles: Boolean) = serverScope.launch {
        when (val result = repository.deleteTorrents(serverId, hashes, deleteFiles)) {
            is RequestResult.Success -> {
                eventChannel.send(Event.TorrentsDeleted(hashes.size))
            }
            is RequestResult.Error -> {
                eventChannel.send(Event.Error(result))
            }
        }
    }

    fun pauseTorrents(serverId: Int, hashes: List<String>) = serverScope.launch {
        when (val result = repository.pauseTorrents(serverId, hashes)) {
            is RequestResult.Success -> {
                eventChannel.send(Event.TorrentsPaused(hashes.size))
            }
            is RequestResult.Error -> {
                eventChannel.send(Event.Error(result))
            }
        }
    }

    fun resumeTorrents(serverId: Int, hashes: List<String>) = serverScope.launch {
        when (val result = repository.resumeTorrents(serverId, hashes)) {
            is RequestResult.Success -> {
                eventChannel.send(Event.TorrentsResumed(hashes.size))
            }
            is RequestResult.Error -> {
                eventChannel.send(Event.Error(result))
            }
        }
    }

    fun deleteCategory(serverId: Int, category: String) = serverScope.launch {
        when (val result = repository.deleteCategory(serverId, category)) {
            is RequestResult.Success -> {
                eventChannel.send(Event.CategoryDeleted(category))
            }
            is RequestResult.Error -> {
                eventChannel.send(Event.Error(result))
            }
        }
    }

    fun deleteTag(serverId: Int, tag: String) = serverScope.launch {
        when (val result = repository.deleteTag(serverId, tag)) {
            is RequestResult.Success -> {
                eventChannel.send(Event.TagDeleted(tag))
            }
            is RequestResult.Error -> {
                eventChannel.send(Event.Error(result))
            }
        }
    }

    fun increaseTorrentPriority(serverId: Int, hashes: List<String>) = serverScope.launch {
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

    fun decreaseTorrentPriority(serverId: Int, hashes: List<String>) = serverScope.launch {
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

    fun maximizeTorrentPriority(serverId: Int, hashes: List<String>) = serverScope.launch {
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

    fun minimizeTorrentPriority(serverId: Int, hashes: List<String>) = serverScope.launch {
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

    fun setLocation(serverId: Int, hashes: List<String>, location: String) = serverScope.launch {
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
        serverScope.launch {
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
        serverScope.launch {
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

    fun createTags(serverId: Int, names: List<String>) = serverScope.launch {
        when (val result = repository.createTags(serverId, names)) {
            is RequestResult.Success -> {
                eventChannel.send(Event.TagCreated)
            }
            is RequestResult.Error -> {
                eventChannel.send(Event.Error(result))
            }
        }
    }

    fun toggleSpeedLimitsMode(serverId: Int, isCurrentLimitAlternative: Boolean) = serverScope.launch {
        when (val result = repository.toggleSpeedLimitsMode(serverId)) {
            is RequestResult.Success -> {
                eventChannel.send(Event.SpeedLimitsToggled(!isCurrentLimitAlternative))
            }
            is RequestResult.Error -> {
                eventChannel.send(Event.Error(result))
            }
        }
    }

    fun shutdown(serverId: Int) = serverScope.launch {
        when (val result = repository.shutdown(serverId)) {
            is RequestResult.Success -> {
                eventChannel.send(Event.Shutdown)
            }
            is RequestResult.Error -> {
                eventChannel.send(Event.Error(result))
            }
        }
    }

    fun setCategory(serverId: Int, hashes: List<String>, category: String?) = serverScope.launch {
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
        _searchQuery.value = query
    }

    fun setSelectedCategory(category: CategoryTag) {
        _selectedCategory.value = category
    }

    fun setSelectedTag(tag: CategoryTag) {
        _selectedTag.value = tag
    }

    fun setSelectedFilter(filter: TorrentFilter) {
        _selectedFilter.value = filter
    }

    fun setSelectedTracker(tracker: Tracker) {
        _selectedTracker.value = tracker
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

    fun setSpeedLimits(serverId: Int, download: Int?, upload: Int?) {
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
        }
    }

    sealed class Event {
        data class Error(val error: RequestResult.Error) : Event()
        data object UpdateMainDataSuccess : Event()
        data object SeverChanged : Event()
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

sealed interface CategoryTag {
    data object All : CategoryTag
    data object Uncategorized : CategoryTag
    data class Item(val name: String) : CategoryTag
}

sealed interface Tracker {
    data object All : Tracker
    data object Trackerless : Tracker
    data class Named(val name: String) : Tracker
}
