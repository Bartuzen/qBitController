package dev.bartuzen.qbitcontroller.ui.settings

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.bartuzen.qbitcontroller.data.ServerManager
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val serverManager: ServerManager
) : ViewModel() {
    fun getServers() = serverManager.serversFlow.value
}
