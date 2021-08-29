package dev.bartuzen.qbitcontroller.network

import dev.bartuzen.qbitcontroller.model.Torrent
import dev.bartuzen.qbitcontroller.model.TorrentFile
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
    suspend fun getTorrentList(@Query("hashes") hash: String? = null): Response<List<Torrent>>

    @GET("api/v2/torrents/files")
    suspend fun getFileList(@Query("hash") hash: String): Response<List<TorrentFile>>
}