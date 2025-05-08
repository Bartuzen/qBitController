package dev.bartuzen.qbitcontroller.network

import dev.bartuzen.qbitcontroller.data.ServerManager
import dev.bartuzen.qbitcontroller.model.Protocol
import dev.bartuzen.qbitcontroller.model.QBittorrentVersion
import dev.bartuzen.qbitcontroller.model.ServerConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.dnsoverhttps.DnsOverHttps
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import retrofit2.create
import java.net.ConnectException
import java.net.InetAddress
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.security.SecureRandom
import java.time.Duration
import java.time.Instant
import javax.net.ssl.SSLContext

class RequestManager(
    private val serverManager: ServerManager,
    private val timeoutInterceptor: TimeoutInterceptor,
    private val userAgentInterceptor: UserAgentInterceptor,
    private val trustAllManager: TrustAllX509TrustManager,
) {
    private val torrentServiceMap = mutableMapOf<Int, TorrentService>()
    private val okHttpClientMap = mutableMapOf<Int, OkHttpClient>()

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
                okHttpClientMap.remove(serverConfig.id)
                loggedInServerIds.remove(serverConfig.id)
                initialLoginLocks.remove(serverConfig.id)
                versions.remove(serverConfig.id)
                versionLocks.remove(serverConfig.id)
            }

            override fun onServerChangedListener(serverConfig: ServerConfig) {
                torrentServiceMap.remove(serverConfig.id)
                okHttpClientMap.remove(serverConfig.id)
                loggedInServerIds.remove(serverConfig.id)
                initialLoginLocks.remove(serverConfig.id)
                versions.remove(serverConfig.id)
                versionLocks.remove(serverConfig.id)
            }
        })
    }

    fun buildOkHttpClient(serverConfig: ServerConfig) = OkHttpClient.Builder().apply clientBuilder@{
        cookieJar(SessionCookieJar())
        addInterceptor(timeoutInterceptor)
        addInterceptor(userAgentInterceptor)

        val basicAuth = serverConfig.advanced.basicAuth
        if (basicAuth.isEnabled && basicAuth.username != null && basicAuth.password != null) {
            addInterceptor(BasicAuthInterceptor(basicAuth.username, basicAuth.password))
        }

        if (serverConfig.protocol == Protocol.HTTPS && serverConfig.advanced.trustSelfSignedCertificates) {
            val sslContext = SSLContext.getInstance("SSL")
            sslContext.init(null, arrayOf(trustAllManager), SecureRandom())
            sslSocketFactory(sslContext.socketFactory, trustAllManager)
            hostnameVerifier { _, _ -> true }
        }

        retryOnConnectionFailure(true)

        if (serverConfig.advanced.dnsOverHttps != null) {
            val dns = DnsOverHttps.Builder().apply {
                client(this@clientBuilder.build())
                url(serverConfig.advanced.dnsOverHttps.url.toHttpUrl())
                bootstrapDnsHosts(serverConfig.advanced.dnsOverHttps.bootstrapDnsHosts.map { InetAddress.getByName(it) })
            }.build()

            dns(dns)
        }
    }.build()

    fun getOkHttpClient(serverId: Int) = okHttpClientMap.getOrPut(serverId) {
        val serverConfig = serverManager.getServer(serverId)
        buildOkHttpClient(serverConfig)
    }

    fun buildTorrentService(serverConfig: ServerConfig, okHttpClient: OkHttpClient) = Retrofit.Builder()
        .baseUrl(serverConfig.requestUrl)
        .client(okHttpClient)
        .addConverterFactory(ScalarsConverterFactory.create())
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create<TorrentService>()

    private fun getTorrentService(serverId: Int) = torrentServiceMap.getOrPut(serverId) {
        val serverConfig = serverManager.getServer(serverId)
        val okHttpClient = getOkHttpClient(serverId)
        buildTorrentService(serverConfig, okHttpClient)
    }

    private fun getInitialLoginLock(serverId: Int) = initialLoginLocks.getOrPut(serverId) { Mutex() }

    fun getQBittorrentVersion(serverId: Int) = versions[serverId]?.second ?: QBittorrentVersion.Invalid

    private suspend fun updateVersionIfNeeded(serverId: Int) {
        val versionLock = versionLocks.getOrPut(serverId) { Mutex() }
        versionLock.withLock {
            val isVersionValid = versions[serverId]?.let { (fetchDate, _) ->
                Duration.between(fetchDate, Instant.now()) < Duration.ofHours(1)
            } == true
            if (!isVersionValid) {
                val service = getTorrentService(serverId)
                val version = service.getVersion()

                val parsedVersion = version.body()?.let { versionString ->
                    QBittorrentVersion.fromString(versionString)
                } ?: QBittorrentVersion.Invalid
                versions[serverId] = Instant.now() to parsedVersion
            }
        }
    }

    private suspend fun tryLogin(serverId: Int): RequestResult<Unit> {
        val service = getTorrentService(serverId)
        val serverConfig = serverManager.getServer(serverId)

        return if (serverConfig.username != null && serverConfig.password != null) {
            val loginResponse = service.login(serverConfig.username, serverConfig.password)
            val code = loginResponse.code()
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
        val code = blockResponse.code()
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
