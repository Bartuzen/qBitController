package dev.bartuzen.qbitcontroller.data.repositories.search

import dev.bartuzen.qbitcontroller.network.RequestManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SearchStartRepository @Inject constructor(
    private val requestManager: RequestManager,
) {
    suspend fun getPlugins(serverId: Int) = requestManager.request(serverId) { service ->
        service.getPlugins()
    }
}
