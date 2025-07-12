package dev.bartuzen.qbitcontroller.utils

import androidx.compose.runtime.Composable
import dev.bartuzen.qbitcontroller.model.TorrentFilePriority
import dev.bartuzen.qbitcontroller.model.TorrentState
import dev.bartuzen.qbitcontroller.network.RequestResult
import io.ktor.http.parseUrl
import qbitcontroller.composeapp.generated.resources.Res
import qbitcontroller.composeapp.generated.resources.error_api
import qbitcontroller.composeapp.generated.resources.error_banned
import qbitcontroller.composeapp.generated.resources.error_cannot_connect
import qbitcontroller.composeapp.generated.resources.error_invalid_credentials
import qbitcontroller.composeapp.generated.resources.error_no_data
import qbitcontroller.composeapp.generated.resources.error_no_internet
import qbitcontroller.composeapp.generated.resources.error_timeout
import qbitcontroller.composeapp.generated.resources.error_unknown
import qbitcontroller.composeapp.generated.resources.error_unknown_host
import qbitcontroller.composeapp.generated.resources.error_unknown_login_response
import qbitcontroller.composeapp.generated.resources.eta_days
import qbitcontroller.composeapp.generated.resources.eta_days_hours
import qbitcontroller.composeapp.generated.resources.eta_hours
import qbitcontroller.composeapp.generated.resources.eta_hours_minutes
import qbitcontroller.composeapp.generated.resources.eta_minutes
import qbitcontroller.composeapp.generated.resources.eta_minutes_seconds
import qbitcontroller.composeapp.generated.resources.eta_seconds
import qbitcontroller.composeapp.generated.resources.size_bytes
import qbitcontroller.composeapp.generated.resources.size_exbibytes
import qbitcontroller.composeapp.generated.resources.size_format
import qbitcontroller.composeapp.generated.resources.size_gibibytes
import qbitcontroller.composeapp.generated.resources.size_kibibytes
import qbitcontroller.composeapp.generated.resources.size_mebibytes
import qbitcontroller.composeapp.generated.resources.size_pebibytes
import qbitcontroller.composeapp.generated.resources.size_tebibytes
import qbitcontroller.composeapp.generated.resources.speed_bytes_per_second
import qbitcontroller.composeapp.generated.resources.speed_format
import qbitcontroller.composeapp.generated.resources.speed_gibibytes_per_second
import qbitcontroller.composeapp.generated.resources.speed_kibibytes_per_second
import qbitcontroller.composeapp.generated.resources.speed_mebibytes_per_second
import qbitcontroller.composeapp.generated.resources.torrent_files_priority_do_not_download
import qbitcontroller.composeapp.generated.resources.torrent_files_priority_high
import qbitcontroller.composeapp.generated.resources.torrent_files_priority_maximum
import qbitcontroller.composeapp.generated.resources.torrent_files_priority_normal
import qbitcontroller.composeapp.generated.resources.torrent_status_checking
import qbitcontroller.composeapp.generated.resources.torrent_status_checking_resume_data
import qbitcontroller.composeapp.generated.resources.torrent_status_completed
import qbitcontroller.composeapp.generated.resources.torrent_status_downloading
import qbitcontroller.composeapp.generated.resources.torrent_status_downloading_metadata
import qbitcontroller.composeapp.generated.resources.torrent_status_error
import qbitcontroller.composeapp.generated.resources.torrent_status_force_downloading
import qbitcontroller.composeapp.generated.resources.torrent_status_force_downloading_metadata
import qbitcontroller.composeapp.generated.resources.torrent_status_force_seeding
import qbitcontroller.composeapp.generated.resources.torrent_status_missing_files
import qbitcontroller.composeapp.generated.resources.torrent_status_moving
import qbitcontroller.composeapp.generated.resources.torrent_status_paused
import qbitcontroller.composeapp.generated.resources.torrent_status_queued
import qbitcontroller.composeapp.generated.resources.torrent_status_seeding
import qbitcontroller.composeapp.generated.resources.torrent_status_stalled
import qbitcontroller.composeapp.generated.resources.torrent_status_unknown
import kotlin.math.roundToLong
import kotlin.time.Instant

@Composable
fun formatBytes(byte: Long) = when (byte) {
    in 0 until 1024 -> {
        stringResource(Res.string.size_format, byte.toString(), stringResource(Res.string.size_bytes))
    }
    in 1024 until 1024 * 1024 -> {
        val text = (byte.toDouble() / 1024).floorToDecimal(1).toString()
        stringResource(Res.string.size_format, text, stringResource(Res.string.size_kibibytes))
    }
    in 1024 * 1024 until 1024 * 1024 * 1024 -> {
        val text = (byte.toDouble() / (1024 * 1024)).floorToDecimal(1).toString()
        stringResource(Res.string.size_format, text, stringResource(Res.string.size_mebibytes))
    }
    in 1024 * 1024 * 1024 until 1024L * 1024 * 1024 * 1024 -> {
        val text = (byte.toDouble() / (1024 * 1024 * 1024)).floorToDecimal(2).toString()
        stringResource(Res.string.size_format, text, stringResource(Res.string.size_gibibytes))
    }
    in 1024L * 1024 * 1024 * 1024 until 1024L * 1024 * 1024 * 1024 * 1024 -> {
        val text = (byte.toDouble() / (1024L * 1024 * 1024 * 1024)).floorToDecimal(3).toString()
        stringResource(Res.string.size_format, text, stringResource(Res.string.size_tebibytes))
    }
    in 1024L * 1024 * 1024 * 1024 * 1024 until 1024L * 1024 * 1024 * 1024 * 1024 * 1024 -> {
        val text = (byte.toDouble() / (1024L * 1024 * 1024 * 1024 * 1024)).floorToDecimal(3).toString()
        stringResource(Res.string.size_format, text, stringResource(Res.string.size_pebibytes))
    }
    else -> {
        val text = (byte.toDouble() / (1024L * 1024 * 1024 * 1024 * 1024 * 1024)).floorToDecimal(3).toString()
        stringResource(Res.string.size_format, text, stringResource(Res.string.size_exbibytes))
    }
}

@Composable
fun formatBytesPerSecond(byte: Long) = when (byte) {
    in 0 until 1024 -> {
        stringResource(Res.string.speed_format, byte.toString(), stringResource(Res.string.speed_bytes_per_second))
    }
    in 1024 until 1024 * 1024 -> {
        val text = (byte.toDouble() / 1024).floorToDecimal(1).toString()
        stringResource(Res.string.speed_format, text, stringResource(Res.string.speed_kibibytes_per_second))
    }
    in 1024 * 1024 until 1024 * 1024 * 1024 -> {
        val text = (byte.toDouble() / (1024 * 1024)).floorToDecimal(1).toString()
        stringResource(Res.string.speed_format, text, stringResource(Res.string.speed_mebibytes_per_second))
    }
    else -> {
        val text = (byte.toDouble() / (1024 * 1024 * 1024)).floorToDecimal(2).toString()
        stringResource(Res.string.speed_format, text, stringResource(Res.string.speed_gibibytes_per_second))
    }
}

@Composable
fun formatSeconds(seconds: Long) = when (seconds) {
    in 0 until 60 -> {
        stringResource(Res.string.eta_seconds, seconds.toString())
    }
    in 60 until 60 * 60 -> {
        val remainder = seconds % 60
        val minutes = (seconds / 60).toString()
        if (remainder != 0L) {
            stringResource(Res.string.eta_minutes_seconds, minutes, remainder.toString())
        } else {
            stringResource(Res.string.eta_minutes, minutes)
        }
    }
    in 60 * 60 until 60 * 60 * 60 -> {
        val remainder = ((seconds % (60 * 60)) / 60.0).roundToLong()
        val hours = (seconds / (60 * 60)).toString()
        if (remainder != 0L) {
            stringResource(Res.string.eta_hours_minutes, hours, remainder.toString())
        } else {
            stringResource(Res.string.eta_hours, hours)
        }
    }
    else -> {
        val remainder = ((seconds % (24 * 60 * 60)) / (60.0 * 60)).roundToLong()
        val days = (seconds / (24 * 60 * 60)).toString()
        if (remainder != 0L) {
            stringResource(Res.string.eta_days_hours, days, remainder.toString())
        } else {
            stringResource(Res.string.eta_days, days)
        }
    }
}

@Composable
fun formatSeconds(seconds: Int) = formatSeconds(seconds.toLong())

@Composable
fun formatTorrentState(state: TorrentState) = stringResource(
    when (state) {
        TorrentState.ERROR -> Res.string.torrent_status_error
        TorrentState.MISSING_FILES -> Res.string.torrent_status_missing_files
        TorrentState.UPLOADING, TorrentState.STALLED_UP -> Res.string.torrent_status_seeding
        TorrentState.PAUSED_UP -> Res.string.torrent_status_completed
        TorrentState.PAUSED_DL -> Res.string.torrent_status_paused
        TorrentState.QUEUED_UP, TorrentState.QUEUED_DL -> Res.string.torrent_status_queued
        TorrentState.STALLED_DL -> Res.string.torrent_status_stalled
        TorrentState.CHECKING_UP, TorrentState.CHECKING_DL -> Res.string.torrent_status_checking
        TorrentState.CHECKING_RESUME_DATA -> Res.string.torrent_status_checking_resume_data
        TorrentState.FORCED_UP -> Res.string.torrent_status_force_seeding
        TorrentState.DOWNLOADING -> Res.string.torrent_status_downloading
        TorrentState.META_DL -> Res.string.torrent_status_downloading_metadata
        TorrentState.FORCED_META_DL -> Res.string.torrent_status_force_downloading_metadata
        TorrentState.FORCED_DL -> Res.string.torrent_status_force_downloading
        TorrentState.MOVING -> Res.string.torrent_status_moving
        TorrentState.UNKNOWN -> Res.string.torrent_status_unknown
    },
)

@Composable
fun formatFilePriority(priority: TorrentFilePriority) = stringResource(
    when (priority) {
        TorrentFilePriority.DO_NOT_DOWNLOAD -> Res.string.torrent_files_priority_do_not_download
        TorrentFilePriority.NORMAL -> Res.string.torrent_files_priority_normal
        TorrentFilePriority.HIGH -> Res.string.torrent_files_priority_high
        TorrentFilePriority.MAXIMUM -> Res.string.torrent_files_priority_maximum
    },
)

suspend fun getErrorMessage(error: RequestResult.Error) = when (error) {
    RequestResult.Error.RequestError.InvalidCredentials -> getString(Res.string.error_invalid_credentials)
    RequestResult.Error.RequestError.Banned -> getString(Res.string.error_banned)
    RequestResult.Error.RequestError.CannotConnect -> getString(Res.string.error_cannot_connect)
    RequestResult.Error.RequestError.UnknownHost -> getString(Res.string.error_unknown_host)
    RequestResult.Error.RequestError.Timeout -> getString(Res.string.error_timeout)
    RequestResult.Error.RequestError.NoData -> getString(Res.string.error_no_data)
    RequestResult.Error.RequestError.NoInternet -> getString(Res.string.error_no_internet)
    is RequestResult.Error.RequestError.UnknownLoginResponse ->
        getString(Res.string.error_unknown_login_response, error.response.toString())
    is RequestResult.Error.RequestError.Unknown -> getString(Res.string.error_unknown, error.message)
    is RequestResult.Error.ApiError -> getString(Res.string.error_api, error.code)
}

expect fun Instant.formatDate(): String

@Composable
expect fun getCountryName(countryCode: String): String

private val ipPattern = Regex(
    """(?:[a-zA-Z]+://)?(?:((?:\d{1,3}\.){3}\d{1,3})|\[?((?:[A-Fa-f0-9]{4}:){7}[A-Fa-f0-9]{4})]?)(?::\d+)?(?:/.*)?""",
)

fun formatUri(uri: String): String {
    val ipAddressMatch = ipPattern.matchEntire(uri)
    if (ipAddressMatch != null) {
        val groups = ipAddressMatch.groups
        val ipAddress = (groups[1] ?: groups[2])?.value
        if (ipAddress != null) {
            return ipAddress
        }
    }

    val host = parseUrl(uri)?.host
    return host ?: uri
}

expect fun getDecimalSeparator(): Char
