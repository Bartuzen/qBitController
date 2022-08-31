package dev.bartuzen.qbitcontroller.network

import dev.bartuzen.qbitcontroller.data.TorrentSort
import dev.bartuzen.qbitcontroller.model.*
import retrofit2.Response
import retrofit2.http.*

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
    @POST("api/v2/torrents/pause")
    suspend fun pauseTorrent(@Field("hashes") hashes: String): Response<String>

    @FormUrlEncoded
    @POST("api/v2/torrents/resume")
    suspend fun resumeTorrent(@Field("hashes") hashes: String): Response<String>

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
}