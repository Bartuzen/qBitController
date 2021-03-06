package dev.bartuzen.qbitcontroller.utils

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalContext
import dev.bartuzen.qbitcontroller.R
import dev.bartuzen.qbitcontroller.model.TorrentState
import dev.bartuzen.qbitcontroller.network.RequestResult
import kotlin.math.roundToLong

fun Context.formatByte(byte: Long) = when (byte) {
    in 0 until 1024 -> getString(R.string.size_byte, byte.toString())
    in 1024 until 1024 * 1024 -> {
        val text = (byte.toDouble() / 1024).floorToDecimal(1).toString()
        getString(R.string.size_kibibyte, text)
    }
    in 1024 * 1024 until 1024 * 1024 * 1024 -> {
        val text = (byte.toDouble() / (1024 * 1024)).floorToDecimal(1).toString()
        getString(R.string.size_mebibyte, text)
    }
    else -> {
        val text = (byte.toDouble() / (1024 * 1024 * 1024)).floorToDecimal(2).toString()
        getString(R.string.size_gibibyte, text)
    }
}

fun Context.formatBytePerSecond(byte: Long) = when (byte) {
    in 0 until 1024 -> getString(R.string.speed_byte_per_second, byte.toString())
    in 1024 until 1024 * 1024 -> {
        val text = (byte.toDouble() / 1024).floorToDecimal(1).toString()
        getString(R.string.speed_kibibyte_per_second, text)
    }
    in 1024 * 1024 until 1024 * 1024 * 1024 -> {
        val text = (byte.toDouble() / (1024 * 1024)).floorToDecimal(1).toString()
        getString(R.string.speed_mebibyte_per_second, text)
    }
    else -> {
        val text = (byte.toDouble() / (1024 * 1024 * 1024)).floorToDecimal(2).toString()
        getString(R.string.speed_gibibyte_per_second, text)
    }
}

fun Context.formatTime(seconds: Long) = when (seconds) {
    in 0 until 60 -> getString(R.string.eta_seconds, seconds.toString())
    in 60 until 60 * 60 -> {
        val remainder = seconds % 60
        val minutes = (seconds / 60).toString()
        if (remainder != 0L) {
            getString(R.string.eta_minutes_seconds, minutes, remainder.toString())
        } else {
            getString(R.string.eta_minutes, minutes)
        }
    }
    in 60 * 60 until 60 * 60 * 60 -> {
        val remainder = ((seconds % (60 * 60)) / 60.0).roundToLong()
        val hours = (seconds / (60 * 60)).toString()
        if (remainder != 0L) {
            getString(R.string.eta_hours_minutes, hours, remainder.toString())
        } else {
            getString(R.string.eta_hours, hours)
        }
    }
    in 60 * 60 * 60 until 8640000 -> {
        val remainder = ((seconds % (24 * 60 * 60)) / (60.0 * 60)).roundToLong()
        val days = (seconds / (24 * 60 * 60)).toString()
        if (remainder != 0L) {
            getString(R.string.eta_days_hours, days, remainder.toString())
        } else {
            getString(R.string.eta_days, days)
        }
    }
    else -> "inf"
}

fun Context.formatState(state: TorrentState?) = getString(
    when (state) {
        TorrentState.ERROR -> R.string.torrent_status_error
        TorrentState.MISSING_FILES -> R.string.torrent_status_missing_files
        TorrentState.UPLOADING -> R.string.torrent_status_seeding
        TorrentState.PAUSED_UP, TorrentState.PAUSED_DL -> R.string.torrent_status_paused
        TorrentState.QUEUED_UP, TorrentState.QUEUED_DL -> R.string.torrent_status_queued
        TorrentState.STALLED_UP, TorrentState.STALLED_DL -> R.string.torrent_status_stalled
        TorrentState.CHECKING_UP, TorrentState.CHECKING_DL, TorrentState.CHECKING_RESUME_DATA -> R.string.torrent_status_checking
        TorrentState.FORCED_UP -> R.string.torrent_status_force_seeding
        TorrentState.ALLOCATING -> R.string.torrent_status_allocating_space
        TorrentState.DOWNLOADING -> R.string.torrent_status_downloading
        TorrentState.META_DL -> R.string.torrent_status_downloading_metadata
        TorrentState.FORCED_DL -> R.string.torrent_status_force_downloading
        TorrentState.MOVING -> R.string.torrent_status_moving
        else -> R.string.torrent_status_unknown
    }
)

fun Context.getErrorMessage(error: RequestResult) = when (error) {
    RequestResult.SUCCESS -> ""
    RequestResult.INVALID_CREDENTIALS -> getString(R.string.error_invalid_credentials)
    RequestResult.BANNED -> getString(R.string.error_banned)
    RequestResult.CANNOT_CONNECT -> getString(R.string.error_cannot_connect)
    RequestResult.UNKNOWN_HOST -> getString(R.string.error_unknown_host)
    RequestResult.TIMEOUT -> getString(R.string.error_timeout)
    RequestResult.UNKNOWN -> getString(R.string.error_unknown)
}


@Composable
@ReadOnlyComposable
fun formatByte(byte: Long) = LocalContext.current.formatByte(byte)

@Composable
@ReadOnlyComposable
fun formatBytePerSecond(byte: Long) = LocalContext.current.formatBytePerSecond(byte)

@Composable
@ReadOnlyComposable
fun formatTime(seconds: Long) = LocalContext.current.formatTime(seconds)

@Composable
@ReadOnlyComposable
fun formatState(state: TorrentState?) = LocalContext.current.formatState(state)

@Composable
@ReadOnlyComposable
fun getErrorMessage(error: RequestResult) = LocalContext.current.getErrorMessage(error)