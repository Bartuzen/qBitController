package dev.bartuzen.qbitcontroller.ui.settings.addeditserver

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fasterxml.jackson.databind.JsonMappingException
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.bartuzen.qbitcontroller.data.ServerManager
import dev.bartuzen.qbitcontroller.model.ServerConfig
import dev.bartuzen.qbitcontroller.network.BasicAuthInterceptor
import dev.bartuzen.qbitcontroller.network.RequestResult
import dev.bartuzen.qbitcontroller.network.TimeoutInterceptor
import dev.bartuzen.qbitcontroller.network.TorrentService
import dev.bartuzen.qbitcontroller.network.TrustAllX509TrustManager
import dev.bartuzen.qbitcontroller.network.UserAgentInterceptor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory
import retrofit2.create
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.security.SecureRandom
import javax.inject.Inject
import javax.net.ssl.SSLContext

@HiltViewModel
class AddEditServerViewModel @Inject constructor(
    private val serverManager: ServerManager,
    private val timeoutInterceptor: TimeoutInterceptor,
    private val userAgentInterceptor: UserAgentInterceptor,
    private val trustAllManager: TrustAllX509TrustManager
) : ViewModel() {
    private val eventChannel = Channel<Event>()
    val eventFlow = eventChannel.receiveAsFlow()

    private val _isTesting = MutableStateFlow(false)
    val isTesting = _isTesting.asStateFlow()

    private var testJob: Job? = null

    fun getServerConfig(serverId: Int) = serverManager.getServer(serverId)

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
                val service = Retrofit.Builder()
                    .baseUrl(serverConfig.url)
                    .client(
                        OkHttpClient().newBuilder().apply {
                            addInterceptor(timeoutInterceptor)
                            addInterceptor(userAgentInterceptor)

                            val basicAuth = serverConfig.basicAuth
                            if (basicAuth.isEnabled && basicAuth.username != null && basicAuth.password != null) {
                                addInterceptor(BasicAuthInterceptor(basicAuth.username, basicAuth.password))
                            }

                            if (serverConfig.trustSelfSignedCertificates) {
                                val sslContext = SSLContext.getInstance("SSL")
                                sslContext.init(null, arrayOf(trustAllManager), SecureRandom())
                                sslSocketFactory(sslContext.socketFactory, trustAllManager)
                                hostnameVerifier { _, _ -> true }
                            }
                        }.build()
                    )
                    .addConverterFactory(ScalarsConverterFactory.create())
                    .build()
                    .create<TorrentService>()

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
            } catch (e: JsonMappingException) {
                if (e.cause is SocketTimeoutException) {
                    RequestResult.Error.RequestError.Timeout
                } else {
                    RequestResult.Error.RequestError.Unknown("${e::class.simpleName} ${e.message}")
                }
            } catch (e: Exception) {
                if (e is CancellationException) {
                    throw e
                }
                RequestResult.Error.RequestError.Unknown("${e::class.simpleName} ${e.message}")
            }

            eventChannel.send(
                if (error == null) {
                    Event.TestSuccess
                } else {
                    Event.TestFailure(error)
                }
            )
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
        object TestSuccess : Event()
    }
}
