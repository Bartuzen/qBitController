package dev.bartuzen.qbitcontroller.ui.torrentlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.bartuzen.qbitcontroller.data.SettingsManager
import dev.bartuzen.qbitcontroller.data.TorrentSort
import dev.bartuzen.qbitcontroller.data.repositories.TorrentListRepository
import dev.bartuzen.qbitcontroller.model.ServerConfig
import dev.bartuzen.qbitcontroller.model.Torrent
import dev.bartuzen.qbitcontroller.network.RequestError
import dev.bartuzen.qbitcontroller.network.RequestResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
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

    val torrentSort = settingsManager.sortFlow

    private val eventChannel = Channel<Event>()
    val eventFlow = eventChannel.receiveAsFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    var isInitialLoadStarted = false

    private fun updateTorrentList(serverConfig: ServerConfig, torrentSort: TorrentSort? = null) =
        viewModelScope.launch {
            when (
                val result = repository.getTorrentList(
                    serverConfig, torrentSort ?: this@TorrentListViewModel.torrentSort.first()
                )
            ) {
                is RequestResult.Success -> {
                    _torrentList.value = result.data
                }
                is RequestResult.Error -> {
                    eventChannel.send(Event.Error(result.error))
                }
            }
        }

    fun loadTorrentList(serverConfig: ServerConfig, torrentSort: TorrentSort? = null) {
        if (!isLoading.value) {
            _isLoading.value = true
            updateTorrentList(serverConfig, torrentSort).invokeOnCompletion {
                _isLoading.value = false
            }
        }
    }

    fun refreshTorrentListCategoryTags(
        serverConfig: ServerConfig,
        torrentSort: TorrentSort? = null
    ) {
        if (!isRefreshing.value) {
            _isRefreshing.value = true
            viewModelScope.launch {
                val torrentListJob = updateTorrentList(serverConfig, torrentSort)
                val categoryTagJob = updateCategoryAndTags(serverConfig)

                torrentListJob.join()
                categoryTagJob.join()

                _isRefreshing.value = false
            }
        }
    }

    fun deleteTorrents(serverConfig: ServerConfig, hashes: List<String>, deleteFiles: Boolean) =
        viewModelScope.launch {
            when (val result = repository.deleteTorrents(serverConfig, hashes, deleteFiles)) {
                is RequestResult.Success -> {
                    eventChannel.send(Event.TorrentsDeleted(hashes.size))
                }
                is RequestResult.Error -> {
                    eventChannel.send(Event.Error(result.error))
                }
            }
        }

    fun pauseTorrents(serverConfig: ServerConfig, hashes: List<String>) =
        viewModelScope.launch {
            when (val result = repository.pauseTorrents(serverConfig, hashes)) {
                is RequestResult.Success -> {
                    eventChannel.send(Event.TorrentsPaused(hashes.size))
                }
                is RequestResult.Error -> {
                    eventChannel.send(Event.Error(result.error))
                }
            }
        }

    fun resumeTorrents(serverConfig: ServerConfig, hashes: List<String>) =
        viewModelScope.launch {
            when (val result = repository.resumeTorrents(serverConfig, hashes)) {
                is RequestResult.Success -> {
                    eventChannel.send(Event.TorrentsResumed(hashes.size))
                }
                is RequestResult.Error -> {
                    eventChannel.send(Event.Error(result.error))
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
                    eventChannel.send(Event.Error(result.error))
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
                    eventChannel.send(Event.Error(result.error))
                    throw CancellationException()
                }
            }
        }

        val categories = categoriesDeferred.await()
        val tags = tagsDeferred.await()

        _categoryList.value = categories
        _tagList.value = tags
    }

    fun deleteCategory(serverConfig: ServerConfig, category: String) = viewModelScope.launch {
        when (val result = repository.deleteCategory(serverConfig, category)) {
            is RequestResult.Success -> {
                eventChannel.send(Event.CategoryDeleted(category))
            }
            is RequestResult.Error -> {
                eventChannel.send(Event.Error(result.error))
            }
        }
    }

    fun deleteTag(serverConfig: ServerConfig, tag: String) = viewModelScope.launch {
        when (val result = repository.deleteTag(serverConfig, tag)) {
            is RequestResult.Success -> {
                eventChannel.send(Event.TagDeleted(tag))
            }
            is RequestResult.Error -> {
                eventChannel.send(Event.Error(result.error))
            }
        }
    }

    fun setTorrentSort(torrentSort: TorrentSort) = viewModelScope.launch {
        settingsManager.setTorrentSort(torrentSort)
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
        data class Error(val result: RequestError) : Event()
        data class TorrentsDeleted(val count: Int) : Event()
        data class TorrentsPaused(val count: Int) : Event()
        data class TorrentsResumed(val count: Int) : Event()
        data class CategoryDeleted(val name: String) : Event()
        data class TagDeleted(val name: String) : Event()
    }
}
