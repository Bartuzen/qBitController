package dev.bartuzen.qbitcontroller.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Preferences stored on the qBittorrent server (`/api/v2/app/preferences`).
 *
 * Every property is nullable with a `null` default. qBittorrent omits properties that are not supported by the
 * running version, so a `null` value can be used to detect whether a setting is available and hide it in the UI.
 */
@Serializable
data class ServerPreferences(
    // General
    @SerialName("save_path") val savePath: String? = null,
    @SerialName("temp_path_enabled") val tempPathEnabled: Boolean? = null,
    @SerialName("temp_path") val tempPath: String? = null,
    @SerialName("start_paused_enabled") val startPausedEnabled: Boolean? = null,
    @SerialName("preallocate_all") val preallocateAll: Boolean? = null,
    @SerialName("incomplete_files_ext") val incompleteFilesExt: Boolean? = null,
    @SerialName("torrent_content_layout") val torrentContentLayout: String? = null,
    @SerialName("use_subcategories") val useSubcategories: Boolean? = null,

    // Downloads
    @SerialName("queueing_enabled") val queueingEnabled: Boolean? = null,
    @SerialName("max_active_downloads") val maxActiveDownloads: Long? = null,
    @SerialName("max_active_uploads") val maxActiveUploads: Long? = null,
    @SerialName("max_active_torrents") val maxActiveTorrents: Long? = null,
    @SerialName("dont_count_slow_torrents") val dontCountSlowTorrents: Boolean? = null,
    @SerialName("slow_torrent_dl_rate_threshold") val slowTorrentDlRateThreshold: Long? = null,
    @SerialName("slow_torrent_ul_rate_threshold") val slowTorrentUlRateThreshold: Long? = null,
    @SerialName("slow_torrent_inactive_timer") val slowTorrentInactiveTimer: Long? = null,

    // Connection
    @SerialName("listen_port") val listenPort: Long? = null,
    @SerialName("random_port") val randomPort: Boolean? = null,
    @SerialName("upnp") val upnp: Boolean? = null,
    @SerialName("max_connec") val maxConnec: Long? = null,
    @SerialName("max_connec_per_torrent") val maxConnecPerTorrent: Long? = null,
    @SerialName("max_uploads") val maxUploads: Long? = null,
    @SerialName("max_uploads_per_torrent") val maxUploadsPerTorrent: Long? = null,
    @SerialName("dht") val dht: Boolean? = null,
    @SerialName("pex") val pex: Boolean? = null,
    @SerialName("lsd") val lsd: Boolean? = null,
    @SerialName("encryption") val encryption: Int? = null,
    @SerialName("anonymous_mode") val anonymousMode: Boolean? = null,
    @SerialName("max_announce_tiers") val maxAnnounceTiers: Long? = null,

    // Speed
    @SerialName("dl_limit") val dlLimit: Long? = null,
    @SerialName("up_limit") val upLimit: Long? = null,
    @SerialName("alt_dl_limit") val altDlLimit: Long? = null,
    @SerialName("alt_up_limit") val altUpLimit: Long? = null,
    @SerialName("scheduler_enabled") val schedulerEnabled: Boolean? = null,
    @SerialName("schedule_from_hour") val scheduleFromHour: Long? = null,
    @SerialName("schedule_from_min") val scheduleFromMin: Long? = null,
    @SerialName("schedule_to_hour") val scheduleToHour: Long? = null,
    @SerialName("schedule_to_min") val scheduleToMin: Long? = null,

    // BitTorrent
    @SerialName("max_active_checking_torrents") val maxActiveCheckingTorrents: Long? = null,
    @SerialName("torrent_file_size_limit") val torrentFileSizeLimit: Long? = null,
    @SerialName("save_resume_data_interval") val saveResumeDataInterval: Long? = null,
    @SerialName("recheck_completed_torrents") val recheckCompletedTorrents: Boolean? = null,
    @SerialName("merge_trackers") val mergeTrackers: Boolean? = null,

    // Share limits (global)
    @SerialName("max_ratio_enabled") val maxRatioEnabled: Boolean? = null,
    @SerialName("max_ratio") val maxRatio: Double? = null,
    @SerialName("max_seeding_time_enabled") val maxSeedingTimeEnabled: Boolean? = null,
    @SerialName("max_seeding_time") val maxSeedingTime: Long? = null,
    @SerialName("max_inactive_seeding_time_enabled") val maxInactiveSeedingTimeEnabled: Boolean? = null,
    @SerialName("max_inactive_seeding_time") val maxInactiveSeedingTime: Long? = null,
    @SerialName("share_limits_mode") val shareLimitsMode: String? = null,
    @SerialName("max_ratio_act") val maxRatioAct: Long? = null,

    // Web UI
    @SerialName("web_ui_address") val webUiAddress: String? = null,
    @SerialName("web_ui_port") val webUiPort: Long? = null,
    @SerialName("web_ui_upnp") val webUiUpnp: Boolean? = null,
    @SerialName("use_https") val useHttps: Boolean? = null,
    @SerialName("web_ui_csrf_protection_enabled") val webUiCsrfProtectionEnabled: Boolean? = null,
    @SerialName("web_ui_clickjacking_protection_enabled") val webUiClickjackingProtectionEnabled: Boolean? = null,
    @SerialName("web_ui_secure_cookie_enabled") val webUiSecureCookieEnabled: Boolean? = null,
    @SerialName("web_ui_host_header_validation_enabled") val webUiHostHeaderValidationEnabled: Boolean? = null,

    // Advanced
    @SerialName("disk_cache") val diskCache: Long? = null,
    @SerialName("disk_cache_ttl") val diskCacheTtl: Long? = null,
    @SerialName("async_io_threads") val asyncIoThreads: Long? = null,
    @SerialName("hashing_threads") val hashingThreads: Long? = null,
    @SerialName("file_pool_size") val filePoolSize: Long? = null,
    @SerialName("checking_memory_use") val checkingMemoryUse: Long? = null,
    @SerialName("bdecode_depth_limit") val bdecodeDepthLimit: Long? = null,
    @SerialName("bdecode_token_limit") val bdecodeTokenLimit: Long? = null,
    @SerialName("disk_io_type") val diskIoType: Long? = null,
    @SerialName("disk_io_read_mode") val diskIoReadMode: Long? = null,
    @SerialName("disk_io_write_mode") val diskIoWriteMode: Long? = null,
)
