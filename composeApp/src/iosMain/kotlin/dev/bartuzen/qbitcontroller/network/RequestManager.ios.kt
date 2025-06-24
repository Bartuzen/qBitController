package dev.bartuzen.qbitcontroller.network

import dev.bartuzen.qbitcontroller.model.ServerConfig
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.engine.darwin.DarwinHttpRequestException
import kotlinx.coroutines.CancellationException
import platform.Foundation.NSURLErrorCannotConnectToHost
import platform.Foundation.NSURLErrorCannotFindHost
import platform.Foundation.NSURLErrorNotConnectedToInternet
import platform.Foundation.NSURLErrorTimedOut

actual fun createHttpClient(serverConfig: ServerConfig, block: HttpClientConfig<*>.() -> Unit) = HttpClient(Darwin) {
    block()
}

actual suspend fun <T> catchRequestError(block: suspend () -> RequestResult<T>, finally: suspend () -> Unit) = try {
    block()
} catch (e: DarwinHttpRequestException) {
    when (e.origin.code) {
        NSURLErrorCannotConnectToHost -> RequestResult.Error.RequestError.CannotConnect
        NSURLErrorTimedOut -> RequestResult.Error.RequestError.Timeout
        NSURLErrorCannotFindHost -> RequestResult.Error.RequestError.UnknownHost
        NSURLErrorNotConnectedToInternet -> RequestResult.Error.RequestError.NoInternet
        else -> RequestResult.Error.RequestError.Unknown("${e::class.simpleName} ${e.message}")
    }
} catch (e: Exception) {
    if (e is CancellationException) {
        throw e
    }
    RequestResult.Error.RequestError.Unknown("${e::class.simpleName} ${e.message}")
} finally {
    finally()
}

actual fun supportsSelfSignedCertificates() = false
actual fun supportsDnsOverHttps() = false
