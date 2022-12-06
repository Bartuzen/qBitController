package dev.bartuzen.qbitcontroller.network

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import dev.bartuzen.qbitcontroller.data.ServerManager
import dev.bartuzen.qbitcontroller.model.ServerConfig
import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RequestManager @Inject constructor(
    serverManager: ServerManager
) {
    private val torrentServiceMap = mutableMapOf<Int, TorrentService>()

    init {
        serverManager.addServerListener(object : ServerManager.ServerListener {
            override fun onServerAddedListener(serverConfig: ServerConfig) {}

            override fun onServerRemovedListener(serverConfig: ServerConfig) {
                torrentServiceMap.remove(serverConfig.id)
            }

            override fun onServerChangedListener(serverConfig: ServerConfig) {
                torrentServiceMap.remove(serverConfig.id)
            }
        })
    }

    private fun getTorrentService(serverConfig: ServerConfig) =
        torrentServiceMap.getOrPut(serverConfig.id) {
            val retrofit = Retrofit.Builder()
                .baseUrl(
                    buildString {
                        append("${serverConfig.protocolString}://${serverConfig.host}")
                        serverConfig.port?.let { port ->
                            append(":$port")
                        }
                        serverConfig.path?.let { path ->
                            append("/$path")
                        }
                    }
                )
                .client(
                    OkHttpClient().newBuilder()
                        .cookieJar(SessionCookieJar())
                        .build()
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

    suspend fun <T : Any> request(
        serverConfig: ServerConfig,
        block: suspend (service: TorrentService) -> Response<T>
    ): RequestResult<T> = try {
        val service = getTorrentService(serverConfig)
        val blockResponse = block(service)
        val body = blockResponse.body()

        if (blockResponse.message() == "Forbidden") {
            val loginResponse = service.login(serverConfig.username, serverConfig.password)

            if (loginResponse.code() == 403) {
                RequestResult.Error(RequestError.BANNED)
            } else if (loginResponse.body() == "Fails.") {
                RequestResult.Error(RequestError.INVALID_CREDENTIALS)
            } else if (!loginResponse.isSuccessful || loginResponse.body() != "Ok.") {
                RequestResult.Error(RequestError.UNKNOWN)
            } else {
                val newResponse = block(service)
                val newBody = newResponse.body()
                if (newBody != null) {
                    RequestResult.Success(newBody)
                } else {
                    RequestResult.Error(RequestError.UNKNOWN)
                }
            }
        } else if (!blockResponse.isSuccessful || body == null) {
            RequestResult.Error(RequestError.UNKNOWN)
        } else {
            RequestResult.Success(body)
        }
    } catch (e: ConnectException) {
        RequestResult.Error(RequestError.CANNOT_CONNECT)
    } catch (e: SocketTimeoutException) {
        RequestResult.Error(RequestError.TIMEOUT)
    } catch (e: UnknownHostException) {
        RequestResult.Error(RequestError.UNKNOWN_HOST)
    } catch (e: IllegalArgumentException) {
        RequestResult.Error(RequestError.UNKNOWN_HOST)
    } catch (e: JsonMappingException) {
        if (e.cause is SocketTimeoutException) {
            RequestResult.Error(RequestError.TIMEOUT)
        } else {
            throw e
        }
    } catch (e: Exception) {
        RequestResult.Error(RequestError.UNKNOWN)
    }
}

sealed class RequestResult<out T : Any?> {
    data class Success<out T : Any?>(val data: T) : RequestResult<T>()
    data class Error(val error: RequestError) : RequestResult<Nothing>()
}

enum class RequestError {
    INVALID_CREDENTIALS, BANNED, CANNOT_CONNECT, UNKNOWN_HOST, TIMEOUT, UNKNOWN
}
