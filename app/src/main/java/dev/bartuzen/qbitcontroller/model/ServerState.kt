package dev.bartuzen.qbitcontroller.model

import com.fasterxml.jackson.annotation.JsonProperty

data class ServerState(
    @JsonProperty("alltime_ul")
    val allTimeUpload: Long,

    @JsonProperty("alltime_dl")
    val allTimeDownload: Long,

    @JsonProperty("use_subcategories")
    val areSubcategoriesEnabled: Boolean,

    @JsonProperty("global_ratio")
    val globalRatio: String,

    @JsonProperty("total_wasted_session")
    val sessionWaste: Long,

    @JsonProperty("total_peer_connections")
    val connectedPeers: Long,

    @JsonProperty("total_buffers_size")
    val bufferSize: Long,

    @JsonProperty("write_cache_overload")
    val writeCacheOverload: String,

    @JsonProperty("read_cache_overload")
    val readCacheOverload: String,

    @JsonProperty("queued_io_jobs")
    val queuedIOJobs: String,

    @JsonProperty("average_time_queue")
    val averageTimeInQueue: Long,

    @JsonProperty("total_queued_size")
    val queuedSize: Long,

    @JsonProperty("dl_info_data")
    val downloadSession: Long,

    @JsonProperty("dl_info_speed")
    val downloadSpeed: Long,

    @JsonProperty("dl_rate_limit")
    val downloadSpeedLimit: Int,

    @JsonProperty("up_info_data")
    val uploadSession: Long,

    @JsonProperty("up_info_speed")
    val uploadSpeed: Long,

    @JsonProperty("up_rate_limit")
    val uploadSpeedLimit: Int,

    @JsonProperty("use_alt_speed_limits")
    val useAlternativeSpeedLimits: Boolean,

    @JsonProperty("queueing")
    val isQueueingEnabled: Boolean,

    @JsonProperty("free_space_on_disk")
    val freeSpace: Long
)
