package dev.bartuzen.qbitcontroller.network

import dev.bartuzen.qbitcontroller.utils.getString
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import qBitController.composeApp.BuildConfig
import qbitcontroller.composeapp.generated.resources.Res
import qbitcontroller.composeapp.generated.resources.app_name

class UserAgentInterceptor() : Interceptor {
    private val userAgent = runBlocking { "${getString(Res.string.app_name)}/${BuildConfig.Version}" }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
            .newBuilder()
            .header("User-Agent", userAgent)
            .build()
        return chain.proceed(request)
    }
}
