package dev.bartuzen.qbitcontroller.utils

import android.Manifest
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.NotificationManagerCompat
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale

@Composable
actual fun notificationPermissionLauncher(): (() -> Unit)? {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val permissionState = rememberPermissionState(permission = Manifest.permission.POST_NOTIFICATIONS)

        if (!permissionState.status.shouldShowRationale) {
            return {
                permissionState.launchPermissionRequest()
            }
        }
    }

    return null
}

@Composable
actual fun areNotificationsEnabled(): Boolean? =
    NotificationManagerCompat.from(LocalContext.current).areNotificationsEnabled()
