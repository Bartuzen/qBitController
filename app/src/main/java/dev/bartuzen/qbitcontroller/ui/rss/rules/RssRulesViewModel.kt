package dev.bartuzen.qbitcontroller.ui.rss.rules

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.bartuzen.qbitcontroller.data.repositories.rss.RssRulesRepository
import dev.bartuzen.qbitcontroller.model.RssRule
import dev.bartuzen.qbitcontroller.network.RequestResult
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RssRulesViewModel @Inject constructor(
    private val repository: RssRulesRepository
) : ViewModel() {
    private val _rssRules = MutableStateFlow<Map<String, RssRule>?>(null)
    val rssRules = _rssRules.asStateFlow()

    private val eventChannel = Channel<Event>()
    val eventFlow = eventChannel.receiveAsFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    var isInitialLoadStarted = false

    private fun updateRssRules(serverId: Int) = viewModelScope.launch {
        when (val result = repository.getRssRules(serverId)) {
            is RequestResult.Success -> {
                _rssRules.value = result.data.toSortedMap(String.CASE_INSENSITIVE_ORDER)
            }
            is RequestResult.Error -> {
                eventChannel.send(Event.Error(result))
            }
        }
    }

    fun loadRssRules(serverId: Int) {
        if (!isLoading.value) {
            _isLoading.value = true
            updateRssRules(serverId).invokeOnCompletion {
                _isLoading.value = false
            }
        }
    }

    fun refreshRssRules(serverId: Int) {
        if (!isRefreshing.value) {
            _isRefreshing.value = true
            updateRssRules(serverId).invokeOnCompletion {
                _isRefreshing.value = false
            }
        }
    }

    fun createRule(serverId: Int, name: String) = viewModelScope.launch {
        when (val result = repository.createRule(serverId, name)) {
            is RequestResult.Success -> {
                eventChannel.send(Event.RuleCreated)
            }
            is RequestResult.Error -> {
                eventChannel.send(Event.Error(result))
            }
        }
    }

    fun renameRule(serverId: Int, name: String, newName: String) = viewModelScope.launch {
        when (val result = repository.renameRule(serverId, name, newName)) {
            is RequestResult.Success -> {
                eventChannel.send(Event.RuleRenamed)
            }
            is RequestResult.Error -> {
                eventChannel.send(Event.Error(result))
            }
        }
    }

    fun deleteRule(serverId: Int, name: String) = viewModelScope.launch {
        when (val result = repository.deleteRule(serverId, name)) {
            is RequestResult.Success -> {
                eventChannel.send(Event.RuleDeleted)
            }
            is RequestResult.Error -> {
                eventChannel.send(Event.Error(result))
            }
        }
    }

    sealed class Event {
        data class Error(val error: RequestResult.Error) : Event()
        data object RuleCreated : Event()
        data object RuleRenamed : Event()
        data object RuleDeleted : Event()
    }
}
