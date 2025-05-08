package dev.bartuzen.qbitcontroller.ui.settings.servers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.bartuzen.qbitcontroller.data.ServerManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class ServersViewModel(
    serverManager: ServerManager,
) : ViewModel() {
    val servers = serverManager.serversFlow
        .map { it.values.toList() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
