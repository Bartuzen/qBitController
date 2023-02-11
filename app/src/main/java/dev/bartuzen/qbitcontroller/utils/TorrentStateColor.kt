package dev.bartuzen.qbitcontroller.utils

import android.content.Context
import dev.bartuzen.qbitcontroller.R
import dev.bartuzen.qbitcontroller.model.TorrentState

fun getTorrentStateColor(context: Context, state: TorrentState) = context.getColorCompat(
    when (state) {
        TorrentState.DOWNLOADING,
        TorrentState.FORCED_DL,
        TorrentState.META_DL,
        TorrentState.CHECKING_DL,
        TorrentState.CHECKING_UP,
        TorrentState.CHECKING_RESUME_DATA,
        TorrentState.MOVING,
        TorrentState.ALLOCATING -> {
            R.color.torrent_state_1
        }
        TorrentState.STALLED_DL -> {
            R.color.torrent_state_2
        }
        TorrentState.STALLED_UP -> {
            R.color.torrent_state_3
        }
        TorrentState.UPLOADING,
        TorrentState.FORCED_UP -> {
            R.color.torrent_state_4
        }
        TorrentState.PAUSED_DL -> {
            R.color.torrent_state_5
        }
        TorrentState.PAUSED_UP -> {
            R.color.torrent_state_6
        }
        TorrentState.QUEUED_DL,
        TorrentState.QUEUED_UP -> {
            R.color.torrent_state_7
        }
        TorrentState.ERROR,
        TorrentState.MISSING_FILES,
        TorrentState.UNKNOWN -> {
            R.color.torrent_state_8
        }
    }
)
