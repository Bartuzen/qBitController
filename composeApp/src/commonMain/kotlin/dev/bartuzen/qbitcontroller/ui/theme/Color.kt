package dev.bartuzen.qbitcontroller.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.materialkolor.ktx.harmonize

val defaultPrimaryColor = Color(0xFF415F91)

data class CustomColors(
    val torrentStateDownloading: Color = Color.Unspecified,
    val torrentStateStalledDownloading: Color = Color.Unspecified,
    val torrentStateStalledUploading: Color = Color.Unspecified,
    val torrentStateUploading: Color = Color.Unspecified,
    val torrentStatePausedDownloading: Color = Color.Unspecified,
    val torrentStatePausedUploading: Color = Color.Unspecified,
    val torrentStateQueued: Color = Color.Unspecified,
    val torrentStateError: Color = Color.Unspecified,
    val seederColor: Color = Color.Unspecified,
    val leecherColor: Color = Color.Unspecified,
    val logTimestamp: Color = Color.Unspecified,
    val logInfo: Color = Color.Unspecified,
    val logWarning: Color = Color.Unspecified,
    val logCritical: Color = Color.Unspecified,
    val filePriorityDoNotDownload: Color = Color.Unspecified,
    val filePriorityNormal: Color = Color.Unspecified,
    val filePriorityHigh: Color = Color.Unspecified,
    val filePriorityMaximum: Color = Color.Unspecified,
    val filePriorityMixed: Color = Color.Unspecified,
)

val LocalCustomColors = staticCompositionLocalOf { CustomColors() }

val lightCustomColors
    @Composable
    get() = CustomColors(
        torrentStateDownloading = harmonizeWithPrimary(Color(0xFF1A7F37)),
        torrentStateStalledDownloading = harmonizeWithPrimary(Color(0xFF2DA44E)),
        torrentStateStalledUploading = harmonizeWithPrimary(Color(0xFF0969DA)),
        torrentStateUploading = harmonizeWithPrimary(Color(0xFF7BB5F9)),
        torrentStatePausedDownloading = harmonizeWithPrimary(Color(0xFF57606A)),
        torrentStatePausedUploading = harmonizeWithPrimary(Color(0xFF8250DF)),
        torrentStateQueued = harmonizeWithPrimary(Color(0xFF7D4E00)),
        torrentStateError = harmonizeWithPrimary(Color(0xFFCF222E)),
        seederColor = harmonizeWithPrimary(Color(0xFF388E3C)),
        leecherColor = harmonizeWithPrimary(Color(0xFF1976D2)),
        logTimestamp = harmonizeWithPrimary(Color(0xFF6E7781)),
        logInfo = harmonizeWithPrimary(Color(0xFF0969DA)),
        logWarning = harmonizeWithPrimary(Color(0xFFBC4C00)),
        logCritical = harmonizeWithPrimary(Color(0xFFCF222E)),
        filePriorityDoNotDownload = harmonizeWithPrimary(Color(0xFF57606A)),
        filePriorityNormal = harmonizeWithPrimary(Color(0xFF2DA44E)),
        filePriorityHigh = harmonizeWithPrimary(Color(0xFFFF5722)),
        filePriorityMaximum = harmonizeWithPrimary(Color(0xFFCF222E)),
        filePriorityMixed = harmonizeWithPrimary(Color(0xFF03A9F4)),
    )

val darkCustomColors
    @Composable
    get() = CustomColors(
        torrentStateDownloading = harmonizeWithPrimary(Color(0xFF3FB950)),
        torrentStateStalledDownloading = harmonizeWithPrimary(Color(0xFF238636)),
        torrentStateStalledUploading = harmonizeWithPrimary(Color(0xFF1F6FEB)),
        torrentStateUploading = harmonizeWithPrimary(Color(0xFF58A6FF)),
        torrentStatePausedDownloading = harmonizeWithPrimary(Color(0xFF8B949E)),
        torrentStatePausedUploading = harmonizeWithPrimary(Color(0xFFA371F7)),
        torrentStateQueued = harmonizeWithPrimary(Color(0xFF845306)),
        torrentStateError = harmonizeWithPrimary(Color(0xFFF85149)),
        seederColor = harmonizeWithPrimary(Color(0xFF66BB6A)),
        leecherColor = harmonizeWithPrimary(Color(0xFF42A5F5)),
        logTimestamp = harmonizeWithPrimary(Color(0xFF6E7681)),
        logInfo = harmonizeWithPrimary(Color(0xFF58A6FF)),
        logWarning = harmonizeWithPrimary(Color(0xFFDB6D28)),
        logCritical = harmonizeWithPrimary(Color(0xFFF85149)),
        filePriorityDoNotDownload = harmonizeWithPrimary(Color(0xFF8B949E)),
        filePriorityNormal = harmonizeWithPrimary(Color(0xFF238636)),
        filePriorityHigh = harmonizeWithPrimary(Color(0xFFE17B5A)),
        filePriorityMaximum = harmonizeWithPrimary(Color(0xFFF85149)),
        filePriorityMixed = harmonizeWithPrimary(Color(0xFF03A9F4)),
    )

@Composable
private fun harmonizeWithPrimary(color: Color) = color.harmonize(MaterialTheme.colorScheme.primary)
