package dev.bartuzen.qbitcontroller.ui.settings.servers

import androidx.lifecycle.ViewModel
import dev.bartuzen.qbitcontroller.data.ServerManager

class ServersViewModel(
    private val serverManager: ServerManager,
) : ViewModel() {
    val servers = serverManager.serversFlow

    fun reorderServer(from: Int, to: Int) = serverManager.reorderServer(from, to)
}
