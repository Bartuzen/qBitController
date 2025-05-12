package dev.bartuzen.qbitcontroller.ui.settings.servers

import androidx.lifecycle.ViewModel
import dev.bartuzen.qbitcontroller.data.ServerManager

class ServersViewModel(
    serverManager: ServerManager,
) : ViewModel() {
    val servers = serverManager.serversFlow
}
