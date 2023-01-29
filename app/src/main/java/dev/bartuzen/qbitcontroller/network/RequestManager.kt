package dev.bartuzen.qbitcontroller.network

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import dev.bartuzen.qbitcontroller.data.ServerManager
import dev.bartuzen.qbitcontroller.model.Protocol
import dev.bartuzen.qbitcontroller.model.ServerConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton
import javax.net.ssl.SSLContext

@Singleton
class RequestManager @Inject constructor(
    private val serverManager: ServerManager,
    private val timeoutInterceptor: TimeoutInterceptor,
    private val userAgentInterceptor: UserAgentInterceptor,
    private val trustAllManager: TrustAllX509TrustManager
) {
    private val torrentServiceMap = mutableMapOf<Int, TorrentService>()
    private val loggedInServerIds = mutableListOf<Int>()
    private val initialLoginLock = Mutex()

    init {
        serverManager.addServerListener(object : ServerManager.ServerListener {
            override fun onServerAddedListener(serverConfig: ServerConfig) {}

            override fun onServerRemovedListener(serverConfig: ServerConfig) {
                torrentServiceMap.remove(serverConfig.id)
                loggedInServerIds.remove(serverConfig.id)
            }

            override fun onServerChangedListener(serverConfig: ServerConfig) {
                torrentServiceMap.remove(serverConfig.id)
                loggedInServerIds.remove(serverConfig.id)
            }
        })
    }

    private fun getTorrentService(serverId: Int) = torrentServiceMap.getOrPut(serverId) {
        val serverConfig = serverManager.getServer(serverId)
        val retrofit = Retrofit.Builder()
            .baseUrl(serverConfig.url)
            .client(
                OkHttpClient().newBuilder().apply {
                    cookieJar(SessionCookieJar())
                    addInterceptor(timeoutInterceptor)
                    addInterceptor(userAgentInterceptor)

                    val basicAuth = serverConfig.basicAuth
                    if (basicAuth.isEnabled && basicAuth.username != null && basicAuth.password != null) {
                        addInterceptor(BasicAuthInterceptor(basicAuth.username, basicAuth.password))
                    }

                    if (serverConfig.protocol == Protocol.HTTPS) {
                        hostnameVerifier { _, _ -> true }

                        if (serverConfig.trustSelfSignedCertificates) {
                            val sslContext = SSLContext.getInstance("SSL")
                            sslContext.init(null, arrayOf(trustAllManager), SecureRandom())
                            sslSocketFactory(sslContext.socketFactory, trustAllManager)
                        }
                    }
                }.build()
            )
            .addConverterFactory(ScalarsConverterFactory.create())
            .addConverterFactory(
                JacksonConverterFactory.create(
                    jacksonObjectMapper()
                        .enable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE)
                        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                )
            )
            .addConverterFactory(EnumConverterFactory())
            .build()
        retrofit.create(TorrentService::class.java)
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
        block: suspend (service: TorrentService) -> Response<T>
    ): RequestResult<T> {
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
        initialLoginLock.lock()
        if (serverId !in loggedInServerIds) {
            val loginResponse = tryLogin(serverId)
            if (loginResponse is RequestResult.Success) {
                loggedInServerIds.add(serverId)
                initialLoginLock.unlock()

                tryRequest(serverId, block)
            } else {
                loginResponse as RequestResult.Error
            }
        } else {
            initialLoginLock.unlock()
            val response = tryRequest(serverId, block)

            if (response is RequestResult.Error.ApiError && response.code == 403) {
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
    } finally {
        withContext(NonCancellable) {
            if (initialLoginLock.isLocked) {
                initialLoginLock.unlock()
            }
        }
    }
}

sealed class RequestResult<out T : Any?> {
    data class Success<out T : Any?>(val data: T) : RequestResult<T>()

    sealed class Error : RequestResult<Nothing>() {
        sealed class RequestError : Error() {
            object InvalidCredentials : RequestError()
            object Banned : RequestError()
            object CannotConnect : RequestError()
            object UnknownHost : RequestError()
            object Timeout : RequestError()
            object NoData : RequestError()
            data class UnknownLoginResponse(val response: String?) : RequestError()
            data class Unknown(val message: String?) : RequestError()
        }

        data class ApiError(val code: Int) : Error()
    }
}
