package dev.bartuzen.qbitcontroller.ui.settings.serversettings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.bartuzen.qbitcontroller.data.repositories.serversettings.ServerSettingsRepository
import dev.bartuzen.qbitcontroller.model.ServerPreferences
import dev.bartuzen.qbitcontroller.network.RequestResult
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject

class ServerSettingsViewModel(
    private val serverId: Int,
    private val repository: ServerSettingsRepository,
) : ViewModel() {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = false
    }

    private val _preferences = MutableStateFlow<ServerPreferences?>(null)
    val preferences = _preferences.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving = _isSaving.asStateFlow()

    private val eventChannel = Channel<Event>()
    val eventFlow = eventChannel.receiveAsFlow()

    init {
        loadPreferences()
    }

    fun loadPreferences() {
        if (isLoading.value) {
            return
        }
        _isLoading.value = true
        viewModelScope.launch {
            when (val result = repository.getPreferences(serverId)) {
                is RequestResult.Success -> {
                    _preferences.value = result.data
                }
                is RequestResult.Error -> {
                    eventChannel.send(Event.Error(result))
                }
            }
            _isLoading.value = false
        }
    }

    fun savePreferences(newPreferences: ServerPreferences) {
        val oldPreferences = _preferences.value ?: return
        if (isSaving.value || oldPreferences == newPreferences) {
            return
        }

        val changes = getChanges(oldPreferences, newPreferences)
        if (changes.isEmpty()) {
            _preferences.value = newPreferences
            return
        }

        _isSaving.value = true
        viewModelScope.launch {
            when (val result = repository.setPreferences(serverId, changes)) {
                is RequestResult.Success -> {
                    _preferences.value = newPreferences
                    eventChannel.send(Event.Saved)
                }
                is RequestResult.Error -> {
                    eventChannel.send(Event.Error(result))
                }
            }
            _isSaving.value = false
        }
    }

    private fun getChanges(oldPreferences: ServerPreferences, newPreferences: ServerPreferences): Map<String, JsonElement> {
        val oldJson = json.encodePreferences(oldPreferences)
        val newJson = json.encodePreferences(newPreferences)

        return newJson.filter { (key, value) -> oldJson[key] != value }.toMap()
    }

    private fun Json.encodePreferences(preferences: ServerPreferences): JsonObject =
        encodeToJsonElement(ServerPreferences.serializer(), preferences).jsonObject

    sealed class Event {
        data class Error(val error: RequestResult.Error) : Event()
        data object Saved : Event()
    }
}
