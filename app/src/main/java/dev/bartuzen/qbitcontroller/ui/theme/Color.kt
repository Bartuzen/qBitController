package dev.bartuzen.qbitcontroller.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val primaryLight = Color(0xFF415F91)
val onPrimaryLight = Color(0xFFFFFFFF)
val primaryContainerLight = Color(0xFFD6E3FF)
val onPrimaryContainerLight = Color(0xFF001B3E)
val secondaryLight = Color(0xFF565F71)
val onSecondaryLight = Color(0xFFFFFFFF)
val secondaryContainerLight = Color(0xFFDAE2F9)
val onSecondaryContainerLight = Color(0xFF131C2B)
val tertiaryLight = Color(0xFF705575)
val onTertiaryLight = Color(0xFFFFFFFF)
val tertiaryContainerLight = Color(0xFFFAD8FD)
val onTertiaryContainerLight = Color(0xFF28132E)
val errorLight = Color(0xFFBA1A1A)
val onErrorLight = Color(0xFFFFFFFF)
val errorContainerLight = Color(0xFFFFDAD6)
val onErrorContainerLight = Color(0xFF410002)
val backgroundLight = Color(0xFFF9F9FF)
val onBackgroundLight = Color(0xFF191C20)
val surfaceLight = Color(0xFFF9F9FF)
val onSurfaceLight = Color(0xFF191C20)
val surfaceVariantLight = Color(0xFFE0E2EC)
val onSurfaceVariantLight = Color(0xFF44474E)
val outlineLight = Color(0xFF74777F)
val outlineVariantLight = Color(0xFFC4C6D0)
val scrimLight = Color(0xFF000000)
val inverseSurfaceLight = Color(0xFF2E3036)
val inverseOnSurfaceLight = Color(0xFFF0F0F7)
val inversePrimaryLight = Color(0xFFAAC7FF)
val surfaceDimLight = Color(0xFFD9D9E0)
val surfaceBrightLight = Color(0xFFF9F9FF)
val surfaceContainerLowestLight = Color(0xFFFFFFFF)
val surfaceContainerLowLight = Color(0xFFF3F3FA)
val surfaceContainerLight = Color(0xFFEDEDF4)
val surfaceContainerHighLight = Color(0xFFE7E8EE)
val surfaceContainerHighestLight = Color(0xFFE2E2E9)

val primaryDark = Color(0xFFAAC7FF)
val onPrimaryDark = Color(0xFF0A305F)
val primaryContainerDark = Color(0xFF284777)
val onPrimaryContainerDark = Color(0xFFD6E3FF)
val secondaryDark = Color(0xFFBEC6DC)
val onSecondaryDark = Color(0xFF283141)
val secondaryContainerDark = Color(0xFF3E4759)
val onSecondaryContainerDark = Color(0xFFDAE2F9)
val tertiaryDark = Color(0xFFDDBCE0)
val onTertiaryDark = Color(0xFF3F2844)
val tertiaryContainerDark = Color(0xFF573E5C)
val onTertiaryContainerDark = Color(0xFFFAD8FD)
val errorDark = Color(0xFFFFB4AB)
val onErrorDark = Color(0xFF690005)
val errorContainerDark = Color(0xFF93000A)
val onErrorContainerDark = Color(0xFFFFDAD6)
val backgroundDark = Color(0xFF111318)
val onBackgroundDark = Color(0xFFE2E2E9)
val surfaceDark = Color(0xFF111318)
val onSurfaceDark = Color(0xFFE2E2E9)
val surfaceVariantDark = Color(0xFF44474E)
val onSurfaceVariantDark = Color(0xFFC4C6D0)
val outlineDark = Color(0xFF8E9099)
val outlineVariantDark = Color(0xFF44474E)
val scrimDark = Color(0xFF000000)
val inverseSurfaceDark = Color(0xFFE2E2E9)
val inverseOnSurfaceDark = Color(0xFF2E3036)
val inversePrimaryDark = Color(0xFF415F91)
val surfaceDimDark = Color(0xFF111318)
val surfaceBrightDark = Color(0xFF37393E)
val surfaceContainerLowestDark = Color(0xFF0C0E13)
val surfaceContainerLowDark = Color(0xFF191C20)
val surfaceContainerDark = Color(0xFF1D2024)
val surfaceContainerHighDark = Color(0xFF282A2F)
val surfaceContainerHighestDark = Color(0xFF33353A)

val torrentStateDownloadingLight = Color(0xFF1A7F37)
val torrentStateStalledDownloadingLight = Color(0xFF2DA44E)
val torrentStateStalledUploadingLight = Color(0xFF0969DA)
val torrentStateUploadingLight = Color(0xFF7BB5F9)
val torrentStatePausedDownloadingLight = Color(0xFF57606A)
val torrentStatePausedUploadingLight = Color(0xFF8250DF)
val torrentStateQueuedLight = Color(0xFF7D4E00)
val torrentStateErrorLight = errorLight

val seederColorLight = Color(0xFF388E3C)
val leecherColorLight = Color(0xFF1976D2)

val logTimestampLight = Color(0xFF6E7781)
val logInfoLight = Color(0xFF0969DA)
val logWarningLight = Color(0xFFBC4C00)
val logCriticalLight = Color(0xFFCF222E)

val filePriorityDoNotDownloadLight = Color(0xFF57606A)
val filePriorityNormalLight = Color(0xFF2DA44E)
val filePriorityHighLight = Color(0xFFFF5722)
val filePriorityMaximumLight = Color(0xFFCF222E)
val filePriorityMixedLight = Color(0xFF03A9F4)

val torrentStateDownloadingDark = Color(0xFF3FB950)
val torrentStateStalledDownloadingDark = Color(0xFF238636)
val torrentStateStalledUploadingDark = Color(0xFF1F6FEB)
val torrentStateUploadingDark = Color(0xFF58A6FF)
val torrentStatePausedDownloadingDark = Color(0xFF8B949E)
val torrentStatePausedUploadingDark = Color(0xFFA371F7)
val torrentStateQueuedDark = Color(0xFF845306)
val torrentStateErrorDark = errorDark

val seederColorDark = Color(0xFF66BB6A)
val leecherColorDark = Color(0xFF42A5F5)

val logTimestampDark = Color(0xFF6E7681)
val logInfoDark = Color(0xFF58A6FF)
val logWarningDark = Color(0xFFDB6D28)
val logCriticalDark = Color(0xFFF85149)

val filePriorityDoNotDownloadDark = Color(0xFF8B949E)
val filePriorityNormalDark = Color(0xFF238636)
val filePriorityHighDark = Color(0xFFE17B5A)
val filePriorityMaximumDark = Color(0xFFF85149)
val filePriorityMixedDark = Color(0xFF03A9F4)

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

val lightCustomColors = CustomColors(
    torrentStateDownloading = torrentStateDownloadingLight,
    torrentStateStalledDownloading = torrentStateStalledDownloadingLight,
    torrentStateStalledUploading = torrentStateStalledUploadingLight,
    torrentStateUploading = torrentStateUploadingLight,
    torrentStatePausedDownloading = torrentStatePausedDownloadingLight,
    torrentStatePausedUploading = torrentStatePausedUploadingLight,
    torrentStateQueued = torrentStateQueuedLight,
    torrentStateError = torrentStateErrorLight,
    seederColor = seederColorLight,
    leecherColor = leecherColorLight,
    logTimestamp = logTimestampLight,
    logInfo = logInfoLight,
    logWarning = logWarningLight,
    logCritical = logCriticalLight,
    filePriorityDoNotDownload = filePriorityDoNotDownloadLight,
    filePriorityNormal = filePriorityNormalLight,
    filePriorityHigh = filePriorityHighLight,
    filePriorityMaximum = filePriorityMaximumLight,
    filePriorityMixed = filePriorityMixedLight,
)

val darkCustomColors = CustomColors(
    torrentStateDownloading = torrentStateDownloadingDark,
    torrentStateStalledDownloading = torrentStateStalledDownloadingDark,
    torrentStateStalledUploading = torrentStateStalledUploadingDark,
    torrentStateUploading = torrentStateUploadingDark,
    torrentStatePausedDownloading = torrentStatePausedDownloadingDark,
    torrentStatePausedUploading = torrentStatePausedUploadingDark,
    torrentStateQueued = torrentStateQueuedDark,
    torrentStateError = torrentStateErrorDark,
    seederColor = seederColorDark,
    leecherColor = leecherColorDark,
    logTimestamp = logTimestampDark,
    logInfo = logInfoDark,
    logWarning = logWarningDark,
    logCritical = logCriticalDark,
    filePriorityDoNotDownload = filePriorityDoNotDownloadDark,
    filePriorityNormal = filePriorityNormalDark,
    filePriorityHigh = filePriorityHighDark,
    filePriorityMaximum = filePriorityMaximumDark,
    filePriorityMixed = filePriorityMixedDark,
)
