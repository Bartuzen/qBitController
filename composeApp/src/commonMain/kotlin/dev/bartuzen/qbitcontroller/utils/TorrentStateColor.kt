package dev.bartuzen.qbitcontroller.utils

import androidx.compose.runtime.Composable
import dev.bartuzen.qbitcontroller.model.TorrentState
import dev.bartuzen.qbitcontroller.ui.theme.LocalCustomColors

@Composable
fun getTorrentStateColor(state: TorrentState) = when (state) {
    TorrentState.DOWNLOADING,
    TorrentState.FORCED_DL,
    TorrentState.META_DL,
    TorrentState.FORCED_META_DL,
    TorrentState.CHECKING_DL,
    TorrentState.CHECKING_UP,
    TorrentState.CHECKING_RESUME_DATA,
    TorrentState.MOVING,
    TorrentState.PAUSED_UP,
    -> {
        LocalCustomColors.current.torrentStateDownloading
    }
    TorrentState.STALLED_DL -> {
        LocalCustomColors.current.torrentStateStalledDownloading
    }
    TorrentState.STALLED_UP -> {
        LocalCustomColors.current.torrentStateStalledUploading
    }
    TorrentState.UPLOADING,
    TorrentState.FORCED_UP,
    -> {
        LocalCustomColors.current.torrentStateUploading
    }
    TorrentState.PAUSED_DL -> {
        LocalCustomColors.current.torrentStatePausedDownloading
    }
    /*TorrentState.PAUSED_UP -> {
        LocalCustomColors.current.torrentStatePausedUploading
    }*/
    TorrentState.QUEUED_DL,
    TorrentState.QUEUED_UP,
    -> {
        LocalCustomColors.current.torrentStateQueued
    }
    TorrentState.ERROR,
    TorrentState.MISSING_FILES,
    TorrentState.UNKNOWN,
    -> {
        LocalCustomColors.current.torrentStateError
    }
}
