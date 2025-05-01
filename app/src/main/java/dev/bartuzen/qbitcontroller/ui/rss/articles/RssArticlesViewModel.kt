package dev.bartuzen.qbitcontroller.ui.rss.articles

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.bartuzen.qbitcontroller.data.repositories.rss.RssArticlesRepository
import dev.bartuzen.qbitcontroller.model.Article
import dev.bartuzen.qbitcontroller.model.serializers.parseRssFeedWithData
import dev.bartuzen.qbitcontroller.network.RequestResult
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel(assistedFactory = RssArticlesViewModel.Factory::class)
class RssArticlesViewModel @AssistedInject constructor(
    @Assisted private val serverId: Int,
    @Assisted feedPath: List<String>,
    @Assisted private val uid: String?,
    private val savedStateHandle: SavedStateHandle,
    private val repository: RssArticlesRepository,
) : ViewModel() {
    @AssistedFactory
    interface Factory {
        fun create(serverId: Int, feedPath: List<String>, uid: String?): RssArticlesViewModel
    }

    private val rssArticles = MutableStateFlow<List<Article>?>(null)

    private val eventChannel = Channel<Event>()
    val eventFlow = eventChannel.receiveAsFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    private val searchQuery = savedStateHandle.getStateFlow("searchQuery", "")

    val feedPath = savedStateHandle.getStateFlow("feedPath!", feedPath)

    val filteredArticles = combine(rssArticles, searchQuery) { articles, searchQuery ->
        if (articles != null) {
            articles to searchQuery
        } else {
            null
        }
    }.filterNotNull().map { (articles, searchQuery) ->
        if (searchQuery.isNotEmpty()) {
            articles.filter { article ->
                val matchesSearchQuery = searchQuery
                    .split(" ")
                    .filter { it.isNotEmpty() && it != "-" }
                    .all { term ->
                        val isExclusion = term.startsWith("-")
                        val cleanTerm = term.removePrefix("-")
                        val containsTerm = article.title.contains(cleanTerm, ignoreCase = true)

                        if (isExclusion) !containsTerm else containsTerm
                    }

                return@filter matchesSearchQuery
            }
        } else {
            articles
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    init {
        loadRssArticles()
    }

    fun updateRssArticles() = viewModelScope.launch {
        when (val result = repository.getRssFeeds(serverId)) {
            is RequestResult.Success -> {
                val (articles, newFeedPath) = parseRssFeedWithData(result.data, feedPath.value, uid)
                if (articles != null) {
                    rssArticles.value = articles

                    if (newFeedPath != null) {
                        savedStateHandle["feedPath!"] = newFeedPath
                        eventChannel.send(Event.FeedPathChanged)
                    }
                } else {
                    eventChannel.send(Event.RssFeedNotFound)
                }
            }
            is RequestResult.Error -> {
                eventChannel.send(Event.Error(result))
            }
        }
    }

    fun loadRssArticles() {
        if (!isLoading.value) {
            _isLoading.value = true
            updateRssArticles().invokeOnCompletion {
                _isLoading.value = false
            }
        }
    }

    fun refreshRssArticles() {
        if (!isRefreshing.value) {
            _isRefreshing.value = true
            updateRssArticles().invokeOnCompletion {
                viewModelScope.launch {
                    delay(25)
                    _isRefreshing.value = false
                }
            }
        }
    }

    fun markAsRead(feedPath: List<String>?, articleId: String?, showMessage: Boolean = true) = viewModelScope.launch {
        val finalFeedPath = feedPath ?: this@RssArticlesViewModel.feedPath.value
        when (val result = repository.markAsRead(serverId, finalFeedPath, articleId)) {
            is RequestResult.Success -> {
                if (articleId == null) {
                    eventChannel.send(Event.AllArticlesMarkedAsRead)
                } else {
                    eventChannel.send(Event.ArticleMarkedAsRead(showMessage))
                }
            }
            is RequestResult.Error -> {
                eventChannel.send(Event.Error(result))
            }
        }
    }

    fun markAllAsRead() = markAsRead(null, null)

    fun refreshFeed() = viewModelScope.launch {
        when (val result = repository.refreshItem(serverId, feedPath.value)) {
            is RequestResult.Success -> {
                eventChannel.send(Event.FeedRefreshed)
            }
            is RequestResult.Error -> {
                eventChannel.send(Event.Error(result))
            }
        }
    }

    fun setSearchQuery(query: String) {
        savedStateHandle["searchQuery"] = query
    }

    sealed class Event {
        data class Error(val error: RequestResult.Error) : Event()
        data object RssFeedNotFound : Event()
        data class ArticleMarkedAsRead(val showMessage: Boolean) : Event()
        data object AllArticlesMarkedAsRead : Event()
        data object FeedRefreshed : Event()
        data object FeedPathChanged : Event()
    }
}
