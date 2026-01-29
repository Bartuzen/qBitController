package dev.bartuzen.qbitcontroller.ui.rss.feeds

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.bartuzen.qbitcontroller.data.repositories.rss.RssFeedRepository
import dev.bartuzen.qbitcontroller.model.RssFeedNode
import dev.bartuzen.qbitcontroller.network.RequestResult
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class RssFeedsViewModel(
    private val serverId: Int,
    private val repository: RssFeedRepository,
) : ViewModel() {
    private val _rssFeeds = MutableStateFlow<RssFeedNode?>(null)
    val rssFeeds = _rssFeeds.asStateFlow()

    private val eventChannel = Channel<Event>()
    val eventFlow = eventChannel.receiveAsFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    init {
        loadRssFeeds()
    }

    private fun updateRssFeeds() = viewModelScope.launch {
        when (val result = repository.getRssFeeds(serverId)) {
            is RequestResult.Success -> {
                _rssFeeds.value = result.data
            }
            is RequestResult.Error -> {
                eventChannel.send(Event.Error(result))
            }
        }
    }

    fun loadRssFeeds() {
        if (!isLoading.value) {
            _isLoading.value = true
            updateRssFeeds().invokeOnCompletion {
                _isLoading.value = false
            }
        }
    }

    fun refreshRssFeeds() {
        if (!isRefreshing.value) {
            _isRefreshing.value = true
            updateRssFeeds().invokeOnCompletion {
                _isRefreshing.value = false
            }
        }
    }

    fun refreshAllFeeds() = viewModelScope.launch {
        when (val result = repository.refreshAllFeeds(serverId)) {
            is RequestResult.Success -> {
                eventChannel.send(Event.AllFeedsRefreshed)
                viewModelScope.launch {
                    delay(1000)
                    loadRssFeeds()
                }
            }
            is RequestResult.Error -> {
                eventChannel.send(Event.Error(result))
            }
        }
    }

    fun addRssFeed(url: String, path: String) = viewModelScope.launch {
        when (val result = repository.addRssFeed(serverId, url, path)) {
            is RequestResult.Success -> {
                eventChannel.send(Event.FeedAdded)
                loadRssFeeds()
            }
            is RequestResult.Error.ApiError if result.code == 409 -> {
                eventChannel.send(Event.FeedAddError)
            }
            is RequestResult.Error -> {
                eventChannel.send(Event.Error(result))
            }
        }
    }

    fun addRssFolder(path: String) = viewModelScope.launch {
        when (val result = repository.addRssFolder(serverId, path)) {
            is RequestResult.Success -> {
                eventChannel.send(Event.FolderAdded)
                loadRssFeeds()
            }
            is RequestResult.Error.ApiError if result.code == 409 -> {
                eventChannel.send(Event.FolderAddError)
            }
            is RequestResult.Error -> {
                eventChannel.send(Event.Error(result))
            }
        }
    }

    fun renameItem(from: String, to: String, isFeed: Boolean) = viewModelScope.launch {
        when (val result = repository.moveItem(serverId, from, to)) {
            is RequestResult.Success -> {
                if (isFeed) {
                    eventChannel.send(Event.FeedRenamed)
                } else {
                    eventChannel.send(Event.FolderRenamed)
                }
                loadRssFeeds()
            }
            is RequestResult.Error.ApiError if result.code == 409 -> {
                if (isFeed) {
                    eventChannel.send(Event.FeedRenameError)
                } else {
                    eventChannel.send(Event.FolderRenameError)
                }
            }
            is RequestResult.Error -> {
                eventChannel.send(Event.Error(result))
            }
        }
    }

    fun moveItem(from: String, to: String, isFeed: Boolean) = viewModelScope.launch {
        when (val result = repository.moveItem(serverId, from, to)) {
            is RequestResult.Success -> {
                if (isFeed) {
                    eventChannel.send(Event.FeedMoved)
                } else {
                    eventChannel.send(Event.FolderMoved)
                }
                loadRssFeeds()
            }
            is RequestResult.Error.ApiError if result.code == 409 -> {
                if (isFeed) {
                    eventChannel.send(Event.FeedMoveError)
                } else {
                    eventChannel.send(Event.FolderMoveError)
                }
            }
            is RequestResult.Error -> {
                eventChannel.send(Event.Error(result))
            }
        }
    }

    fun deleteItem(path: String, isFeed: Boolean) = viewModelScope.launch {
        when (val result = repository.removeItem(serverId, path)) {
            is RequestResult.Success -> {
                if (isFeed) {
                    eventChannel.send(Event.FeedDeleted)
                } else {
                    eventChannel.send(Event.FolderDeleted)
                }
                loadRssFeeds()
            }
            is RequestResult.Error.ApiError if result.code == 409 -> {
                if (isFeed) {
                    eventChannel.send(Event.FeedDeleteError)
                } else {
                    eventChannel.send(Event.FolderDeleteError)
                }
            }
            is RequestResult.Error -> {
                eventChannel.send(Event.Error(result))
            }
        }
    }

    fun setFeedUrl(path: String, url: String) = viewModelScope.launch {
        when (val result = repository.setFeedUrl(serverId, path, url)) {
            is RequestResult.Success -> {
                eventChannel.send(Event.FeedUrlChanged)
                loadRssFeeds()
            }
            is RequestResult.Error -> {
                eventChannel.send(Event.Error(result))
            }
        }
    }

    sealed class Event {
        data class Error(val error: RequestResult.Error) : Event()
        data object AllFeedsRefreshed : Event()
        data object FeedAddError : Event()
        data object FeedRenameError : Event()
        data object FeedMoveError : Event()
        data object FeedDeleteError : Event()
        data object FolderAddError : Event()
        data object FolderRenameError : Event()
        data object FolderMoveError : Event()
        data object FolderDeleteError : Event()
        data object FeedAdded : Event()
        data object FeedRenamed : Event()
        data object FeedUrlChanged : Event()
        data object FeedMoved : Event()
        data object FeedDeleted : Event()
        data object FolderAdded : Event()
        data object FolderRenamed : Event()
        data object FolderMoved : Event()
        data object FolderDeleted : Event()
    }
}
