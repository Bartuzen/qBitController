package dev.bartuzen.qbitcontroller.ui.rss.feeds

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.bartuzen.qbitcontroller.data.repositories.rss.RssFeedRepository
import dev.bartuzen.qbitcontroller.model.RssFeedNode
import dev.bartuzen.qbitcontroller.model.serializers.parseRssFeeds
import dev.bartuzen.qbitcontroller.network.RequestResult
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.ArrayDeque
import javax.inject.Inject

@HiltViewModel
class RssFeedsViewModel @Inject constructor(
    private val repository: RssFeedRepository
) : ViewModel() {
    private val _rssFeeds = MutableStateFlow<RssFeedNode?>(null)
    val rssFeeds = _rssFeeds.asStateFlow()

    private val _currentDirectory = MutableStateFlow(ArrayDeque<String>())
    val currentDirectory = _currentDirectory.asStateFlow()

    private val eventChannel = Channel<Event>()
    val eventFlow = eventChannel.receiveAsFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    var isInitialLoadStarted = false

    private fun updateRssFeeds(serverId: Int) = viewModelScope.launch {
        when (val result = repository.getRssFeeds(serverId)) {
            is RequestResult.Success -> {
                _rssFeeds.value = parseRssFeeds(result.data)
            }
            is RequestResult.Error -> {
                eventChannel.send(Event.Error(result))
            }
        }
    }

    fun loadRssFeeds(serverId: Int) {
        if (!isLoading.value) {
            _isLoading.value = true
            updateRssFeeds(serverId).invokeOnCompletion {
                _isLoading.value = false
            }
        }
    }

    fun refreshRssFeeds(serverId: Int) {
        if (!isRefreshing.value) {
            _isRefreshing.value = true
            updateRssFeeds(serverId).invokeOnCompletion {
                _isRefreshing.value = false
            }
        }
    }

    fun refreshAllFeeds(serverId: Int) = viewModelScope.launch {
        when (val result = repository.refreshAllFeeds(serverId)) {
            is RequestResult.Success -> {
                eventChannel.send(Event.AllFeedsRefreshed)
            }
            is RequestResult.Error -> {
                eventChannel.send(Event.Error(result))
            }
        }
    }

    fun addRssFeed(serverId: Int, url: String, path: String) = viewModelScope.launch {
        when (val result = repository.addRssFeed(serverId, url, path)) {
            is RequestResult.Success -> {
                eventChannel.send(Event.FeedAdded)
            }
            is RequestResult.Error -> {
                if (result is RequestResult.Error.ApiError && result.code == 409) {
                    eventChannel.send(Event.FeedAddError)
                } else {
                    eventChannel.send(Event.Error(result))
                }
            }
        }
    }

    fun addRssFolder(serverId: Int, path: String) = viewModelScope.launch {
        when (val result = repository.addRssFolder(serverId, path)) {
            is RequestResult.Success -> {
                eventChannel.send(Event.FolderAdded)
            }
            is RequestResult.Error -> {
                if (result is RequestResult.Error.ApiError && result.code == 409) {
                    eventChannel.send(Event.FolderAddError)
                } else {
                    eventChannel.send(Event.Error(result))
                }
            }
        }
    }

    fun renameItem(serverId: Int, from: String, to: String, isFeed: Boolean) = viewModelScope.launch {
        when (val result = repository.moveItem(serverId, from, to)) {
            is RequestResult.Success -> {
                if (isFeed) {
                    eventChannel.send(Event.FeedRenamed)
                } else {
                    eventChannel.send(Event.FolderRenamed)
                }
            }
            is RequestResult.Error -> {
                if (result is RequestResult.Error.ApiError && result.code == 409) {
                    if (isFeed) {
                        eventChannel.send(Event.FeedRenameError)
                    } else {
                        eventChannel.send(Event.FolderRenameError)
                    }
                } else {
                    eventChannel.send(Event.Error(result))
                }
            }
        }
    }

    fun moveItem(serverId: Int, from: String, to: String, isFeed: Boolean) = viewModelScope.launch {
        when (val result = repository.moveItem(serverId, from, to)) {
            is RequestResult.Success -> {
                if (isFeed) {
                    eventChannel.send(Event.FeedMoved)
                } else {
                    eventChannel.send(Event.FolderMoved)
                }
            }
            is RequestResult.Error -> {
                if (result is RequestResult.Error.ApiError && result.code == 409) {
                    if (isFeed) {
                        eventChannel.send(Event.FeedMoveError)
                    } else {
                        eventChannel.send(Event.FolderMoveError)
                    }
                } else {
                    eventChannel.send(Event.Error(result))
                }
            }
        }
    }

    fun deleteItem(serverId: Int, path: String, isFeed: Boolean) = viewModelScope.launch {
        when (val result = repository.removeItem(serverId, path)) {
            is RequestResult.Success -> {
                if (isFeed) {
                    eventChannel.send(Event.FeedDeleted)
                } else {
                    eventChannel.send(Event.FolderDeleted)
                }
            }
            is RequestResult.Error -> {
                if (result is RequestResult.Error.ApiError && result.code == 409) {
                    if (isFeed) {
                        eventChannel.send(Event.FeedDeleteError)
                    } else {
                        eventChannel.send(Event.FolderDeleteError)
                    }
                } else {
                    eventChannel.send(Event.Error(result))
                }
            }
        }
    }

    fun goToFolder(node: String) {
        _currentDirectory.update { stack ->
            stack.clone().apply {
                push(node)
            }
        }
    }

    fun goBack() {
        _currentDirectory.update { stack ->
            stack.clone().apply {
                pop()
            }
        }
    }

    fun goToRoot() {
        _currentDirectory.update {
            ArrayDeque()
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
        data object FeedMoved : Event()
        data object FeedDeleted : Event()
        data object FolderAdded : Event()
        data object FolderRenamed : Event()
        data object FolderMoved : Event()
        data object FolderDeleted : Event()
    }
}
