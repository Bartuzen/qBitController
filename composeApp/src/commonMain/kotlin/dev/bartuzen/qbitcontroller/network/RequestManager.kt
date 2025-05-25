package dev.bartuzen.qbitcontroller.network

import de.jensklingenberg.ktorfit.Ktorfit
import de.jensklingenberg.ktorfit.Response
import de.jensklingenberg.ktorfit.converter.ResponseConverterFactory
import dev.bartuzen.qbitcontroller.data.ServerManager
import dev.bartuzen.qbitcontroller.data.SettingsManager
import dev.bartuzen.qbitcontroller.model.QBittorrentVersion
import dev.bartuzen.qbitcontroller.model.ServerConfig
import dev.bartuzen.qbitcontroller.utils.getString
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.UserAgent
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BasicAuthCredentials
import io.ktor.client.plugins.auth.providers.basic
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.cookies.AcceptAllCookiesStorage
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.json.Json
import qBitController.composeApp.BuildConfig
import qbitcontroller.composeapp.generated.resources.Res
import qbitcontroller.composeapp.generated.resources.app_name
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds

class RequestManager(
    private val serverManager: ServerManager,
    private val settingsManager: SettingsManager,
) {
    private val torrentServiceMap = mutableMapOf<Int, TorrentService>()
    private val httpClientMap = mutableMapOf<Int, HttpClient>()

    private val loggedInServerIds = mutableListOf<Int>()
    private val initialLoginLocks = mutableMapOf<Int, Mutex>()

    private val versions = mutableMapOf<Int, Pair<Instant, QBittorrentVersion>>()
    private val versionLocks = mutableMapOf<Int, Mutex>()

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        explicitNulls = false
    }

    init {
        serverManager.addServerListener(object : ServerManager.ServerListener {
            override fun onServerAddedListener(serverConfig: ServerConfig) {}

            override fun onServerRemovedListener(serverConfig: ServerConfig) {
                torrentServiceMap.remove(serverConfig.id)
                httpClientMap.remove(serverConfig.id)
                loggedInServerIds.remove(serverConfig.id)
                initialLoginLocks.remove(serverConfig.id)
                versions.remove(serverConfig.id)
                versionLocks.remove(serverConfig.id)
            }

            override fun onServerChangedListener(serverConfig: ServerConfig) {
                torrentServiceMap.remove(serverConfig.id)
                httpClientMap.remove(serverConfig.id)
                loggedInServerIds.remove(serverConfig.id)
                initialLoginLocks.remove(serverConfig.id)
                versions.remove(serverConfig.id)
                versionLocks.remove(serverConfig.id)
            }
        })
    }

    fun buildHttpClient(serverConfig: ServerConfig) = createHttpClient(serverConfig) {
        install(ContentNegotiation) {
            this.json(json)
        }

        install(HttpCookies) {
            storage = AcceptAllCookiesStorage()
        }

        install(HttpTimeout) {
            CoroutineScope(Dispatchers.Default).launch {
                settingsManager.connectionTimeout.flow.collectLatest {
                    requestTimeoutMillis = it.seconds.inWholeMilliseconds
                    connectTimeoutMillis = it.seconds.inWholeMilliseconds
                    socketTimeoutMillis = it.seconds.inWholeMilliseconds
                }
            }
        }

        install(UserAgent) {
            agent = runBlocking { "${getString(Res.string.app_name)}/${BuildConfig.Version}" }
        }

        val basicAuth = serverConfig.advanced.basicAuth
        if (basicAuth.isEnabled && basicAuth.username != null && basicAuth.password != null) {
            install(Auth) {
                basic {
                    credentials {
                        BasicAuthCredentials(basicAuth.username, basicAuth.password)
                    }
                }
            }
        }
    }

    fun getHttpClient(serverId: Int) = httpClientMap.getOrPut(serverId) {
        val serverConfig = serverManager.getServer(serverId)
        buildHttpClient(serverConfig)
    }

    fun buildTorrentService(serverConfig: ServerConfig, client: HttpClient) = Ktorfit.Builder()
        .baseUrl(serverConfig.requestUrl)
        .httpClient(client)
        .converterFactories(ResponseConverterFactory())
        .build()
        .createTorrentService()

    private fun getTorrentService(serverId: Int) = torrentServiceMap.getOrPut(serverId) {
        val serverConfig = serverManager.getServer(serverId)
        val httpClient = getHttpClient(serverId)
        buildTorrentService(serverConfig, httpClient)
    }

    private fun getInitialLoginLock(serverId: Int) = initialLoginLocks.getOrPut(serverId) { Mutex() }

    fun getQBittorrentVersion(serverId: Int) = versions[serverId]?.second ?: QBittorrentVersion.Invalid

    private suspend fun updateVersionIfNeeded(serverId: Int) {
        val versionLock = versionLocks.getOrPut(serverId) { Mutex() }
        versionLock.withLock {
            val isVersionValid = versions[serverId]?.let { (fetchDate, _) ->
                Clock.System.now() - fetchDate < 1.hours
            } == true
            if (!isVersionValid) {
                val service = getTorrentService(serverId)
                val version = service.getVersion()

                val parsedVersion = version.body()?.let { versionString ->
                    QBittorrentVersion.fromString(versionString)
                } ?: QBittorrentVersion.Invalid
                versions[serverId] = Clock.System.now() to parsedVersion
            }
        }
    }

    private suspend fun tryLogin(serverId: Int): RequestResult<Unit> {
        val service = getTorrentService(serverId)
        val serverConfig = serverManager.getServer(serverId)

        return if (serverConfig.username != null && serverConfig.password != null) {
            val loginResponse = service.login(serverConfig.username, serverConfig.password)
            val code = loginResponse.code
            val body = loginResponse.body()

            when {
                code == 403 -> RequestResult.Error.RequestError.Banned
                body == "Fails." -> RequestResult.Error.RequestError.InvalidCredentials
                body != "Ok." -> RequestResult.Error.RequestError.UnknownLoginResponse(body)
                else -> RequestResult.Success(Unit)
            }
        } else {
            RequestResult.Success(Unit)
        }
    }

    private suspend fun <T : Any> tryRequest(
        serverId: Int,
        block: suspend (service: TorrentService) -> Response<T>,
    ): RequestResult<T> {
        updateVersionIfNeeded(serverId)

        val service = getTorrentService(serverId)

        val blockResponse = block(service)
        val code = blockResponse.code
        val body = blockResponse.body()

        return if (code == 200 && body != null) {
            RequestResult.Success(body)
        } else if (code == 403) {
            RequestResult.Error.RequestError.InvalidCredentials
        } else {
            RequestResult.Error.ApiError(code)
        }
    }

    suspend fun <T : Any> request(serverId: Int, block: suspend (service: TorrentService) -> Response<T>) = try {
        val initialLoginLock = getInitialLoginLock(serverId)

        initialLoginLock.lock()
        if (serverId !in loggedInServerIds) {
            val loginResponse = tryLogin(serverId)
            if (loginResponse is RequestResult.Success) {
                loggedInServerIds.add(serverId)
                initialLoginLock.tryUnlock()

                tryRequest(serverId, block)
            } else {
                loginResponse as RequestResult.Error
            }
        } else {
            initialLoginLock.tryUnlock()
            val response = tryRequest(serverId, block)

            if (response is RequestResult.Error.RequestError.InvalidCredentials) {
                val loginResponse = tryLogin(serverId)

                if (loginResponse is RequestResult.Success) {
                    tryRequest(serverId, block)
                } else {
                    loginResponse as RequestResult.Error
                }
            } else {
                response
            }
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
    } finally {
        withContext(NonCancellable) {
            val initialLoginLock = getInitialLoginLock(serverId)

            if (initialLoginLock.isLocked) {
                initialLoginLock.tryUnlock()
            }
        }
    }

    private fun Mutex.tryUnlock() {
        try {
            unlock()
        } catch (_: IllegalStateException) {
        }
    }
}

sealed class RequestResult<out T : Any?> {
    data class Success<out T : Any?>(val data: T) : RequestResult<T>()

    sealed class Error : RequestResult<Nothing>() {
        sealed class RequestError : Error() {
            data object InvalidCredentials : RequestError()
            data object Banned : RequestError()
            data object CannotConnect : RequestError()
            data object UnknownHost : RequestError()
            data object Timeout : RequestError()
            data object NoData : RequestError()
            data class UnknownLoginResponse(val response: String?) : RequestError()
            data class Unknown(val message: String) : RequestError()
        }

        data class ApiError(val code: Int) : Error()
    }
}

expect fun createHttpClient(serverConfig: ServerConfig, block: HttpClientConfig<*>.() -> Unit): HttpClient
