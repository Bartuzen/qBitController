package dev.bartuzen.qbitcontroller.network

import dev.bartuzen.qbitcontroller.data.SettingsManager
import okhttp3.Interceptor
import okhttp3.Response
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TimeoutInterceptor @Inject constructor(
    private val settingsManager: SettingsManager
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val connectionTimeout = settingsManager.connectionTimeout.value
        val request = chain.request()

        return chain
            .withConnectTimeout(connectionTimeout, TimeUnit.SECONDS)
            .withReadTimeout(connectionTimeout, TimeUnit.SECONDS)
            .withWriteTimeout(connectionTimeout, TimeUnit.SECONDS)
            .proceed(request)
    }
}
