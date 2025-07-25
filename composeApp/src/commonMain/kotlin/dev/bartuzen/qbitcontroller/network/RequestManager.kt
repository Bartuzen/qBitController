package dev.bartuzen.qbitcontroller.network

import dev.bartuzen.qbitcontroller.data.ServerManager
import dev.bartuzen.qbitcontroller.data.SettingsManager
import dev.bartuzen.qbitcontroller.generated.BuildConfig
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
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.serialization.Configuration
import io.ktor.serialization.kotlinx.json.DefaultJson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import qbitcontroller.composeapp.generated.resources.Res
import qbitcontroller.composeapp.generated.resources.app_name
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

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
        serverManager.addServerListener(
            remove = { serverConfig ->
                torrentServiceMap.remove(serverConfig.id)
                httpClientMap.remove(serverConfig.id)
                loggedInServerIds.remove(serverConfig.id)
                initialLoginLocks.remove(serverConfig.id)
                versions.remove(serverConfig.id)
                versionLocks.remove(serverConfig.id)
            },
            change = { serverConfig ->
                torrentServiceMap.remove(serverConfig.id)
                httpClientMap.remove(serverConfig.id)
                loggedInServerIds.remove(serverConfig.id)
                initialLoginLocks.remove(serverConfig.id)
                versions.remove(serverConfig.id)
                versionLocks.remove(serverConfig.id)
            },
        )
    }

    fun buildHttpClient(serverConfig: ServerConfig) = createHttpClient(serverConfig) {
        install(ContentNegotiation) {
            platformJsonIo(json)
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

        defaultRequest {
            serverConfig.advanced.customHeaders.forEach { (key, value) ->
                header(key, value)
            }
        }
    }

    fun getHttpClient(serverId: Int) = httpClientMap.getOrPut(serverId) {
        val serverConfig = serverManager.getServer(serverId)
        buildHttpClient(serverConfig)
    }

    fun buildTorrentService(serverConfig: ServerConfig, client: HttpClient) = TorrentService(
        client = client,
        baseUrl = serverConfig.requestUrl,
    )

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

                val parsedVersion = version.body?.let { versionString ->
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
            val body = loginResponse.body

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
        val body = blockResponse.body

        return when (code) {
            200 if body != null -> RequestResult.Success(body)
            403 -> RequestResult.Error.RequestError.InvalidCredentials
            else -> RequestResult.Error.ApiError(code)
        }
    }

    suspend fun <T : Any> request(serverId: Int, block: suspend (service: TorrentService) -> Response<T>) =
        catchRequestError(
            block = {
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
            },
            finally = {
                withContext(NonCancellable) {
                    val initialLoginLock = getInitialLoginLock(serverId)

                    if (initialLoginLock.isLocked) {
                        initialLoginLock.tryUnlock()
                    }
                }
            },
        )

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
            data object NoInternet : RequestError()
            data class UnknownLoginResponse(val response: String?) : RequestError()
            data class Unknown(val message: String) : RequestError()
        }

        data class ApiError(val code: Int) : Error()
    }
}

expect fun createHttpClient(serverConfig: ServerConfig, block: HttpClientConfig<*>.() -> Unit): HttpClient

expect suspend fun <T> catchRequestError(
    block: suspend () -> RequestResult<T>,
    finally: suspend () -> Unit = {},
): RequestResult<T>

expect fun supportsSelfSignedCertificates(): Boolean
expect fun supportsDnsOverHttps(): Boolean

expect fun Configuration.platformJsonIo(json: Json = DefaultJson, contentType: ContentType = ContentType.Application.Json)
