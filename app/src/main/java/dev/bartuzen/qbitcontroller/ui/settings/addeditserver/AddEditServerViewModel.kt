package dev.bartuzen.qbitcontroller.ui.settings.addeditserver

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.bartuzen.qbitcontroller.data.SettingsManager
import dev.bartuzen.qbitcontroller.model.ServerConfig
import dev.bartuzen.qbitcontroller.ui.common.PersistentMutableState
import dev.bartuzen.qbitcontroller.ui.common.PersistentState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import javax.inject.Inject

@HiltViewModel
class AddEditServerViewModel @Inject constructor(
    private val settingsManager: SettingsManager, state: SavedStateHandle
) : ViewModel() {
    var id by PersistentState(
        state = state,
        key = "id",
        defaultValue = -1
    )

    private val _host = mutableStateOf("")
    private val _username = mutableStateOf("")
    private val _password = mutableStateOf("")
    private val _name = mutableStateOf("")

    var host: String by PersistentMutableState(
        state = state,
        mutableState = _host,
        key = "host"
    )
    var username: String by PersistentMutableState(
        state = state,
        mutableState = _username,
        key = "username"
    )
    var password: String by PersistentMutableState(
        state = state,
        mutableState = _password,
        key = "password"
    )
    var name: String by PersistentMutableState(
        state = state,
        mutableState = _name,
        key = "name"
    )

    val eventFlow = MutableSharedFlow<AddEditServerEvent>()

    suspend fun updateDetails(serverId: Int) {
        val serverConfig = settingsManager.serversFlow.first()[serverId]
        serverConfig?.let { config ->
            id = serverId
            host = config.host
            username = config.username
            password = config.password
            name = config.name
        }
    }

    suspend fun saveServer(
        defaultServerName: String
    ) {
        if (isValid()) {
            val serverConfig = createServerConfig(defaultServerName)

            if (id == -1) {
                settingsManager.addServer(serverConfig)
                eventFlow.emit(AddEditServerEvent.ServerCreated)
            } else {
                settingsManager.editServer(serverConfig)
                eventFlow.emit(AddEditServerEvent.ServerEdited)
            }
        } else {
            eventFlow.emit(AddEditServerEvent.BlankFields)
        }
    }

    suspend fun deleteServer() {
        settingsManager.removeServer(id)
        eventFlow.emit(AddEditServerEvent.ServerDeleted)
    }

    private fun createServerConfig(defaultServerName: String) =
        ServerConfig(
            id,
            host,
            username,
            password,
            if (name.isNotBlank()) name else defaultServerName
        )

    private fun isValid() =
        host.isNotBlank() && username.isNotBlank() && password.isNotBlank()

    sealed class AddEditServerEvent {
        object ServerCreated : AddEditServerEvent()
        object ServerEdited : AddEditServerEvent()
        object ServerDeleted : AddEditServerEvent()
        object BlankFields : AddEditServerEvent()
    }
}