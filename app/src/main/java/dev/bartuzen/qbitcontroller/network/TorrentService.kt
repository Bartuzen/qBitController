package dev.bartuzen.qbitcontroller.network

import dev.bartuzen.qbitcontroller.data.TorrentSort
import dev.bartuzen.qbitcontroller.model.Category
import dev.bartuzen.qbitcontroller.model.PieceState
import dev.bartuzen.qbitcontroller.model.Torrent
import dev.bartuzen.qbitcontroller.model.TorrentFile
import dev.bartuzen.qbitcontroller.model.TorrentProperties
import dev.bartuzen.qbitcontroller.model.TorrentTracker
import retrofit2.Response
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface TorrentService {
    @FormUrlEncoded
    @POST("api/v2/auth/login")
    suspend fun login(
        @Field("username") username: String,
        @Field("password") password: String
    ): Response<String>

    @GET("api/v2/torrents/info")
    suspend fun getTorrentList(
        @Query("hashes") hashes: String? = null,
        @Query("sort") torrentSort: TorrentSort? = null
    ): Response<List<Torrent>>

    @GET("api/v2/torrents/files")
    suspend fun getFiles(@Query("hash") hash: String): Response<List<TorrentFile>>

    @FormUrlEncoded
    @POST("api/v2/torrents/delete")
    suspend fun deleteTorrents(
        @Field("hashes") hashes: String,
        @Field("deleteFiles") deleteFiles: Boolean
    ): Response<Unit>

    @FormUrlEncoded
    @POST("api/v2/torrents/pause")
    suspend fun pauseTorrents(@Field("hashes") hashes: String): Response<String>

    @FormUrlEncoded
    @POST("api/v2/torrents/resume")
    suspend fun resumeTorrents(@Field("hashes") hashes: String): Response<String>

    @GET("api/v2/torrents/pieceStates")
    suspend fun getTorrentPieces(@Query("hash") hash: String): Response<List<PieceState>>

    @GET("api/v2/torrents/properties")
    suspend fun getTorrentProperties(@Query("hash") hash: String): Response<TorrentProperties>

    @GET("api/v2/torrents/trackers")
    suspend fun getTorrentTrackers(@Query("hash") hash: String): Response<List<TorrentTracker>>

    @FormUrlEncoded
    @POST("api/v2/torrents/addTrackers")
    suspend fun addTorrentTrackers(
        @Field("hash") hash: String,
        @Field("urls") urls: String
    ): Response<Unit>

    @FormUrlEncoded
    @POST("api/v2/torrents/removeTrackers")
    suspend fun deleteTorrentTrackers(
        @Field("hash") hash: String,
        @Field("urls") urls: String
    ): Response<Unit>

    @GET("api/v2/torrents/categories")
    suspend fun getCategories(): Response<Map<String, Category>>

    @GET("api/v2/torrents/tags")
    suspend fun getTags(): Response<List<String>>

    @FormUrlEncoded
    @POST("api/v2/torrents/removeCategories")
    suspend fun deleteCategories(@Field("categories") categories: String): Response<Unit>

    @FormUrlEncoded
    @POST("api/v2/torrents/deleteTags")
    suspend fun deleteTags(@Field("tags") tags: String): Response<Unit>

    @FormUrlEncoded
    @POST("api/v2/torrents/increasePrio")
    suspend fun increaseTorrentPriority(@Field("hashes") hashes: String): Response<Unit>

    @FormUrlEncoded
    @POST("api/v2/torrents/decreasePrio")
    suspend fun decreaseTorrentPriority(@Field("hashes") hashes: String): Response<Unit>

    @FormUrlEncoded
    @POST("api/v2/torrents/topPrio")
    suspend fun maximizeTorrentPriority(@Field("hashes") hashes: String): Response<Unit>

    @FormUrlEncoded
    @POST("api/v2/torrents/bottomPrio")
    suspend fun minimizeTorrentPriority(@Field("hashes") hashes: String): Response<Unit>

    @FormUrlEncoded
    @POST("api/v2/torrents/createCategory")
    suspend fun createCategory(
        @Field("category") name: String,
        @Field("savePath") savePath: String
    ): Response<Unit>

    @FormUrlEncoded
    @POST("api/v2/torrents/createTags")
    suspend fun createTags(@Field("tags") names: String): Response<Unit>

    @FormUrlEncoded
    @POST("api/v2/torrents/toggleSequentialDownload")
    suspend fun toggleSequentialDownload(@Field("hashes") hashes: String): Response<Unit>

    @FormUrlEncoded
    @POST("api/v2/torrents/add")
    suspend fun addTorrent(
        @Field("urls") links: String,
        @Field("dlLimit") downloadSpeedLimit: Int?,
        @Field("upLimit") uploadSpeedLimit: Int?,
        @Field("ratioLimit") ratioLimit: Double?,
        @Field("paused") isPaused: Boolean,
        @Field("skip_checking") skipHashChecking: Boolean,
        @Field("autoTMM") isAutoTorrentManagementEnabled: Boolean,
        @Field("sequentialDownload") isSequentialDownloadEnabled: Boolean,
        @Field("firstLastPiecePrio") isFirstLastPiecePrioritized: Boolean
    ): Response<Unit>
}
