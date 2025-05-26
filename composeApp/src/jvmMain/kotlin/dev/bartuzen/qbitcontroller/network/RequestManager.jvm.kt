package dev.bartuzen.qbitcontroller.network

import dev.bartuzen.qbitcontroller.model.Protocol
import dev.bartuzen.qbitcontroller.model.ServerConfig
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.okhttp.OkHttp
import kotlinx.coroutines.CancellationException
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
