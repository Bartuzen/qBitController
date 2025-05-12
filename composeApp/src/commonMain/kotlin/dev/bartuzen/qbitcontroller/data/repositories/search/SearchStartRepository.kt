package dev.bartuzen.qbitcontroller.data.repositories.search

import dev.bartuzen.qbitcontroller.network.RequestManager

class SearchStartRepository(
    private val requestManager: RequestManager,
) {
    suspend fun getPlugins(serverId: Int) = requestManager.request(serverId) { service ->
        service.getPlugins()
    }
}
