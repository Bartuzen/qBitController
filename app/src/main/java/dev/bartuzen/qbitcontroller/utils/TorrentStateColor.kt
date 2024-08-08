package dev.bartuzen.qbitcontroller.utils

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import dev.bartuzen.qbitcontroller.R
import dev.bartuzen.qbitcontroller.model.TorrentState

fun getTorrentStateColor(context: Context, state: TorrentState) = context.getColorCompat(
    when (state) {
        TorrentState.DOWNLOADING,
        TorrentState.FORCED_DL,
        TorrentState.META_DL,
        TorrentState.FORCED_META_DL,
        TorrentState.CHECKING_DL,
        TorrentState.CHECKING_UP,
        TorrentState.CHECKING_RESUME_DATA,
        TorrentState.MOVING,
        -> {
            R.color.torrent_state_downloading
        }
        TorrentState.STALLED_DL -> {
            R.color.torrent_state_stalled_downloading
        }
        TorrentState.STALLED_UP -> {
            R.color.torrent_state_stalled_uploading
        }
        TorrentState.UPLOADING,
        TorrentState.FORCED_UP,
        -> {
            R.color.torrent_state_uploading
        }
        TorrentState.PAUSED_DL -> {
            R.color.torrent_state_paused_downloading
        }
        TorrentState.PAUSED_UP -> {
            R.color.torrent_state_paused_uploading
        }
        TorrentState.QUEUED_DL,
        TorrentState.QUEUED_UP,
        -> {
            R.color.torrent_state_queued
        }
        TorrentState.ERROR,
        TorrentState.MISSING_FILES,
        TorrentState.UNKNOWN,
        -> {
            R.color.torrent_state_error
        }
    },
)

@Composable
fun getTorrentStateColor(state: TorrentState) = Color(getTorrentStateColor(LocalContext.current, state))
