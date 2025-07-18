package dev.bartuzen.qbitcontroller.network

import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.util.reflect.TypeInfo
import io.ktor.util.reflect.typeInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async

class Response<T>(
    private val rawResponse: HttpResponse,
    typeInfo: TypeInfo,
    customDeserializer: ((String) -> T)? = null,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val code: Int = rawResponse.status.value

    private val bodyDeferred = scope.async {
        if (code in 200..<300 && code != 204 && code != 205) {
            if (customDeserializer == null) {
                rawResponse.body(typeInfo)
            } else {
                customDeserializer(rawResponse.bodyAsText())
            }
        } else {
            null
        }
    }.also { it.start() }

    suspend fun body(): T? = bodyDeferred.await()
}

inline fun <reified T> HttpResponse.toResponse(noinline customDeserializer: ((String) -> T)? = null) = Response(
    rawResponse = this,
    typeInfo = typeInfo<T>(),
    customDeserializer = customDeserializer,
)
