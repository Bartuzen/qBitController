package dev.bartuzen.qbitcontroller.ui.rss.editrule

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.bartuzen.qbitcontroller.data.repositories.rss.EditRssRuleRepository
import dev.bartuzen.qbitcontroller.model.RssFeedNode
import dev.bartuzen.qbitcontroller.model.RssRule
import dev.bartuzen.qbitcontroller.model.deserializers.parseRssFeeds
import dev.bartuzen.qbitcontroller.network.RequestResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EditRssRuleViewModel @Inject constructor(
    private val repository: EditRssRuleRepository,
    private val state: SavedStateHandle
) : ViewModel() {
    private val _rssRule = MutableStateFlow<RssRule?>(null)
    val rssRule = _rssRule.asStateFlow()

    val categories = state.getStateFlow<List<String>?>("categories", null)
    val feeds = state.getStateFlow<List<Pair<String, String>>?>("feeds", null)

    private val eventChannel = Channel<Event>()
    val eventFlow = eventChannel.receiveAsFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    var isInitialLoadStarted = false

    val isFetched = state.getStateFlow("isFetched", false)

    fun setRule(serverId: Int, name: String, rule: RssRule) = viewModelScope.launch {
        val mapper = jacksonObjectMapper()
        val json = mapper.writeValueAsString(rule)

        when (val result = repository.setRule(serverId, name, json)) {
            is RequestResult.Success -> {
                eventChannel.send(Event.RuleUpdated)
            }
            is RequestResult.Error -> {
                eventChannel.send(Event.Error(result))
            }
        }
    }

    private fun updateData(serverId: Int, ruleName: String) = viewModelScope.launch {
        if (isFetched.value) {
            return@launch
        }

        val deferredList = mutableListOf<Deferred<Any>>()

        deferredList += async {
            when (val result = repository.getRssRules(serverId)) {
                is RequestResult.Success -> {
                    val rule = result.data[ruleName]
                    if (rule != null) {
                        rule
                    } else {
                        eventChannel.send(Event.RuleNotFound)
                        throw CancellationException()
                    }
                }
                is RequestResult.Error -> {
                    eventChannel.send(Event.Error(result))
                    throw CancellationException()
                }
            }
        }
        deferredList += async {
            when (val result = repository.getCategories(serverId)) {
                is RequestResult.Success -> {
                    result.data.values
                        .toList()
                        .map { it.name }
                        .sorted()
                }
                is RequestResult.Error -> {
                    eventChannel.send(Event.Error(result))
                    throw CancellationException()
                }
            }
        }
        deferredList += async {
            when (val result = repository.getRssFeeds(serverId)) {
                is RequestResult.Success -> {
                    val feedNode = parseRssFeeds(result.data)

                    val feedList = mutableListOf<Pair<String, String>>()
                    val nodeQueue = ArrayDeque<RssFeedNode>()
                    nodeQueue.add(feedNode)
                    while (nodeQueue.isNotEmpty()) {
                        val currentNode = nodeQueue.removeFirst()
                        if (currentNode.isFeed) {
                            feedList.add(currentNode.name to currentNode.feed!!.url)
                        } else {
                            currentNode.children?.forEach { childNode ->
                                nodeQueue.add(childNode)
                            }
                        }
                    }
                    feedList.sortWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.first })
                    feedList
                }
                is RequestResult.Error -> {
                    eventChannel.send(Event.Error(result))
                    throw CancellationException()
                }
            }
        }

        val results = deferredList.awaitAll()
        _rssRule.value = results[0] as RssRule

        @Suppress("UNCHECKED_CAST")
        setCategories(results[1] as List<String>)

        @Suppress("UNCHECKED_CAST")
        setFeeds(results[2] as List<String>)

        onFetch()
    }

    fun loadData(serverId: Int, ruleName: String) {
        if (!isLoading.value) {
            _isLoading.value = true
            updateData(serverId, ruleName).invokeOnCompletion {
                _isLoading.value = false
            }
        }
    }

    private fun onFetch() {
        state["isFetched"] = true
    }

    private fun setCategories(categories: List<String>) {
        state["categories"] = categories
    }

    private fun setFeeds(feeds: List<String>) {
        state["feeds"] = feeds
    }

    sealed class Event {
        data class Error(val error: RequestResult.Error) : Event()
        object RuleUpdated : Event()
        object RuleNotFound : Event()
    }
}
