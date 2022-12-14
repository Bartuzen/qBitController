package dev.bartuzen.qbitcontroller.ui.torrentlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.bartuzen.qbitcontroller.data.SettingsManager
import dev.bartuzen.qbitcontroller.data.TorrentSort
import dev.bartuzen.qbitcontroller.data.repositories.TorrentListRepository
import dev.bartuzen.qbitcontroller.model.ServerConfig
import dev.bartuzen.qbitcontroller.model.Torrent
import dev.bartuzen.qbitcontroller.network.RequestResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
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
import javax.inject.Inject

@HiltViewModel
class TorrentListViewModel @Inject constructor(
    private val repository: TorrentListRepository,
    private val settingsManager: SettingsManager
) : ViewModel() {
    private val _torrentList = MutableStateFlow<List<Torrent>?>(null)
    val torrentList = _torrentList.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _categoryList = MutableStateFlow<List<String>?>(null)
    val categoryList = _categoryList.asStateFlow()

    private val _tagList = MutableStateFlow<List<String>?>(null)
    val tagList = _tagList.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory = _selectedCategory.asStateFlow()

    private val _selectedTag = MutableStateFlow<String?>(null)
    val selectedTag = _selectedTag.asStateFlow()

    val torrentSort = settingsManager.sort.flow
    val isReverseSorting = settingsManager.isReverseSorting.flow

    private val eventChannel = Channel<Event>()
    val eventFlow = eventChannel.receiveAsFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    val sortedTorrentList =
        combine(torrentList, torrentSort, isReverseSorting) { torrentList, torrentSort, isReverseSorting ->
            if (torrentList != null) {
                Triple(torrentList, torrentSort, isReverseSorting)
            } else {
                null
            }
        }.filterNotNull().map { (torrentList, torrentSort, isReverseSorting) ->
            torrentList
                .run {
                    when (torrentSort) {
                        TorrentSort.NAME -> sortedWith(compareBy({ it.name }, { it.hash }))
                        TorrentSort.HASH -> sortedBy { it.hash }
                        TorrentSort.DOWNLOAD_SPEED -> sortedWith(compareBy({ it.downloadSpeed }, { it.hash }))
                        TorrentSort.UPLOAD_SPEED -> sortedWith(compareBy({ it.uploadSpeed }, { it.hash }))
                        TorrentSort.PRIORITY -> sortedWith(compareBy({ it.priority }, { it.hash }))
                    }
                }
                .run {
                    if (isReverseSorting) {
                        reversed()
                    } else {
                        this
                    }
                }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    var isInitialLoadStarted = false

    private fun updateTorrentList(serverConfig: ServerConfig) = viewModelScope.launch {
        when (val result = repository.getTorrentList(serverConfig)) {
            is RequestResult.Success -> {
                _torrentList.value = result.data
            }
            is RequestResult.Error -> {
                eventChannel.send(Event.Error(result))
            }
        }
    }

    fun updateCategoryAndTags(serverConfig: ServerConfig) = viewModelScope.launch {
        val categoriesDeferred = async {
            when (val result = repository.getCategories(serverConfig)) {
                is RequestResult.Success -> {
                    result.data.values
                        .toList()
                        .map { it.name }
                        .sortedBy { it }
                }
                is RequestResult.Error -> {
                    eventChannel.send(Event.Error(result))
                    throw CancellationException()
                }
            }
        }
        val tagsDeferred = async {
            when (val result = repository.getTags(serverConfig)) {
                is RequestResult.Success -> {
                    result.data.sortedBy { it }
                }
                is RequestResult.Error -> {
                    eventChannel.send(Event.Error(result))
                    throw CancellationException()
                }
            }
        }

        val categories = categoriesDeferred.await()
        val tags = tagsDeferred.await()

        _categoryList.value = categories
        _tagList.value = tags
    }

    fun loadTorrentList(serverConfig: ServerConfig) {
        if (!isLoading.value) {
            _isLoading.value = true
            updateTorrentList(serverConfig).invokeOnCompletion {
                _isLoading.value = false
            }
        }
    }

    fun refreshTorrentListCategoryTags(serverConfig: ServerConfig) {
        if (!isRefreshing.value) {
            _isRefreshing.value = true
            viewModelScope.launch {
                val torrentListJob = updateTorrentList(serverConfig)
                val categoryTagJob = updateCategoryAndTags(serverConfig)

                torrentListJob.join()
                categoryTagJob.join()

                _isRefreshing.value = false
            }
        }
    }

    fun deleteTorrents(serverConfig: ServerConfig, hashes: List<String>, deleteFiles: Boolean) = viewModelScope.launch {
        when (val result = repository.deleteTorrents(serverConfig, hashes, deleteFiles)) {
            is RequestResult.Success -> {
                eventChannel.send(Event.TorrentsDeleted(hashes.size))
            }
            is RequestResult.Error -> {
                eventChannel.send(Event.Error(result))
            }
        }
    }

    fun pauseTorrents(serverConfig: ServerConfig, hashes: List<String>) = viewModelScope.launch {
        when (val result = repository.pauseTorrents(serverConfig, hashes)) {
            is RequestResult.Success -> {
                eventChannel.send(Event.TorrentsPaused(hashes.size))
            }
            is RequestResult.Error -> {
                eventChannel.send(Event.Error(result))
            }
        }
    }

    fun resumeTorrents(serverConfig: ServerConfig, hashes: List<String>) = viewModelScope.launch {
        when (val result = repository.resumeTorrents(serverConfig, hashes)) {
            is RequestResult.Success -> {
                eventChannel.send(Event.TorrentsResumed(hashes.size))
            }
            is RequestResult.Error -> {
                eventChannel.send(Event.Error(result))
            }
        }
    }

    fun deleteCategory(serverConfig: ServerConfig, category: String) = viewModelScope.launch {
        when (val result = repository.deleteCategory(serverConfig, category)) {
            is RequestResult.Success -> {
                eventChannel.send(Event.CategoryDeleted(category))
            }
            is RequestResult.Error -> {
                eventChannel.send(Event.Error(result))
            }
        }
    }

    fun deleteTag(serverConfig: ServerConfig, tag: String) = viewModelScope.launch {
        when (val result = repository.deleteTag(serverConfig, tag)) {
            is RequestResult.Success -> {
                eventChannel.send(Event.TagDeleted(tag))
            }
            is RequestResult.Error -> {
                eventChannel.send(Event.Error(result))
            }
        }
    }

    fun increaseTorrentPriority(serverConfig: ServerConfig, hashes: List<String>) = viewModelScope.launch {
        when (val result = repository.increaseTorrentPriority(serverConfig, hashes)) {
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

    fun decreaseTorrentPriority(serverConfig: ServerConfig, hashes: List<String>) = viewModelScope.launch {
        when (val result = repository.decreaseTorrentPriority(serverConfig, hashes)) {
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

    fun maximizeTorrentPriority(serverConfig: ServerConfig, hashes: List<String>) = viewModelScope.launch {
        when (val result = repository.maximizeTorrentPriority(serverConfig, hashes)) {
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

    fun minimizeTorrentPriority(serverConfig: ServerConfig, hashes: List<String>) = viewModelScope.launch {
        when (val result = repository.minimizeTorrentPriority(serverConfig, hashes)) {
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

    fun createCategory(serverConfig: ServerConfig, name: String, savePath: String) = viewModelScope.launch {
        when (val result = repository.createCategory(serverConfig, name, savePath)) {
            is RequestResult.Success -> {
                eventChannel.send(Event.CategoryCreated)
            }
            is RequestResult.Error -> {
                eventChannel.send(Event.Error(result))
            }
        }
    }

    fun createTags(serverConfig: ServerConfig, names: List<String>) = viewModelScope.launch {
        when (val result = repository.createTags(serverConfig, names)) {
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
        _searchQuery.value = query
    }

    fun setSelectedCategory(name: String?) {
        _selectedCategory.value = name
    }

    fun setSelectedTag(name: String?) {
        _selectedTag.value = name
    }

    sealed class Event {
        data class Error(val error: RequestResult.Error) : Event()
        object QueueingNotEnabled : Event()
        data class TorrentsDeleted(val count: Int) : Event()
        data class TorrentsPaused(val count: Int) : Event()
        data class TorrentsResumed(val count: Int) : Event()
        data class CategoryDeleted(val name: String) : Event()
        data class TagDeleted(val name: String) : Event()
        object TorrentsPriorityIncreased : Event()
        object TorrentsPriorityDecreased : Event()
        object TorrentsPriorityMaximized : Event()
        object TorrentsPriorityMinimized : Event()
        object CategoryCreated : Event()
        object TagCreated : Event()
    }
}
