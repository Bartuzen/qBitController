package dev.bartuzen.qbitcontroller.network

import android.content.Context
import dev.bartuzen.qbitcontroller.BuildConfig
import dev.bartuzen.qbitcontroller.R
import okhttp3.Interceptor
import okhttp3.Response

class UserAgentInterceptor(
    context: Context,
) : Interceptor {
    private val userAgent = "${context.getString(R.string.app_name)}/${BuildConfig.VERSION_NAME}"

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
            .newBuilder()
            .header("User-Agent", userAgent)
            .build()
        return chain.proceed(request)
    }
}
