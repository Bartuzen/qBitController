package dev.bartuzen.qbitcontroller.ui.rss.articles

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.bartuzen.qbitcontroller.data.repositories.rss.RssArticlesRepository
import dev.bartuzen.qbitcontroller.model.Article
import dev.bartuzen.qbitcontroller.model.serializers.parseRssFeedWithData
import dev.bartuzen.qbitcontroller.network.RequestResult
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RssArticlesViewModel @Inject constructor(
    private val repository: RssArticlesRepository
) : ViewModel() {
    private val rssArticles = MutableStateFlow<List<Article>?>(null)

    private val eventChannel = Channel<Event>()
    val eventFlow = eventChannel.receiveAsFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    private val searchQuery = MutableStateFlow("")

    var isInitialLoadStarted = false

    val filteredArticles = combine(rssArticles, searchQuery) { articles, searchQuery ->
        if (articles != null) {
            articles to searchQuery
        } else {
            null
        }
    }.filterNotNull().map { (articles, searchQuery) ->
        if (searchQuery.isNotEmpty()) {
            articles.filter { article ->
                article.title.contains(searchQuery, ignoreCase = true)
            }
        } else {
            articles
        }
    }

    private fun updateRssArticles(serverId: Int, feedPath: List<String>) = viewModelScope.launch {
        when (val result = repository.getRssFeeds(serverId)) {
            is RequestResult.Success -> {
                val articles = parseRssFeedWithData(result.data, feedPath)
                if (articles != null) {
                    rssArticles.value = articles
                } else {
                    eventChannel.send(Event.RssFeedNotFound)
                }
            }
            is RequestResult.Error -> {
                eventChannel.send(Event.Error(result))
            }
        }
    }

    fun loadRssArticles(serverId: Int, feedPath: List<String>) {
        if (!isLoading.value) {
            _isLoading.value = true
            updateRssArticles(serverId, feedPath).invokeOnCompletion {
                _isLoading.value = false
            }
        }
    }

    fun refreshRssArticles(serverId: Int, feedPath: List<String>) {
        if (!isRefreshing.value) {
            _isRefreshing.value = true
            updateRssArticles(serverId, feedPath).invokeOnCompletion {
                _isRefreshing.value = false
            }
        }
    }

    fun markAsRead(serverId: Int, feedPath: List<String>, articleId: String?) = viewModelScope.launch {
        when (val result = repository.markAsRead(serverId, feedPath, articleId)) {
            is RequestResult.Success -> {
                if (articleId == null) {
                    eventChannel.send(Event.AllArticlesMarkedAsRead)
                } else {
                    eventChannel.send(Event.ArticleMarkedAsRead)
                }
            }
            is RequestResult.Error -> {
                eventChannel.send(Event.Error(result))
            }
        }
    }

    fun refreshFeed(serverId: Int, feedPath: List<String>) = viewModelScope.launch {
        when (val result = repository.refreshItem(serverId, feedPath)) {
            is RequestResult.Success -> {
                eventChannel.send(Event.FeedRefreshed)
            }
            is RequestResult.Error -> {
                eventChannel.send(Event.Error(result))
            }
        }
    }

    fun setSearchQuery(query: String) {
        searchQuery.value = query
    }

    sealed class Event {
        data class Error(val error: RequestResult.Error) : Event()
        data object RssFeedNotFound : Event()
        data object ArticleMarkedAsRead : Event()
        data object AllArticlesMarkedAsRead : Event()
        data object FeedRefreshed : Event()
    }
}
