package dev.bartuzen.qbitcontroller.ui.settings.addeditserver

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.bartuzen.qbitcontroller.data.ServerManager
import dev.bartuzen.qbitcontroller.model.ServerConfig
import dev.bartuzen.qbitcontroller.network.RequestManager
import dev.bartuzen.qbitcontroller.network.RequestResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

@HiltViewModel(assistedFactory = AddEditServerViewModel.Factory::class)
class AddEditServerViewModel @AssistedInject constructor(
    @Assisted private val serverId: Int?,
    private val serverManager: ServerManager,
    private val requestManager: RequestManager,
) : ViewModel() {
    @AssistedFactory
    interface Factory {
        fun create(serverId: Int?): AddEditServerViewModel
    }

    private val eventChannel = Channel<Event>()
    val eventFlow = eventChannel.receiveAsFlow()

    private val _isTesting = MutableStateFlow(false)
    val isTesting = _isTesting.asStateFlow()

    private var testJob: Job? = null

    val serverConfig = serverId?.let { serverManager.getServer(it) }

    fun addServer(serverConfig: ServerConfig) = viewModelScope.launch {
        serverManager.addServer(serverConfig)
    }

    fun editServer(serverConfig: ServerConfig) = viewModelScope.launch {
        serverManager.editServer(serverConfig)
    }

    fun removeServer(serverId: Int) = viewModelScope.launch {
        serverManager.removeServer(serverId)
    }

    fun testConnection(serverConfig: ServerConfig) {
        testJob?.cancel()

        _isTesting.value = true
        val job = viewModelScope.launch {
            val error = try {
                val service =
                    requestManager.buildTorrentService(serverConfig, requestManager.buildOkHttpClient(serverConfig))

                val response = service.login(serverConfig.username ?: "", serverConfig.password ?: "")

                if (response.code() == 403) {
                    RequestResult.Error.RequestError.Banned
                } else if (response.body() == "Fails.") {
                    RequestResult.Error.RequestError.InvalidCredentials
                } else if (response.body() != "Ok.") {
                    RequestResult.Error.RequestError.UnknownLoginResponse(response.body())
                } else {
                    null
                }
            } catch (e: ConnectException) {
                RequestResult.Error.RequestError.CannotConnect
            } catch (e: SocketTimeoutException) {
                RequestResult.Error.RequestError.Timeout
            } catch (e: UnknownHostException) {
                RequestResult.Error.RequestError.UnknownHost
            } catch (e: Exception) {
                if (e is CancellationException) {
                    throw e
                }
                RequestResult.Error.RequestError.Unknown("${e::class.simpleName} ${e.message}")
            }

            if (error == null) {
                eventChannel.send(Event.TestSuccess)
            } else {
                eventChannel.send(Event.TestFailure(error))
            }
        }

        job.invokeOnCompletion { e ->
            if (e !is CancellationException) {
                _isTesting.value = false
                testJob = null
            }
        }

        testJob = job
    }

    sealed class Event {
        data class TestFailure(val error: RequestResult.Error) : Event()
        data object TestSuccess : Event()
    }
}
