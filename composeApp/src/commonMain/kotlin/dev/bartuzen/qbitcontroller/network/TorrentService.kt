package dev.bartuzen.qbitcontroller.network

import dev.bartuzen.qbitcontroller.model.Category
import dev.bartuzen.qbitcontroller.model.Log
import dev.bartuzen.qbitcontroller.model.MainData
import dev.bartuzen.qbitcontroller.model.PieceState
import dev.bartuzen.qbitcontroller.model.Plugin
import dev.bartuzen.qbitcontroller.model.RssFeedNode
import dev.bartuzen.qbitcontroller.model.RssFeedWithData
import dev.bartuzen.qbitcontroller.model.RssFeedWithDataSerializer
import dev.bartuzen.qbitcontroller.model.RssRule
import dev.bartuzen.qbitcontroller.model.Search
import dev.bartuzen.qbitcontroller.model.StartSearch
import dev.bartuzen.qbitcontroller.model.Torrent
import dev.bartuzen.qbitcontroller.model.TorrentFile
import dev.bartuzen.qbitcontroller.model.TorrentPeers
import dev.bartuzen.qbitcontroller.model.TorrentProperties
import dev.bartuzen.qbitcontroller.model.TorrentTracker
import dev.bartuzen.qbitcontroller.model.TorrentWebSeed
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.prepareForm
import io.ktor.client.request.prepareGet
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.appendEncodedPathSegments
import io.ktor.http.parametersOf
import io.ktor.http.takeFrom
import io.ktor.serialization.kotlinx.json.DefaultJson
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.serializer

class TorrentService(
    val client: HttpClient,
    val baseUrl: String,
) {
    suspend inline fun <reified T> get(path: String, parameters: Map<String, Any?> = emptyMap()): Response<T> =
        client.prepareGet {
            url.takeFrom(baseUrl).appendEncodedPathSegments("api/v2/$path")
            parameters.forEach { (key, value) ->
                if (value != null) {
                    url.parameters.append(key, value.toString())
                }
            }
        }.execute(::execute)

    suspend inline fun <reified T> get(
        path: String,
        parameters: Map<String, Any?> = emptyMap(),
        deserializer: DeserializationStrategy<T>,
        json: Json = DefaultJson,
    ): Response<T> = client.prepareGet {
        url.takeFrom(baseUrl).appendEncodedPathSegments("api/v2/$path")
        parameters.forEach { (key, value) ->
            if (value != null) {
                url.parameters.append(key, value.toString())
            }
        }
    }.execute { response ->
        execute(response) {
            decodeJson(
                channel = response.bodyAsChannel(),
                deserializer = deserializer,
                json = json,
            )
        }
    }

    suspend inline fun <reified T> post(path: String, parameters: Map<String, Any?> = emptyMap()): Response<T> =
        client.prepareForm(
            formParameters = parametersOf(
                parameters
                    .filterValues { it != null }
                    .mapValues { listOf(it.value.toString()) },
            ),
        ) {
            url.takeFrom(baseUrl).appendEncodedPathSegments("api/v2/$path")
        }.execute(::execute)

    suspend inline fun <reified T> post(path: String, body: MultiPartFormDataContent): Response<T> = client.prepareForm {
        url.takeFrom(baseUrl).appendEncodedPathSegments("api/v2/$path")
        setBody(body)
    }.execute(::execute)

    suspend inline fun <reified T> execute(
        response: HttpResponse,
        noinline body: suspend () -> T? = { response.body<T>() },
    ): Response<T> = withContext(Dispatchers.Default) {
        val code = response.status.value
        val body = if (code in 200..<300 && code != 204 && code != 205) body() else null

        Response(code, body)
    }

    suspend fun login(username: String, password: String): Response<String> = post(
        "auth/login",
        mapOf("username" to username, "password" to password),
    )

    suspend fun getVersion(): Response<String> = get("app/version")

    suspend fun getDefaultSavePath(): Response<String> = get("app/defaultSavePath")

    suspend fun shutdown(): Response<String> = post("app/shutdown")

    suspend fun getLog(): Response<List<Log>> = get("log/main")

    suspend fun getMainData(): Response<MainData> = get("sync/maindata")

    suspend fun getPartialMainData(rid: Int): Response<JsonElement> = get(
        "sync/maindata",
        mapOf("rid" to rid),
    )

    suspend fun toggleSpeedLimitsMode(): Response<Unit> = post("transfer/toggleSpeedLimitsMode")

    suspend fun setDownloadSpeedLimit(limit: Int): Response<Unit> = post(
        "transfer/setDownloadLimit",
        mapOf("limit" to limit),
    )

    suspend fun setUploadSpeedLimit(limit: Int): Response<Unit> = post(
        "transfer/setUploadLimit",
        mapOf("limit" to limit),
    )

    suspend fun getTorrentList(hashes: String? = null): Response<List<Torrent>> = get(
        "torrents/info",
        mapOf("hashes" to hashes),
    )

    suspend fun getFiles(hash: String): Response<List<TorrentFile>> = get(
        "torrents/files",
        mapOf("hash" to hash),
    )

    suspend fun deleteTorrents(hashes: String, deleteFiles: Boolean): Response<Unit> = post(
        "torrents/delete",
        mapOf("hashes" to hashes, "deleteFiles" to deleteFiles),
    )

    suspend fun pauseTorrents(hashes: String): Response<String> = post(
        "torrents/pause",
        mapOf("hashes" to hashes),
    )

    suspend fun resumeTorrents(hashes: String): Response<String> = post(
        "torrents/resume",
        mapOf("hashes" to hashes),
    )

    suspend fun stopTorrents(hashes: String): Response<String> = post(
        "torrents/stop",
        mapOf("hashes" to hashes),
    )

    suspend fun startTorrents(hashes: String): Response<String> = post(
        "torrents/start",
        mapOf("hashes" to hashes),
    )

    suspend fun recheckTorrents(hashes: String): Response<Unit> = post(
        "torrents/recheck",
        mapOf("hashes" to hashes),
    )

    suspend fun reannounceTorrents(hashes: String): Response<Unit> = post(
        "torrents/reannounce",
        mapOf("hashes" to hashes),
    )

    suspend fun getTorrentPieces(hash: String): Response<List<PieceState>> = get(
        "torrents/pieceStates",
        mapOf("hash" to hash),
    )

    suspend fun getTorrentProperties(hash: String): Response<TorrentProperties> = get(
        "torrents/properties",
        mapOf("hash" to hash),
    )

    suspend fun getTorrentTrackers(hash: String): Response<List<TorrentTracker>> = get(
        "torrents/trackers",
        mapOf("hash" to hash),
    )

    suspend fun getWebSeeds(hash: String): Response<List<TorrentWebSeed>> = get(
        "torrents/webseeds",
        mapOf("hash" to hash),
    )

    suspend fun addTorrentTrackers(hash: String, urls: String): Response<Unit> = post(
        "torrents/addTrackers",
        mapOf("hash" to hash, "urls" to urls),
    )

    suspend fun deleteTorrentTrackers(hash: String, urls: String): Response<Unit> = post(
        "torrents/removeTrackers",
        mapOf("hash" to hash, "urls" to urls),
    )

    suspend fun editTorrentTrackers(hash: String, tracker: String, newUrl: String): Response<Unit> = post(
        "torrents/editTracker",
        mapOf("hash" to hash, "origUrl" to tracker, "newUrl" to newUrl),
    )

    suspend fun getCategories(): Response<Map<String, Category>> = get("torrents/categories")

    suspend fun getTags(): Response<List<String>> = get("torrents/tags")

    suspend fun deleteCategories(categories: String): Response<Unit> = post(
        "torrents/removeCategories",
        mapOf("categories" to categories),
    )

    suspend fun deleteTags(tags: String): Response<Unit> = post(
        "torrents/deleteTags",
        mapOf("tags" to tags),
    )

    suspend fun increaseTorrentPriority(hashes: String): Response<Unit> = post(
        "torrents/increasePrio",
        mapOf("hashes" to hashes),
    )

    suspend fun decreaseTorrentPriority(hashes: String): Response<Unit> = post(
        "torrents/decreasePrio",
        mapOf("hashes" to hashes),
    )

    suspend fun maximizeTorrentPriority(hashes: String): Response<Unit> = post(
        "torrents/topPrio",
        mapOf("hashes" to hashes),
    )

    suspend fun minimizeTorrentPriority(hashes: String): Response<Unit> = post(
        "torrents/bottomPrio",
        mapOf("hashes" to hashes),
    )

    suspend fun createCategory(
        name: String,
        savePath: String,
        downloadPathEnabled: Boolean?,
        downloadPath: String,
    ): Response<Unit> = post(
        "torrents/createCategory",
        mapOf(
            "category" to name,
            "savePath" to savePath,
            "downloadPathEnabled" to downloadPathEnabled,
            "downloadPath" to downloadPath,
        ),
    )

    suspend fun editCategory(
        name: String,
        savePath: String,
        downloadPathEnabled: Boolean?,
        downloadPath: String,
    ): Response<Unit> = post(
        "torrents/editCategory",
        mapOf(
            "category" to name,
            "savePath" to savePath,
            "downloadPathEnabled" to downloadPathEnabled,
            "downloadPath" to downloadPath,
        ),
    )

    suspend fun createTags(names: String): Response<Unit> = post(
        "torrents/createTags",
        mapOf("tags" to names),
    )

    suspend fun setShareLimit(
        hashes: String,
        ratioLimit: Double,
        seedingTimeLimit: Int,
        inactiveSeedingTimeLimit: Int,
    ): Response<Unit> = post(
        "torrents/setShareLimits",
        mapOf(
            "hashes" to hashes,
            "ratioLimit" to ratioLimit,
            "seedingTimeLimit" to seedingTimeLimit,
            "inactiveSeedingTimeLimit" to inactiveSeedingTimeLimit,
        ),
    )

    suspend fun toggleSequentialDownload(hashes: String): Response<Unit> = post(
        "torrents/toggleSequentialDownload",
        mapOf("hashes" to hashes),
    )

    suspend fun togglePrioritizeFirstLastPiecesDownload(hashes: String): Response<Unit> = post(
        "torrents/toggleFirstLastPiecePrio",
        mapOf("hashes" to hashes),
    )

    suspend fun addTorrent(formData: MultiPartFormDataContent): Response<String> = post(
        "torrents/add",
        formData,
    )

    suspend fun setAutomaticTorrentManagement(hashes: String, enable: Boolean): Response<Unit> = post(
        "torrents/setAutoManagement",
        mapOf("hashes" to hashes, "enable" to enable),
    )

    suspend fun setDownloadSpeedLimit(hashes: String, limit: Int): Response<Unit> = post(
        "torrents/setDownloadLimit",
        mapOf("hashes" to hashes, "limit" to limit),
    )

    suspend fun setUploadSpeedLimit(hashes: String, limit: Int): Response<Unit> = post(
        "torrents/setUploadLimit",
        mapOf("hashes" to hashes, "limit" to limit),
    )

    suspend fun setForceStart(hashes: String, value: Boolean): Response<Unit> = post(
        "torrents/setForceStart",
        mapOf("hashes" to hashes, "value" to value),
    )

    suspend fun setSuperSeeding(hashes: String, value: Boolean): Response<Unit> = post(
        "torrents/setSuperSeeding",
        mapOf("hashes" to hashes, "value" to value),
    )

    suspend fun renameTorrent(hash: String, name: String): Response<Unit> = post(
        "torrents/rename",
        mapOf("hash" to hash, "name" to name),
    )

    suspend fun setLocation(hashes: String, location: String): Response<Unit> = post(
        "torrents/setLocation",
        mapOf("hashes" to hashes, "location" to location),
    )

    suspend fun setDownloadPath(hashes: String, path: String): Response<Unit> = post(
        "torrents/setDownloadPath",
        mapOf("id" to hashes, "path" to path),
    )

    suspend fun setFilePriority(hash: String, id: String, priority: Int): Response<Unit> = post(
        "torrents/filePrio",
        mapOf("hash" to hash, "id" to id, "priority" to priority),
    )

    suspend fun renameFile(hash: String, oldPath: String, newPath: String): Response<Unit> = post(
        "torrents/renameFile",
        mapOf("hash" to hash, "oldPath" to oldPath, "newPath" to newPath),
    )

    suspend fun renameFolder(hash: String, oldPath: String, newPath: String): Response<Unit> = post(
        "torrents/renameFolder",
        mapOf("hash" to hash, "oldPath" to oldPath, "newPath" to newPath),
    )

    suspend fun setCategory(hashes: String, category: String): Response<Unit> = post(
        "torrents/setCategory",
        mapOf("hashes" to hashes, "category" to category),
    )

    suspend fun addTags(hashes: String, tags: String): Response<Unit> = post(
        "torrents/addTags",
        mapOf("hashes" to hashes, "tags" to tags),
    )

    suspend fun removeTags(hashes: String, tags: String): Response<Unit> = post(
        "torrents/removeTags",
        mapOf("hashes" to hashes, "tags" to tags),
    )

    suspend fun exportTorrent(hash: String, block: suspend (ByteReadChannel) -> Unit): Response<Unit> = client.prepareGet {
        url.takeFrom(baseUrl).appendEncodedPathSegments("api/v2/torrents/export")
        url.parameters.append("hash", hash)
    }.execute { response ->
        withContext(Dispatchers.IO) {
            val channel = response.bodyAsChannel()
            block(channel)
            Response(response.status.value, Unit)
        }
    }

    suspend fun getPeers(hash: String): Response<TorrentPeers> = get(
        "sync/torrentPeers",
        mapOf("hash" to hash),
    )

    suspend fun addPeers(hashes: String, peers: String): Response<Unit> = post(
        "torrents/addPeers",
        mapOf("hashes" to hashes, "peers" to peers),
    )

    suspend fun banPeers(peers: String): Response<Unit> = post(
        "transfer/banPeers",
        mapOf("peers" to peers),
    )

    suspend fun getRssFeeds(): Response<RssFeedNode> = get(
        "rss/items",
    )

    suspend fun getRssFeedWithData(path: List<String>, uid: String?): Response<RssFeedWithData> = get(
        "rss/items",
        mapOf("withData" to true),
        RssFeedWithDataSerializer(path, uid),
    )

    suspend fun markAsRead(itemPath: String, articleId: String?): Response<Unit> = post(
        "rss/markAsRead",
        mapOf("itemPath" to itemPath, "articleId" to articleId),
    )

    suspend fun refreshItem(itemPath: String): Response<Unit> = post(
        "rss/refreshItem",
        mapOf("itemPath" to itemPath),
    )

    suspend fun addRssFeed(url: String, path: String): Response<Unit> = post(
        "rss/addFeed",
        mapOf("url" to url, "path" to path),
    )

    suspend fun setFeedUrl(path: String, url: String): Response<Unit> = post(
        "rss/setFeedURL",
        mapOf("path" to path, "url" to url),
    )

    suspend fun addRssFolder(path: String): Response<Unit> = post(
        "rss/addFolder",
        mapOf("path" to path),
    )

    suspend fun moveItem(from: String, to: String): Response<Unit> = post(
        "rss/moveItem",
        mapOf("itemPath" to from, "destPath" to to),
    )

    suspend fun removeItem(path: String): Response<Unit> = post(
        "rss/removeItem",
        mapOf("path" to path),
    )

    suspend fun getRssRules(): Response<Map<String, RssRule>> = get("rss/rules")

    suspend fun setRule(name: String, ruleDefinition: String): Response<Unit> = post(
        "rss/setRule",
        mapOf("ruleName" to name, "ruleDef" to ruleDefinition),
    )

    suspend fun renameRule(name: String, newName: String): Response<Unit> = post(
        "rss/renameRule",
        mapOf("ruleName" to name, "newRuleName" to newName),
    )

    suspend fun deleteRule(name: String): Response<Unit> = post(
        "rss/removeRule",
        mapOf("ruleName" to name),
    )

    suspend fun startSearch(pattern: String, category: String, plugins: String): Response<StartSearch> = post(
        "search/start",
        mapOf("pattern" to pattern, "category" to category, "plugins" to plugins),
    )

    suspend fun stopSearch(id: Int): Response<Unit> = post(
        "search/stop",
        mapOf("id" to id),
    )

    suspend fun deleteSearch(id: Int): Response<Unit> = post(
        "search/delete",
        mapOf("id" to id),
    )

    suspend fun getSearchResults(id: Int, offset: Int): Response<Search> = get(
        "search/results",
        mapOf("id" to id, "offset" to offset),
    )

    suspend fun getPlugins(): Response<List<Plugin>> = get("search/plugins")

    suspend fun enablePlugins(names: String, isEnabled: Boolean): Response<Unit> = post(
        "search/enablePlugin",
        mapOf("names" to names, "enable" to isEnabled),
    )

    suspend fun installPlugins(sources: String): Response<Unit> = post(
        "search/installPlugin",
        mapOf("sources" to sources),
    )

    suspend fun uninstallPlugins(names: String): Response<Unit> = post(
        "search/uninstallPlugin",
        mapOf("names" to names),
    )

    suspend fun updatePlugins(): Response<Unit> = post("search/updatePlugins")
}

expect suspend inline fun <reified T> decodeJson(
    channel: ByteReadChannel,
    deserializer: DeserializationStrategy<T> = serializer<T>(),
    json: Json = DefaultJson,
): T?
