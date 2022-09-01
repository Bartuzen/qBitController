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
    private val activityEventChannel = Channel<ActivityEvent>()
    val activityEventFlow = activityEventChannel.receiveAsFlow()

    private val fragmentEventChannel = Channel<FragmentEvent>()
    val fragmentEventFlow = fragmentEventChannel.receiveAsFlow()

    fun getServers() = runBlocking {
        settingsManager.serversFlow.first()
    }

    fun movePage(fragment: PreferenceFragmentCompat) = viewModelScope.launch {
        activityEventChannel.send(ActivityEvent.MovePage(fragment))
    }

    fun addServer(serverConfig: ServerConfig) = viewModelScope.launch {
        activityEventChannel.send(ActivityEvent.AddEditServerCompleted)
        settingsManager.addServer(serverConfig)
        fragmentEventChannel.send(FragmentEvent.AddEditServerCompleted)
    }

    fun editServer(serverConfig: ServerConfig) = viewModelScope.launch {
        activityEventChannel.send(ActivityEvent.AddEditServerCompleted)
        settingsManager.editServer(serverConfig)
        fragmentEventChannel.send(FragmentEvent.AddEditServerCompleted)
    }

    fun removeServer(serverConfig: ServerConfig) = viewModelScope.launch {
        activityEventChannel.send(ActivityEvent.AddEditServerCompleted)
        settingsManager.removeServer(serverConfig)
        fragmentEventChannel.send(FragmentEvent.AddEditServerCompleted)
    }

    sealed class ActivityEvent {
        data class MovePage(val fragment: PreferenceFragmentCompat) : ActivityEvent()
        object AddEditServerCompleted : ActivityEvent()
    }

    sealed class FragmentEvent {
        object AddEditServerCompleted : FragmentEvent()
    }
}