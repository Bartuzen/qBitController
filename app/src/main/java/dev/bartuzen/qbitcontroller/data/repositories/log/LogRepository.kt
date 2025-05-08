package dev.bartuzen.qbitcontroller.data.repositories.log

import dev.bartuzen.qbitcontroller.network.RequestManager

class LogRepository(
    private val requestManager: RequestManager,
) {
    suspend fun getLog(serverId: Int) = requestManager.request(serverId) { service ->
        service.getLog()
    }
}
