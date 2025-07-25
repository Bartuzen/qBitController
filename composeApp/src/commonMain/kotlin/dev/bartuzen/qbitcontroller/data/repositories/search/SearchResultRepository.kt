package dev.bartuzen.qbitcontroller.data.repositories.search

import dev.bartuzen.qbitcontroller.network.RequestManager

class SearchResultRepository(
    private val requestManager: RequestManager,
) {
    suspend fun startSearch(serverId: Int, pattern: String, category: String, plugins: String) =
        requestManager.request(serverId) { service ->
            service.startSearch(pattern, category, plugins)
        }

    suspend fun stopSearch(serverId: Int, searchId: Int) = requestManager.request(serverId) { service ->
        service.stopSearch(searchId)
    }

    suspend fun deleteSearch(serverId: Int, searchId: Int) = requestManager.request(serverId) { service ->
        service.deleteSearch(searchId)
    }

    suspend fun getSearchResults(serverId: Int, searchId: Int, offset: Int) = requestManager.request(serverId) { service ->
        service.getSearchResults(searchId, offset)
    }
}
