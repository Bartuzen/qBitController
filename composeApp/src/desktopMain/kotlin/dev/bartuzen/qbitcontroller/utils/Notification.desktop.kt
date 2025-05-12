package dev.bartuzen.qbitcontroller.utils

import androidx.compose.runtime.Composable

@Composable
actual fun notificationPermissionLauncher(): (() -> Unit)? = null

@Composable
actual fun areNotificationsEnabled(): Boolean? = null
