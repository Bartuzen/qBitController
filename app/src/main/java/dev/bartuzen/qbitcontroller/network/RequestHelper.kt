package dev.bartuzen.qbitcontroller.network

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
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
class RequestHelper @Inject constructor() {
    private val torrentServiceMap = mutableMapOf<Int, TorrentService>()

    fun getTorrentService(serverConfig: ServerConfig): TorrentService =
        serverConfig.run {
            torrentServiceMap.getOrElse(id) {
                val retrofit = Retrofit.Builder()
                    .baseUrl(
                        if (host.startsWith("http://") || host.startsWith("https://")) {
                            host
                        } else {
                            "http://$host"
                        }
                    )
                    .client(
                        OkHttpClient().newBuilder()
                            .cookieJar(SessionCookieJar()).build()
                    )
                    .addConverterFactory(ScalarsConverterFactory.create())
                    .addConverterFactory(
                        JacksonConverterFactory.create(
                            jacksonObjectMapper()
                                .enable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL)
                        )
                    )
                    .addConverterFactory(EnumConverterFactory())
                    .build()
                val service = retrofit.create(TorrentService::class.java)
                torrentServiceMap[id] = service
                service
            }
        }

    fun removeTorrentService(serverId: Int) {
        torrentServiceMap.remove(serverId)
    }

    private suspend fun login(serverConfig: ServerConfig) = serverConfig.run {
        getTorrentService(serverConfig).login(username, password)
    }

    suspend fun <T : Any> request(
        serverConfig: ServerConfig,
        block: suspend (host: String) -> Response<T>
    ): Pair<RequestResult, Response<T>?> {
        return try {
            val blockResponse = block(serverConfig.host)
            if (blockResponse.message() == "Forbidden") {
                val loginResponse = login(serverConfig)

                if (loginResponse.code() == 403) {
                    return Pair(RequestResult.BANNED, null)
                } else if (loginResponse.body() == "Fails.") {
                    return Pair(RequestResult.INVALID_CREDENTIALS, null)
                } else if (!loginResponse.isSuccessful || loginResponse.body() != "Ok.") {
                    return Pair(RequestResult.UNKNOWN, null)
                }

                val newResponse = block(serverConfig.host)
                return Pair(RequestResult.SUCCESS, newResponse)
            } else if (!blockResponse.isSuccessful || blockResponse.body() == null) {
                Pair(RequestResult.UNKNOWN, null)
            } else {
                Pair(RequestResult.SUCCESS, blockResponse)
            }
        } catch (e: ConnectException) {
            Pair(RequestResult.CANNOT_CONNECT, null)
        } catch (e: SocketTimeoutException) {
            Pair(RequestResult.TIMEOUT, null)
        } catch (e: UnknownHostException) {
            Pair(RequestResult.UNKNOWN_HOST, null)
        } catch (e: IllegalArgumentException) {
            Pair(RequestResult.UNKNOWN_HOST, null)
        } catch (e: JsonMappingException) {
            if (e.cause is SocketTimeoutException) {
                Pair(RequestResult.TIMEOUT, null)
            } else {
                throw e
            }
        }
    }
}

enum class RequestResult {
    SUCCESS, INVALID_CREDENTIALS, BANNED, CANNOT_CONNECT, UNKNOWN_HOST, TIMEOUT, UNKNOWN
}