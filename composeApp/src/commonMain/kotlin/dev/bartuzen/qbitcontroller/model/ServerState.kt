package dev.bartuzen.qbitcontroller.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ServerState(
    @SerialName("alltime_ul")
    val allTimeUpload: Long,

    @SerialName("alltime_dl")
    val allTimeDownload: Long,

    @SerialName("use_subcategories")
    val areSubcategoriesEnabled: Boolean = false,

    @SerialName("global_ratio")
    val globalRatio: String,

    @SerialName("total_wasted_session")
    val sessionWaste: Long,

    @SerialName("total_peer_connections")
    val connectedPeers: Long,

    @SerialName("read_cache_hits")
    val readCacheHits: String,

    @SerialName("total_buffers_size")
    val bufferSize: Long,

    @SerialName("write_cache_overload")
    val writeCacheOverload: String,

    @SerialName("read_cache_overload")
    val readCacheOverload: String,

    @SerialName("queued_io_jobs")
    val queuedIOJobs: Long,

    @SerialName("average_time_queue")
    val averageTimeInQueue: Long,

    @SerialName("total_queued_size")
    val queuedSize: Long,

    @SerialName("dl_info_data")
    val downloadSession: Long,

    @SerialName("dl_info_speed")
    val downloadSpeed: Long,

    @SerialName("dl_rate_limit")
    val downloadSpeedLimit: Int,

    @SerialName("up_info_data")
    val uploadSession: Long,

    @SerialName("up_info_speed")
    val uploadSpeed: Long,

    @SerialName("up_rate_limit")
    val uploadSpeedLimit: Int,

    @SerialName("use_alt_speed_limits")
    val useAlternativeSpeedLimits: Boolean,

    @SerialName("queueing")
    val isQueueingEnabled: Boolean,

    @SerialName("free_space_on_disk")
    val freeSpace: Long,
)
