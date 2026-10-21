package dev.bartuzen.qbitcontroller.ui.settings.serversettings

import dev.bartuzen.qbitcontroller.model.ServerPreferences
import org.jetbrains.compose.resources.StringResource
import qbitcontroller.composeapp.generated.resources.Res
import qbitcontroller.composeapp.generated.resources.server_settings_advanced_async_io_threads
import qbitcontroller.composeapp.generated.resources.server_settings_advanced_bdecode_depth_limit
import qbitcontroller.composeapp.generated.resources.server_settings_advanced_bdecode_token_limit
import qbitcontroller.composeapp.generated.resources.server_settings_advanced_checking_memory_use
import qbitcontroller.composeapp.generated.resources.server_settings_advanced_disk_cache
import qbitcontroller.composeapp.generated.resources.server_settings_advanced_disk_cache_ttl
import qbitcontroller.composeapp.generated.resources.server_settings_advanced_file_pool_size
import qbitcontroller.composeapp.generated.resources.server_settings_advanced_hashing_threads
import qbitcontroller.composeapp.generated.resources.server_settings_bittorrent_max_active_checking_torrents
import qbitcontroller.composeapp.generated.resources.server_settings_bittorrent_merge_trackers
import qbitcontroller.composeapp.generated.resources.server_settings_bittorrent_merge_trackers_desc
import qbitcontroller.composeapp.generated.resources.server_settings_bittorrent_recheck_completed_torrents
import qbitcontroller.composeapp.generated.resources.server_settings_bittorrent_save_resume_data_interval
import qbitcontroller.composeapp.generated.resources.server_settings_bittorrent_torrent_file_size_limit
import qbitcontroller.composeapp.generated.resources.server_settings_category_advanced
import qbitcontroller.composeapp.generated.resources.server_settings_category_bittorrent
import qbitcontroller.composeapp.generated.resources.server_settings_category_connection
import qbitcontroller.composeapp.generated.resources.server_settings_category_downloads
import qbitcontroller.composeapp.generated.resources.server_settings_category_general
import qbitcontroller.composeapp.generated.resources.server_settings_category_speed
import qbitcontroller.composeapp.generated.resources.server_settings_category_webui
import qbitcontroller.composeapp.generated.resources.server_settings_connection_anonymous_mode
import qbitcontroller.composeapp.generated.resources.server_settings_connection_dht
import qbitcontroller.composeapp.generated.resources.server_settings_connection_encryption
import qbitcontroller.composeapp.generated.resources.server_settings_connection_encryption_disable
import qbitcontroller.composeapp.generated.resources.server_settings_connection_encryption_prefer
import qbitcontroller.composeapp.generated.resources.server_settings_connection_encryption_require
import qbitcontroller.composeapp.generated.resources.server_settings_connection_listen_port
import qbitcontroller.composeapp.generated.resources.server_settings_connection_lsd
import qbitcontroller.composeapp.generated.resources.server_settings_connection_max_announce_tiers
import qbitcontroller.composeapp.generated.resources.server_settings_connection_max_connec
import qbitcontroller.composeapp.generated.resources.server_settings_connection_max_connec_per_torrent
import qbitcontroller.composeapp.generated.resources.server_settings_connection_max_uploads
import qbitcontroller.composeapp.generated.resources.server_settings_connection_max_uploads_per_torrent
import qbitcontroller.composeapp.generated.resources.server_settings_connection_pex
import qbitcontroller.composeapp.generated.resources.server_settings_connection_random_port
import qbitcontroller.composeapp.generated.resources.server_settings_connection_upnp
import qbitcontroller.composeapp.generated.resources.server_settings_downloads_dont_count_slow_torrents
import qbitcontroller.composeapp.generated.resources.server_settings_downloads_dont_count_slow_torrents_desc
import qbitcontroller.composeapp.generated.resources.server_settings_downloads_max_active_downloads
import qbitcontroller.composeapp.generated.resources.server_settings_downloads_max_active_torrents
import qbitcontroller.composeapp.generated.resources.server_settings_downloads_max_active_uploads
import qbitcontroller.composeapp.generated.resources.server_settings_downloads_queueing_enabled
import qbitcontroller.composeapp.generated.resources.server_settings_downloads_slow_torrent_dl_rate_threshold
import qbitcontroller.composeapp.generated.resources.server_settings_downloads_slow_torrent_inactive_timer
import qbitcontroller.composeapp.generated.resources.server_settings_downloads_slow_torrent_ul_rate_threshold
import qbitcontroller.composeapp.generated.resources.server_settings_general_incomplete_files_ext
import qbitcontroller.composeapp.generated.resources.server_settings_general_preallocate_all
import qbitcontroller.composeapp.generated.resources.server_settings_general_save_path
import qbitcontroller.composeapp.generated.resources.server_settings_general_start_paused_enabled
import qbitcontroller.composeapp.generated.resources.server_settings_general_temp_path
import qbitcontroller.composeapp.generated.resources.server_settings_general_temp_path_desc
import qbitcontroller.composeapp.generated.resources.server_settings_general_temp_path_enabled
import qbitcontroller.composeapp.generated.resources.server_settings_general_torrent_content_layout
import qbitcontroller.composeapp.generated.resources.server_settings_general_torrent_content_layout_no_subfolder
import qbitcontroller.composeapp.generated.resources.server_settings_general_torrent_content_layout_original
import qbitcontroller.composeapp.generated.resources.server_settings_general_torrent_content_layout_subfolder
import qbitcontroller.composeapp.generated.resources.server_settings_general_use_subcategories
import qbitcontroller.composeapp.generated.resources.server_settings_speed_alt_dl_limit
import qbitcontroller.composeapp.generated.resources.server_settings_speed_alt_up_limit
import qbitcontroller.composeapp.generated.resources.server_settings_speed_dl_limit
import qbitcontroller.composeapp.generated.resources.server_settings_speed_dl_limit_desc
import qbitcontroller.composeapp.generated.resources.server_settings_speed_schedule_from_hour
import qbitcontroller.composeapp.generated.resources.server_settings_speed_schedule_from_min
import qbitcontroller.composeapp.generated.resources.server_settings_speed_schedule_to_hour
import qbitcontroller.composeapp.generated.resources.server_settings_speed_schedule_to_min
import qbitcontroller.composeapp.generated.resources.server_settings_speed_scheduler_enabled
import qbitcontroller.composeapp.generated.resources.server_settings_speed_up_limit
import qbitcontroller.composeapp.generated.resources.server_settings_speed_up_limit_desc
import qbitcontroller.composeapp.generated.resources.server_settings_webui_address
import qbitcontroller.composeapp.generated.resources.server_settings_webui_clickjacking_protection
import qbitcontroller.composeapp.generated.resources.server_settings_webui_csrf_protection
import qbitcontroller.composeapp.generated.resources.server_settings_webui_host_header_validation
import qbitcontroller.composeapp.generated.resources.server_settings_webui_port
import qbitcontroller.composeapp.generated.resources.server_settings_webui_secure_cookie
import qbitcontroller.composeapp.generated.resources.server_settings_webui_upnp
import qbitcontroller.composeapp.generated.resources.server_settings_webui_use_https

sealed interface ServerPreferenceItem {
    val title: StringResource
    val description: StringResource?
}

data class SwitchItem(
    override val title: StringResource,
    override val description: StringResource? = null,
    val value: (ServerPreferences) -> Boolean?,
    val onChange: (ServerPreferences, Boolean) -> ServerPreferences,
) : ServerPreferenceItem

enum class NumberFormat {
    Plain,
    Speed,
    Bytes,
    Seconds,
    MiB,
}

data class NumberItem(
    override val title: StringResource,
    override val description: StringResource? = null,
    val format: NumberFormat = NumberFormat.Plain,
    val value: (ServerPreferences) -> Long?,
    val onChange: (ServerPreferences, Long) -> ServerPreferences,
) : ServerPreferenceItem

data class TextItem(
    override val title: StringResource,
    override val description: StringResource? = null,
    val value: (ServerPreferences) -> String?,
    val onChange: (ServerPreferences, String) -> ServerPreferences,
) : ServerPreferenceItem

data class ChoiceOption(
    val value: Any,
    val label: StringResource,
)

data class ChoiceItem(
    override val title: StringResource,
    override val description: StringResource? = null,
    val options: List<ChoiceOption>,
    val value: (ServerPreferences) -> Any?,
    val onChange: (ServerPreferences, Any) -> ServerPreferences,
) : ServerPreferenceItem

data class PreferenceGroup(
    val title: StringResource,
    val items: List<ServerPreferenceItem>,
)

val serverPreferenceGroups = listOf(
    PreferenceGroup(
        title = Res.string.server_settings_category_general,
        items = listOf(
            TextItem(
                title = Res.string.server_settings_general_save_path,
                value = { it.savePath },
                onChange = { prefs, value -> prefs.copy(savePath = value) },
            ),
            SwitchItem(
                title = Res.string.server_settings_general_temp_path_enabled,
                value = { it.tempPathEnabled },
                onChange = { prefs, value -> prefs.copy(tempPathEnabled = value) },
            ),
            TextItem(
                title = Res.string.server_settings_general_temp_path,
                description = Res.string.server_settings_general_temp_path_desc,
                value = { it.tempPath },
                onChange = { prefs, value -> prefs.copy(tempPath = value) },
            ),
            SwitchItem(
                title = Res.string.server_settings_general_start_paused_enabled,
                value = { it.startPausedEnabled },
                onChange = { prefs, value -> prefs.copy(startPausedEnabled = value) },
            ),
            SwitchItem(
                title = Res.string.server_settings_general_preallocate_all,
                value = { it.preallocateAll },
                onChange = { prefs, value -> prefs.copy(preallocateAll = value) },
            ),
            SwitchItem(
                title = Res.string.server_settings_general_incomplete_files_ext,
                value = { it.incompleteFilesExt },
                onChange = { prefs, value -> prefs.copy(incompleteFilesExt = value) },
            ),
            ChoiceItem(
                title = Res.string.server_settings_general_torrent_content_layout,
                value = { it.torrentContentLayout },
                onChange = { prefs, value -> prefs.copy(torrentContentLayout = value as String) },
                options = listOf(
                    ChoiceOption("Original", Res.string.server_settings_general_torrent_content_layout_original),
                    ChoiceOption("Subfolder", Res.string.server_settings_general_torrent_content_layout_subfolder),
                    ChoiceOption("NoSubfolder", Res.string.server_settings_general_torrent_content_layout_no_subfolder),
                ),
            ),
            SwitchItem(
                title = Res.string.server_settings_general_use_subcategories,
                value = { it.useSubcategories },
                onChange = { prefs, value -> prefs.copy(useSubcategories = value) },
            ),
        ),
    ),
    PreferenceGroup(
        title = Res.string.server_settings_category_downloads,
        items = listOf(
            SwitchItem(
                title = Res.string.server_settings_downloads_queueing_enabled,
                value = { it.queueingEnabled },
                onChange = { prefs, value -> prefs.copy(queueingEnabled = value) },
            ),
            NumberItem(
                title = Res.string.server_settings_downloads_max_active_downloads,
                value = { it.maxActiveDownloads },
                onChange = { prefs, value -> prefs.copy(maxActiveDownloads = value) },
            ),
            NumberItem(
                title = Res.string.server_settings_downloads_max_active_uploads,
                value = { it.maxActiveUploads },
                onChange = { prefs, value -> prefs.copy(maxActiveUploads = value) },
            ),
            NumberItem(
                title = Res.string.server_settings_downloads_max_active_torrents,
                value = { it.maxActiveTorrents },
                onChange = { prefs, value -> prefs.copy(maxActiveTorrents = value) },
            ),
            SwitchItem(
                title = Res.string.server_settings_downloads_dont_count_slow_torrents,
                description = Res.string.server_settings_downloads_dont_count_slow_torrents_desc,
                value = { it.dontCountSlowTorrents },
                onChange = { prefs, value -> prefs.copy(dontCountSlowTorrents = value) },
            ),
            NumberItem(
                title = Res.string.server_settings_downloads_slow_torrent_dl_rate_threshold,
                format = NumberFormat.Speed,
                value = { it.slowTorrentDlRateThreshold },
                onChange = { prefs, value -> prefs.copy(slowTorrentDlRateThreshold = value) },
            ),
            NumberItem(
                title = Res.string.server_settings_downloads_slow_torrent_ul_rate_threshold,
                format = NumberFormat.Speed,
                value = { it.slowTorrentUlRateThreshold },
                onChange = { prefs, value -> prefs.copy(slowTorrentUlRateThreshold = value) },
            ),
            NumberItem(
                title = Res.string.server_settings_downloads_slow_torrent_inactive_timer,
                format = NumberFormat.Seconds,
                value = { it.slowTorrentInactiveTimer },
                onChange = { prefs, value -> prefs.copy(slowTorrentInactiveTimer = value) },
            ),
        ),
    ),
    PreferenceGroup(
        title = Res.string.server_settings_category_connection,
        items = listOf(
            NumberItem(
                title = Res.string.server_settings_connection_listen_port,
                value = { it.listenPort },
                onChange = { prefs, value -> prefs.copy(listenPort = value) },
            ),
            SwitchItem(
                title = Res.string.server_settings_connection_random_port,
                value = { it.randomPort },
                onChange = { prefs, value -> prefs.copy(randomPort = value) },
            ),
            SwitchItem(
                title = Res.string.server_settings_connection_upnp,
                value = { it.upnp },
                onChange = { prefs, value -> prefs.copy(upnp = value) },
            ),
            NumberItem(
                title = Res.string.server_settings_connection_max_connec,
                value = { it.maxConnec },
                onChange = { prefs, value -> prefs.copy(maxConnec = value) },
            ),
            NumberItem(
                title = Res.string.server_settings_connection_max_connec_per_torrent,
                value = { it.maxConnecPerTorrent },
                onChange = { prefs, value -> prefs.copy(maxConnecPerTorrent = value) },
            ),
            NumberItem(
                title = Res.string.server_settings_connection_max_uploads,
                value = { it.maxUploads },
                onChange = { prefs, value -> prefs.copy(maxUploads = value) },
            ),
            NumberItem(
                title = Res.string.server_settings_connection_max_uploads_per_torrent,
                value = { it.maxUploadsPerTorrent },
                onChange = { prefs, value -> prefs.copy(maxUploadsPerTorrent = value) },
            ),
            SwitchItem(
                title = Res.string.server_settings_connection_dht,
                value = { it.dht },
                onChange = { prefs, value -> prefs.copy(dht = value) },
            ),
            SwitchItem(
                title = Res.string.server_settings_connection_pex,
                value = { it.pex },
                onChange = { prefs, value -> prefs.copy(pex = value) },
            ),
            SwitchItem(
                title = Res.string.server_settings_connection_lsd,
                value = { it.lsd },
                onChange = { prefs, value -> prefs.copy(lsd = value) },
            ),
            ChoiceItem(
                title = Res.string.server_settings_connection_encryption,
                value = { it.encryption },
                onChange = { prefs, value -> prefs.copy(encryption = value as Int) },
                options = listOf(
                    ChoiceOption(0, Res.string.server_settings_connection_encryption_prefer),
                    ChoiceOption(1, Res.string.server_settings_connection_encryption_require),
                    ChoiceOption(2, Res.string.server_settings_connection_encryption_disable),
                ),
            ),
            SwitchItem(
                title = Res.string.server_settings_connection_anonymous_mode,
                value = { it.anonymousMode },
                onChange = { prefs, value -> prefs.copy(anonymousMode = value) },
            ),
            NumberItem(
                title = Res.string.server_settings_connection_max_announce_tiers,
                value = { it.maxAnnounceTiers },
                onChange = { prefs, value -> prefs.copy(maxAnnounceTiers = value) },
            ),
        ),
    ),
    PreferenceGroup(
        title = Res.string.server_settings_category_speed,
        items = listOf(
            NumberItem(
                title = Res.string.server_settings_speed_dl_limit,
                description = Res.string.server_settings_speed_dl_limit_desc,
                format = NumberFormat.Speed,
                value = { it.dlLimit },
                onChange = { prefs, value -> prefs.copy(dlLimit = value) },
            ),
            NumberItem(
                title = Res.string.server_settings_speed_up_limit,
                description = Res.string.server_settings_speed_up_limit_desc,
                format = NumberFormat.Speed,
                value = { it.upLimit },
                onChange = { prefs, value -> prefs.copy(upLimit = value) },
            ),
            NumberItem(
                title = Res.string.server_settings_speed_alt_dl_limit,
                format = NumberFormat.Speed,
                value = { it.altDlLimit },
                onChange = { prefs, value -> prefs.copy(altDlLimit = value) },
            ),
            NumberItem(
                title = Res.string.server_settings_speed_alt_up_limit,
                format = NumberFormat.Speed,
                value = { it.altUpLimit },
                onChange = { prefs, value -> prefs.copy(altUpLimit = value) },
            ),
            SwitchItem(
                title = Res.string.server_settings_speed_scheduler_enabled,
                value = { it.schedulerEnabled },
                onChange = { prefs, value -> prefs.copy(schedulerEnabled = value) },
            ),
            NumberItem(
                title = Res.string.server_settings_speed_schedule_from_hour,
                value = { it.scheduleFromHour },
                onChange = { prefs, value -> prefs.copy(scheduleFromHour = value) },
            ),
            NumberItem(
                title = Res.string.server_settings_speed_schedule_from_min,
                value = { it.scheduleFromMin },
                onChange = { prefs, value -> prefs.copy(scheduleFromMin = value) },
            ),
            NumberItem(
                title = Res.string.server_settings_speed_schedule_to_hour,
                value = { it.scheduleToHour },
                onChange = { prefs, value -> prefs.copy(scheduleToHour = value) },
            ),
            NumberItem(
                title = Res.string.server_settings_speed_schedule_to_min,
                value = { it.scheduleToMin },
                onChange = { prefs, value -> prefs.copy(scheduleToMin = value) },
            ),
        ),
    ),
    PreferenceGroup(
        title = Res.string.server_settings_category_bittorrent,
        items = listOf(
            NumberItem(
                title = Res.string.server_settings_bittorrent_max_active_checking_torrents,
                value = { it.maxActiveCheckingTorrents },
                onChange = { prefs, value -> prefs.copy(maxActiveCheckingTorrents = value) },
            ),
            NumberItem(
                title = Res.string.server_settings_bittorrent_torrent_file_size_limit,
                format = NumberFormat.Bytes,
                value = { it.torrentFileSizeLimit },
                onChange = { prefs, value -> prefs.copy(torrentFileSizeLimit = value) },
            ),
            NumberItem(
                title = Res.string.server_settings_bittorrent_save_resume_data_interval,
                format = NumberFormat.Seconds,
                value = { it.saveResumeDataInterval },
                onChange = { prefs, value -> prefs.copy(saveResumeDataInterval = value) },
            ),
            SwitchItem(
                title = Res.string.server_settings_bittorrent_recheck_completed_torrents,
                value = { it.recheckCompletedTorrents },
                onChange = { prefs, value -> prefs.copy(recheckCompletedTorrents = value) },
            ),
            SwitchItem(
                title = Res.string.server_settings_bittorrent_merge_trackers,
                description = Res.string.server_settings_bittorrent_merge_trackers_desc,
                value = { it.mergeTrackers },
                onChange = { prefs, value -> prefs.copy(mergeTrackers = value) },
            ),
        ),
    ),
    PreferenceGroup(
        title = Res.string.server_settings_category_webui,
        items = listOf(
            TextItem(
                title = Res.string.server_settings_webui_address,
                value = { it.webUiAddress },
                onChange = { prefs, value -> prefs.copy(webUiAddress = value) },
            ),
            NumberItem(
                title = Res.string.server_settings_webui_port,
                value = { it.webUiPort },
                onChange = { prefs, value -> prefs.copy(webUiPort = value) },
            ),
            SwitchItem(
                title = Res.string.server_settings_webui_upnp,
                value = { it.webUiUpnp },
                onChange = { prefs, value -> prefs.copy(webUiUpnp = value) },
            ),
            SwitchItem(
                title = Res.string.server_settings_webui_use_https,
                value = { it.useHttps },
                onChange = { prefs, value -> prefs.copy(useHttps = value) },
            ),
            SwitchItem(
                title = Res.string.server_settings_webui_csrf_protection,
                value = { it.webUiCsrfProtectionEnabled },
                onChange = { prefs, value -> prefs.copy(webUiCsrfProtectionEnabled = value) },
            ),
            SwitchItem(
                title = Res.string.server_settings_webui_clickjacking_protection,
                value = { it.webUiClickjackingProtectionEnabled },
                onChange = { prefs, value -> prefs.copy(webUiClickjackingProtectionEnabled = value) },
            ),
            SwitchItem(
                title = Res.string.server_settings_webui_secure_cookie,
                value = { it.webUiSecureCookieEnabled },
                onChange = { prefs, value -> prefs.copy(webUiSecureCookieEnabled = value) },
            ),
            SwitchItem(
                title = Res.string.server_settings_webui_host_header_validation,
                value = { it.webUiHostHeaderValidationEnabled },
                onChange = { prefs, value -> prefs.copy(webUiHostHeaderValidationEnabled = value) },
            ),
        ),
    ),
    PreferenceGroup(
        title = Res.string.server_settings_category_advanced,
        items = listOf(
            NumberItem(
                title = Res.string.server_settings_advanced_disk_cache,
                format = NumberFormat.MiB,
                value = { it.diskCache },
                onChange = { prefs, value -> prefs.copy(diskCache = value) },
            ),
            NumberItem(
                title = Res.string.server_settings_advanced_disk_cache_ttl,
                format = NumberFormat.Seconds,
                value = { it.diskCacheTtl },
                onChange = { prefs, value -> prefs.copy(diskCacheTtl = value) },
            ),
            NumberItem(
                title = Res.string.server_settings_advanced_async_io_threads,
                value = { it.asyncIoThreads },
                onChange = { prefs, value -> prefs.copy(asyncIoThreads = value) },
            ),
            NumberItem(
                title = Res.string.server_settings_advanced_hashing_threads,
                value = { it.hashingThreads },
                onChange = { prefs, value -> prefs.copy(hashingThreads = value) },
            ),
            NumberItem(
                title = Res.string.server_settings_advanced_file_pool_size,
                value = { it.filePoolSize },
                onChange = { prefs, value -> prefs.copy(filePoolSize = value) },
            ),
            NumberItem(
                title = Res.string.server_settings_advanced_checking_memory_use,
                format = NumberFormat.MiB,
                value = { it.checkingMemoryUse },
                onChange = { prefs, value -> prefs.copy(checkingMemoryUse = value) },
            ),
            NumberItem(
                title = Res.string.server_settings_advanced_bdecode_depth_limit,
                value = { it.bdecodeDepthLimit },
                onChange = { prefs, value -> prefs.copy(bdecodeDepthLimit = value) },
            ),
            NumberItem(
                title = Res.string.server_settings_advanced_bdecode_token_limit,
                value = { it.bdecodeTokenLimit },
                onChange = { prefs, value -> prefs.copy(bdecodeTokenLimit = value) },
            ),
        ),
    ),
)
