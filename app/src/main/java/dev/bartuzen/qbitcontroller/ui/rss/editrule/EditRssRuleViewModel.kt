package dev.bartuzen.qbitcontroller.ui.rss.editrule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.bartuzen.qbitcontroller.data.repositories.rss.EditRssRuleRepository
import dev.bartuzen.qbitcontroller.model.RssRule
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
    private val repository: EditRssRuleRepository
) : ViewModel() {
    private val _rssRule = MutableStateFlow<RssRule?>(null)
    val rssRule = _rssRule.asStateFlow()

    private val _categories = MutableStateFlow<List<String>?>(null)
    val categories = _categories.asStateFlow()

    private val eventChannel = Channel<Event>()
    val eventFlow = eventChannel.receiveAsFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    var isInitialLoadStarted = false

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

        val results = deferredList.awaitAll()
        _rssRule.value = results[0] as RssRule

        @Suppress("UNCHECKED_CAST")
        _categories.value = results[1] as List<String>
    }

    fun loadData(serverId: Int, ruleName: String) {
        if (!isLoading.value) {
            _isLoading.value = true
            updateData(serverId, ruleName).invokeOnCompletion {
                _isLoading.value = false
            }
        }
    }

    sealed class Event {
        data class Error(val error: RequestResult.Error) : Event()
        object RuleUpdated : Event()
        object RuleNotFound : Event()
    }
}
