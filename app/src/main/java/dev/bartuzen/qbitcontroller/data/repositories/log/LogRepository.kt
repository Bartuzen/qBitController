package dev.bartuzen.qbitcontroller.data.repositories.log

import dev.bartuzen.qbitcontroller.network.RequestManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LogRepository @Inject constructor(
    private val requestManager: RequestManager,
) {
    suspend fun getLog(serverId: Int) = requestManager.request(serverId) { service ->
        service.getLog()
    }
}
