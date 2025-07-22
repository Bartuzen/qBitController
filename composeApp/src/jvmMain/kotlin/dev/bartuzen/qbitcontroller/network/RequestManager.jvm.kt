package dev.bartuzen.qbitcontroller.network

import dev.bartuzen.qbitcontroller.model.Protocol
import dev.bartuzen.qbitcontroller.model.ServerConfig
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.http.ContentType
import io.ktor.http.content.ChannelWriterContent
import io.ktor.http.content.OutgoingContent
import io.ktor.serialization.Configuration
import io.ktor.serialization.ContentConverter
import io.ktor.serialization.JsonConvertException
import io.ktor.serialization.kotlinx.guessSerializer
import io.ktor.serialization.kotlinx.serializerForTypeInfo
import io.ktor.util.reflect.TypeInfo
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.InternalAPI
import io.ktor.utils.io.charsets.Charset
import io.ktor.utils.io.core.remaining
import io.ktor.utils.io.jvm.javaio.toInputStream
import kotlinx.coroutines.CancellationException
import kotlinx.io.Buffer
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.io.encodeToSink
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.dnsoverhttps.DnsOverHttps
import java.net.ConnectException
import java.net.InetAddress
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager

actual fun createHttpClient(serverConfig: ServerConfig, block: HttpClientConfig<*>.() -> Unit) = HttpClient(OkHttp) {
    block()

    engine {
        config {
            retryOnConnectionFailure(true)

            if (serverConfig.protocol == Protocol.HTTPS && serverConfig.advanced.trustSelfSignedCertificates) {
                val trustAllManager = object : X509TrustManager {
                    override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}

                    override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}

                    override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
                }

                val sslContext = SSLContext.getInstance("SSL")
                sslContext.init(null, arrayOf(trustAllManager), SecureRandom())
                sslSocketFactory(sslContext.socketFactory, trustAllManager)
                hostnameVerifier { _, _ -> true }
            }

            if (serverConfig.advanced.dnsOverHttps != null) {
                val dns = DnsOverHttps.Builder().apply {
                    client(OkHttpClient())
                    url(serverConfig.advanced.dnsOverHttps.url.toHttpUrl())
                    bootstrapDnsHosts(
                        serverConfig.advanced.dnsOverHttps.bootstrapDnsHosts.map {
                            InetAddress.getByName(it)
                        },
                    )
                }.build()

                dns(dns)
            }
        }
    }
}

actual suspend fun <T> catchRequestError(
    block: suspend () -> RequestResult<T>,
    finally: suspend () -> Unit,
): RequestResult<T> = try {
    block()
} catch (_: ConnectException) {
    RequestResult.Error.RequestError.CannotConnect
} catch (_: SocketTimeoutException) {
    RequestResult.Error.RequestError.Timeout
} catch (_: UnknownHostException) {
    RequestResult.Error.RequestError.UnknownHost
} catch (e: Exception) {
    if (e is CancellationException) {
        throw e
    }
    RequestResult.Error.RequestError.Unknown("${e::class.simpleName} ${e.message}")
} finally {
    finally()
}

actual fun supportsSelfSignedCertificates() = true
actual fun supportsDnsOverHttps() = true

/**
 * Copy of [io.ktor.serialization.kotlinx.json.ExperimentalJsonConverter] that uses stream instead of source.
 */
@OptIn(InternalSerializationApi::class, InternalAPI::class)
private class JsonConverter(private val format: Json) : ContentConverter {
    override suspend fun serialize(
        contentType: ContentType,
        charset: Charset,
        typeInfo: TypeInfo,
        value: Any?,
    ): OutgoingContent {
        val serializer = try {
            format.serializersModule.serializerForTypeInfo(typeInfo)
        } catch (_: SerializationException) {
            guessSerializer(value, format.serializersModule)
        }
        val buffer = Buffer().also {
            format.encodeToSink(
                serializer as KSerializer<Any?>,
                value,
                it,
            )
        }
        return ChannelWriterContent(
            { writeBuffer.transferFrom(buffer) },
            contentType,
            contentLength = buffer.remaining,
        )
    }

    override suspend fun deserialize(charset: Charset, typeInfo: TypeInfo, content: ByteReadChannel): Any? {
        val serializer = format.serializersModule.serializerForTypeInfo(typeInfo)
        return try {
            format.decodeFromStream(serializer, content.toInputStream())
        } catch (cause: Throwable) {
            throw JsonConvertException("Illegal input: ${cause.message}", cause)
        }
    }
}

actual fun Configuration.platformJsonIo(json: Json, contentType: ContentType) {
    register(contentType, JsonConverter(json))
}
