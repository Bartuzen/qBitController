package dev.bartuzen.qbitcontroller.utils

import android.content.Context
import dev.bartuzen.qbitcontroller.R
import dev.bartuzen.qbitcontroller.model.TorrentFilePriority
import dev.bartuzen.qbitcontroller.model.TorrentState
import dev.bartuzen.qbitcontroller.network.RequestResult
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.math.roundToLong

fun formatBytes(context: Context, byte: Long) = when (byte) {
    in 0 until 1024 -> context.getString(R.string.size_bytes, byte.toString())
    in 1024 until 1024 * 1024 -> {
        val text = (byte.toDouble() / 1024).floorToDecimal(1).toString()
        context.getString(R.string.size_kibibytes, text)
    }
    in 1024 * 1024 until 1024 * 1024 * 1024 -> {
        val text = (byte.toDouble() / (1024 * 1024)).floorToDecimal(1).toString()
        context.getString(R.string.size_mebibytes, text)
    }
    in 1024 * 1024 * 1024 until 1024L * 1024 * 1024 * 1024 -> {
        val text = (byte.toDouble() / (1024 * 1024 * 1024)).floorToDecimal(2).toString()
        context.getString(R.string.size_gibibytes, text)
    }
    in 1024L * 1024 * 1024 * 1024 until 1024L * 1024 * 1024 * 1024 * 1024 -> {
        val text = (byte.toDouble() / (1024L * 1024 * 1024 * 1024)).floorToDecimal(3).toString()
        context.getString(R.string.size_tebibytes, text)
    }
    in 1024L * 1024 * 1024 * 1024 * 1024 until 1024L * 1024 * 1024 * 1024 * 1024 * 1024 -> {
        val text = (byte.toDouble() / (1024L * 1024 * 1024 * 1024 * 1024)).floorToDecimal(3).toString()
        context.getString(R.string.size_pebibytes, text)
    }
    else -> {
        val text = (byte.toDouble() / (1024L * 1024 * 1024 * 1024 * 1024 * 1024)).floorToDecimal(3).toString()
        context.getString(R.string.size_exbibytes, text)
    }
}

fun formatBytesPerSecond(context: Context, byte: Long) = when (byte) {
    in 0 until 1024 -> context.getString(R.string.speed_bytes_per_second, byte.toString())
    in 1024 until 1024 * 1024 -> {
        val text = (byte.toDouble() / 1024).floorToDecimal(1).toString()
        context.getString(R.string.speed_kibibytes_per_second, text)
    }
    in 1024 * 1024 until 1024 * 1024 * 1024 -> {
        val text = (byte.toDouble() / (1024 * 1024)).floorToDecimal(1).toString()
        context.getString(R.string.speed_mebibytes_per_second, text)
    }
    else -> {
        val text = (byte.toDouble() / (1024 * 1024 * 1024)).floorToDecimal(2).toString()
        context.getString(R.string.speed_gibibytes_per_second, text)
    }
}

fun formatSeconds(context: Context, seconds: Long) = when (seconds) {
    in 0 until 60 -> {
        context.getString(R.string.eta_seconds, seconds.toString())
    }
    in 60 until 60 * 60 -> {
        val remainder = seconds % 60
        val minutes = (seconds / 60).toString()
        if (remainder != 0L) {
            context.getString(R.string.eta_minutes_seconds, minutes, remainder.toString())
        } else {
            context.getString(R.string.eta_minutes, minutes)
        }
    }
    in 60 * 60 until 60 * 60 * 60 -> {
        val remainder = ((seconds % (60 * 60)) / 60.0).roundToLong()
        val hours = (seconds / (60 * 60)).toString()
        if (remainder != 0L) {
            context.getString(R.string.eta_hours_minutes, hours, remainder.toString())
        } else {
            context.getString(R.string.eta_hours, hours)
        }
    }
    else -> {
        val remainder = ((seconds % (24 * 60 * 60)) / (60.0 * 60)).roundToLong()
        val days = (seconds / (24 * 60 * 60)).toString()
        if (remainder != 0L) {
            context.getString(R.string.eta_days_hours, days, remainder.toString())
        } else {
            context.getString(R.string.eta_days, days)
        }
    }
}

fun formatSeconds(context: Context, seconds: Int) = formatSeconds(context, seconds.toLong())

fun formatTorrentState(context: Context, state: TorrentState) = context.getString(
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
        TorrentState.ALLOCATING -> R.string.torrent_status_allocating_space
        TorrentState.DOWNLOADING -> R.string.torrent_status_downloading
        TorrentState.META_DL -> R.string.torrent_status_downloading_metadata
        TorrentState.FORCED_META_DL -> R.string.torrent_status_force_downloading_metadata
        TorrentState.FORCED_DL -> R.string.torrent_status_force_downloading
        TorrentState.MOVING -> R.string.torrent_status_moving
        TorrentState.UNKNOWN -> R.string.torrent_status_unknown
    }
)

fun formatFilePriority(context: Context, priority: TorrentFilePriority) = context.getString(
    when (priority) {
        TorrentFilePriority.DO_NOT_DOWNLOAD -> R.string.torrent_files_priority_do_not_download
        TorrentFilePriority.NORMAL -> R.string.torrent_files_priority_normal
        TorrentFilePriority.HIGH -> R.string.torrent_files_priority_high
        TorrentFilePriority.MAXIMUM -> R.string.torrent_files_priority_maximum
    }
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

fun formatDate(epochSecond: Long): String = Instant.ofEpochSecond(epochSecond)
    .atZone(ZoneId.systemDefault())
    .format(
        DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
            .withZone(ZoneId.systemDefault())
    )
