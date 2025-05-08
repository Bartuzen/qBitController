package dev.bartuzen.qbitcontroller.utils

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import dev.bartuzen.qbitcontroller.R
import dev.bartuzen.qbitcontroller.model.TorrentFilePriority
import dev.bartuzen.qbitcontroller.model.TorrentState
import dev.bartuzen.qbitcontroller.network.RequestResult
import okhttp3.internal.publicsuffix.PublicSuffixDatabase
import java.net.URI
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlin.math.roundToLong

@Composable
fun formatBytes(byte: Long) = when (byte) {
    in 0 until 1024 -> {
        stringResource(R.string.size_format, byte.toString(), stringResource(R.string.size_bytes))
    }
    in 1024 until 1024 * 1024 -> {
        val text = (byte.toDouble() / 1024).floorToDecimal(1).toString()
        stringResource(R.string.size_format, text, stringResource(R.string.size_kibibytes))
    }
    in 1024 * 1024 until 1024 * 1024 * 1024 -> {
        val text = (byte.toDouble() / (1024 * 1024)).floorToDecimal(1).toString()
        stringResource(R.string.size_format, text, stringResource(R.string.size_mebibytes))
    }
    in 1024 * 1024 * 1024 until 1024L * 1024 * 1024 * 1024 -> {
        val text = (byte.toDouble() / (1024 * 1024 * 1024)).floorToDecimal(2).toString()
        stringResource(R.string.size_format, text, stringResource(R.string.size_gibibytes))
    }
    in 1024L * 1024 * 1024 * 1024 until 1024L * 1024 * 1024 * 1024 * 1024 -> {
        val text = (byte.toDouble() / (1024L * 1024 * 1024 * 1024)).floorToDecimal(3).toString()
        stringResource(R.string.size_format, text, stringResource(R.string.size_tebibytes))
    }
    in 1024L * 1024 * 1024 * 1024 * 1024 until 1024L * 1024 * 1024 * 1024 * 1024 * 1024 -> {
        val text = (byte.toDouble() / (1024L * 1024 * 1024 * 1024 * 1024)).floorToDecimal(3).toString()
        stringResource(R.string.size_format, text, stringResource(R.string.size_pebibytes))
    }
    else -> {
        val text = (byte.toDouble() / (1024L * 1024 * 1024 * 1024 * 1024 * 1024)).floorToDecimal(3).toString()
        stringResource(R.string.size_format, text, stringResource(R.string.size_exbibytes))
    }
}

@Composable
fun formatBytesPerSecond(byte: Long) = when (byte) {
    in 0 until 1024 -> {
        stringResource(R.string.speed_format, byte.toString(), stringResource(R.string.speed_bytes_per_second))
    }
    in 1024 until 1024 * 1024 -> {
        val text = (byte.toDouble() / 1024).floorToDecimal(1).toString()
        stringResource(R.string.speed_format, text, stringResource(R.string.speed_kibibytes_per_second))
    }
    in 1024 * 1024 until 1024 * 1024 * 1024 -> {
        val text = (byte.toDouble() / (1024 * 1024)).floorToDecimal(1).toString()
        stringResource(R.string.speed_format, text, stringResource(R.string.speed_mebibytes_per_second))
    }
    else -> {
        val text = (byte.toDouble() / (1024 * 1024 * 1024)).floorToDecimal(2).toString()
        stringResource(R.string.speed_format, text, stringResource(R.string.speed_gibibytes_per_second))
    }
}

@Composable
fun formatSeconds(seconds: Long) = when (seconds) {
    in 0 until 60 -> {
        stringResource(R.string.eta_seconds, seconds.toString())
    }
    in 60 until 60 * 60 -> {
        val remainder = seconds % 60
        val minutes = (seconds / 60).toString()
        if (remainder != 0L) {
            stringResource(R.string.eta_minutes_seconds, minutes, remainder.toString())
        } else {
            stringResource(R.string.eta_minutes, minutes)
        }
    }
    in 60 * 60 until 60 * 60 * 60 -> {
        val remainder = ((seconds % (60 * 60)) / 60.0).roundToLong()
        val hours = (seconds / (60 * 60)).toString()
        if (remainder != 0L) {
            stringResource(R.string.eta_hours_minutes, hours, remainder.toString())
        } else {
            stringResource(R.string.eta_hours, hours)
        }
    }
    else -> {
        val remainder = ((seconds % (24 * 60 * 60)) / (60.0 * 60)).roundToLong()
        val days = (seconds / (24 * 60 * 60)).toString()
        if (remainder != 0L) {
            stringResource(R.string.eta_days_hours, days, remainder.toString())
        } else {
            stringResource(R.string.eta_days, days)
        }
    }
}

@Composable fun formatSeconds(seconds: Int) = formatSeconds(seconds.toLong())

@Composable
fun formatTorrentState(state: TorrentState) = stringResource(
    when (state) {
        TorrentState.ERROR -> R.string.torrent_status_error
        TorrentState.MISSING_FILES -> R.string.torrent_status_missing_files
        TorrentState.UPLOADING, TorrentState.STALLED_UP -> R.string.torrent_status_seeding
        TorrentState.PAUSED_UP -> R.string.torrent_status_completed
        TorrentState.PAUSED_DL -> R.string.torrent_status_paused
        TorrentState.QUEUED_UP, TorrentState.QUEUED_DL -> R.string.torrent_status_queued
        TorrentState.STALLED_DL -> R.string.torrent_status_stalled
        TorrentState.CHECKING_UP, TorrentState.CHECKING_DL -> R.string.torrent_status_checking
        TorrentState.CHECKING_RESUME_DATA -> R.string.torrent_status_checking_resume_data
        TorrentState.FORCED_UP -> R.string.torrent_status_force_seeding
        TorrentState.DOWNLOADING -> R.string.torrent_status_downloading
        TorrentState.META_DL -> R.string.torrent_status_downloading_metadata
        TorrentState.FORCED_META_DL -> R.string.torrent_status_force_downloading_metadata
        TorrentState.FORCED_DL -> R.string.torrent_status_force_downloading
        TorrentState.MOVING -> R.string.torrent_status_moving
        TorrentState.UNKNOWN -> R.string.torrent_status_unknown
    },
)

@Composable
fun formatFilePriority(priority: TorrentFilePriority) = stringResource(
    when (priority) {
        TorrentFilePriority.DO_NOT_DOWNLOAD -> R.string.torrent_files_priority_do_not_download
        TorrentFilePriority.NORMAL -> R.string.torrent_files_priority_normal
        TorrentFilePriority.HIGH -> R.string.torrent_files_priority_high
        TorrentFilePriority.MAXIMUM -> R.string.torrent_files_priority_maximum
    },
)

fun getErrorMessage(context: Context, error: RequestResult.Error) = when (error) {
    RequestResult.Error.RequestError.InvalidCredentials -> context.getString(R.string.error_invalid_credentials)
    RequestResult.Error.RequestError.Banned -> context.getString(R.string.error_banned)
    RequestResult.Error.RequestError.CannotConnect -> context.getString(R.string.error_cannot_connect)
    RequestResult.Error.RequestError.UnknownHost -> context.getString(R.string.error_unknown_host)
    RequestResult.Error.RequestError.Timeout -> context.getString(R.string.error_timeout)
    RequestResult.Error.RequestError.NoData -> context.getString(R.string.error_no_data)
    is RequestResult.Error.RequestError.UnknownLoginResponse ->
        context.getString(R.string.error_unknown_login_response, error.response)
    is RequestResult.Error.RequestError.Unknown -> context.getString(R.string.error_unknown, error.message)
    is RequestResult.Error.ApiError -> context.getString(R.string.error_api, error.code)
}

fun formatDate(epochSecondOrMilli: Long): String = if (epochSecondOrMilli >= 100000000000) {
    Instant.ofEpochMilli(epochSecondOrMilli)
} else {
    Instant.ofEpochSecond(epochSecondOrMilli)
}.atZone(ZoneId.systemDefault())
    .format(
        DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
            .withZone(ZoneId.systemDefault()),
    )

@Composable
fun getCountryName(countryCode: String): String {
    val (language, country) = stringResource(R.string.language_code)
        .replace("-", "_")
        .split("_")
        .let { it.first() to it.getOrNull(1) }

    val localeForLanguage = if (country == null) Locale(language) else Locale(language, country)

    return Locale("", countryCode).getDisplayCountry(
        localeForLanguage,
    )
}

private val ipPattern = Regex(
    """(?:[a-zA-Z]+://)?(?:((?:\d{1,3}\.){3}\d{1,3})|\[?((?:[A-Fa-f0-9]{4}:){7}[A-Fa-f0-9]{4})]?)(?::\d+)?(?:/.*)?""",
)

fun formatUri(uri: String) = try {
    val ipAddressMatch = ipPattern.matchEntire(uri)
    if (ipAddressMatch != null) {
        val groups = ipAddressMatch.groups
        (groups[1] ?: groups[2])?.value
    } else {
        val host = uri.toUri().host ?: URI.create(uri).host ?: throw IllegalArgumentException()
        PublicSuffixDatabase.get().getEffectiveTldPlusOne(host)
    }
} catch (_: IllegalArgumentException) {
    null
} ?: uri
