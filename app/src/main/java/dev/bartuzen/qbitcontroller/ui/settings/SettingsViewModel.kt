package dev.bartuzen.qbitcontroller.ui.settings

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceFragmentCompat
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.bartuzen.qbitcontroller.data.SettingsManager
import dev.bartuzen.qbitcontroller.model.ServerConfig
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsManager: SettingsManager,
    val state: SavedStateHandle
) : ViewModel() {
    private val settingsActivityEventChannel = Channel<SettingsActivityEvent>()
    val settingsActivityEvent = settingsActivityEventChannel.receiveAsFlow()

    private val settingsFragmentEventChannel = Channel<SettingsFragmentEvent>()
    val settingsFragmentEvent = settingsFragmentEventChannel.receiveAsFlow()

    fun getServers() = runBlocking {
        settingsManager.serversFlow.first()
    }

    fun movePage(fragment: PreferenceFragmentCompat) = viewModelScope.launch {
        settingsActivityEventChannel.send(SettingsActivityEvent.MovePage(fragment))
    }

    fun addServer(serverConfig: ServerConfig) = viewModelScope.launch {
        settingsActivityEventChannel.send(SettingsActivityEvent.AddEditServerCompleted)
        settingsManager.addServer(serverConfig)
        settingsFragmentEventChannel.send(SettingsFragmentEvent.AddEditServerCompleted)
    }

    fun editServer(serverConfig: ServerConfig) = viewModelScope.launch {
        settingsActivityEventChannel.send(SettingsActivityEvent.AddEditServerCompleted)
        settingsManager.editServer(serverConfig)
        settingsFragmentEventChannel.send(SettingsFragmentEvent.AddEditServerCompleted)
    }

    fun removeServer(serverConfig: ServerConfig) = viewModelScope.launch {
        settingsActivityEventChannel.send(SettingsActivityEvent.AddEditServerCompleted)
        settingsManager.removeServer(serverConfig)
        settingsFragmentEventChannel.send(SettingsFragmentEvent.AddEditServerCompleted)
    }

    sealed class SettingsActivityEvent {
        data class MovePage(val fragment: PreferenceFragmentCompat) : SettingsActivityEvent()
        object AddEditServerCompleted : SettingsActivityEvent()
    }

    sealed class SettingsFragmentEvent {
        object AddEditServerCompleted : SettingsFragmentEvent()
    }
}