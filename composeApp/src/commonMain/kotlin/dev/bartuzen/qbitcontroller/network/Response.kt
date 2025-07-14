package dev.bartuzen.qbitcontroller.network

import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.ktor.util.reflect.TypeInfo
import io.ktor.util.reflect.typeInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async

class Response<T>(
    private val rawResponse: HttpResponse,
    private val typeInfo: TypeInfo,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val bodyDeferred = scope.async<T> {
        rawResponse.body(typeInfo)
    }.also { it.start() }

    suspend fun body(): T = bodyDeferred.await()

    val code: Int = rawResponse.status.value
}

inline fun <reified T> HttpResponse.toResponse() = Response<T>(
    rawResponse = this,
    typeInfo = typeInfo<T>(),
)
