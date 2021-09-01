package dev.bartuzen.qbitcontroller.network

import dev.bartuzen.qbitcontroller.model.PieceState
import dev.bartuzen.qbitcontroller.model.Torrent
import dev.bartuzen.qbitcontroller.model.TorrentFile
import dev.bartuzen.qbitcontroller.model.TorrentProperties
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface TorrentService {
    @GET("api/v2/auth/login")
    suspend fun login(
        @Query("username") username: String,
        @Query("password") password: String
    ): Response<String>

    @GET("api/v2/torrents/info")
    suspend fun getTorrentList(@Query("hashes") hashes: String? = null): Response<List<Torrent>>

    @GET("api/v2/torrents/files")
    suspend fun getFiles(@Query("hash") hash: String): Response<List<TorrentFile>>

    @GET("api/v2/torrents/pause")
    suspend fun pauseTorrent(@Query("hashes") hashes: String): Response<String>

    @GET("api/v2/torrents/resume")
    suspend fun resumeTorrent(@Query("hashes") hashes: String): Response<String>

    @GET("api/v2/torrents/pieceStates")
    suspend fun getTorrentPieces(@Query("hash") hash: String): Response<List<PieceState>>


    @GET("api/v2/torrents/properties")
    suspend fun getTorrentProperties(@Query("hash") hash: String): Response<TorrentProperties>
}