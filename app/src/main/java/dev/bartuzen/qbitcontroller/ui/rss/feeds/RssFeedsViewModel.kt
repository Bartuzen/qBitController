package dev.bartuzen.qbitcontroller.ui.rss.feeds

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.bartuzen.qbitcontroller.data.repositories.rss.RssFeedRepository
import dev.bartuzen.qbitcontroller.model.RssFeedNode
import dev.bartuzen.qbitcontroller.model.ServerConfig
import dev.bartuzen.qbitcontroller.model.deserializers.parseRssFeeds
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

    private fun updateRssFeeds(serverConfig: ServerConfig) = viewModelScope.launch {
        when (val result = repository.getRssFeeds(serverConfig)) {
            is RequestResult.Success -> {
                _rssFeeds.value = parseRssFeeds(result.data)
            }
            is RequestResult.Error -> {
                eventChannel.send(Event.Error(result))
            }
        }
    }

    fun loadRssFeeds(serverConfig: ServerConfig) {
        if (!isLoading.value) {
            _isLoading.value = true
            updateRssFeeds(serverConfig).invokeOnCompletion {
                _isLoading.value = false
            }
        }
    }

    fun refreshRssFeeds(serverConfig: ServerConfig) {
        if (!isRefreshing.value) {
            _isRefreshing.value = true
            updateRssFeeds(serverConfig).invokeOnCompletion {
                _isRefreshing.value = false
            }
        }
    }

    fun addRssFeed(serverConfig: ServerConfig, url: String, path: String) = viewModelScope.launch {
        when (val result = repository.addRssFeed(serverConfig, url, path)) {
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

    fun addRssFolder(serverConfig: ServerConfig, path: String) = viewModelScope.launch {
        when (val result = repository.addRssFolder(serverConfig, path)) {
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

    fun renameItem(serverConfig: ServerConfig, from: String, to: String, isFeed: Boolean) = viewModelScope.launch {
        when (val result = repository.moveItem(serverConfig, from, to)) {
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

    fun moveItem(serverConfig: ServerConfig, from: String, to: String, isFeed: Boolean) = viewModelScope.launch {
        when (val result = repository.moveItem(serverConfig, from, to)) {
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

    fun deleteItem(serverConfig: ServerConfig, path: String, isFeed: Boolean) = viewModelScope.launch {
        when (val result = repository.removeItem(serverConfig, path)) {
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
        object FeedAddError : Event()
        object FeedRenameError : Event()
        object FeedMoveError : Event()
        object FeedDeleteError : Event()
        object FolderAddError : Event()
        object FolderRenameError : Event()
        object FolderMoveError : Event()
        object FolderDeleteError : Event()
        object FeedAdded : Event()
        object FeedRenamed : Event()
        object FeedMoved : Event()
        object FeedDeleted : Event()
        object FolderAdded : Event()
        object FolderRenamed : Event()
        object FolderMoved : Event()
        object FolderDeleted : Event()
    }
}
