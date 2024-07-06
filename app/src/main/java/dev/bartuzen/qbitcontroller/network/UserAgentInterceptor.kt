package dev.bartuzen.qbitcontroller.network

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.bartuzen.qbitcontroller.BuildConfig
import dev.bartuzen.qbitcontroller.R
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserAgentInterceptor @Inject constructor(
    @ApplicationContext private val context: Context,
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
