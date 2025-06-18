package dev.bartuzen.qbitcontroller

import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import dev.bartuzen.qbitcontroller.data.ConfigMigrator
import dev.bartuzen.qbitcontroller.data.DesktopSettingsManager
import dev.bartuzen.qbitcontroller.di.appModule
import dev.bartuzen.qbitcontroller.generated.BuildConfig
import dev.bartuzen.qbitcontroller.model.WindowState
import dev.bartuzen.qbitcontroller.network.UpdateChecker
import dev.bartuzen.qbitcontroller.network.VersionInfo
import dev.bartuzen.qbitcontroller.ui.components.Dialog
import dev.bartuzen.qbitcontroller.ui.main.MainScreen
import dev.bartuzen.qbitcontroller.ui.theme.AppTheme
import dev.bartuzen.qbitcontroller.utils.rememberReplaceAndApplyStyle
import dev.bartuzen.qbitcontroller.utils.stringResource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.koin.core.context.startKoin
import qbitcontroller.composeapp.generated.resources.Res
import qbitcontroller.composeapp.generated.resources.app_name
import qbitcontroller.composeapp.generated.resources.dialog_cancel
import qbitcontroller.composeapp.generated.resources.icon_rounded
import qbitcontroller.composeapp.generated.resources.update_dialog_download
import qbitcontroller.composeapp.generated.resources.update_dialog_message
import qbitcontroller.composeapp.generated.resources.update_dialog_title
import java.awt.Color
import java.awt.Dimension

fun main() {
    System.setProperty("apple.awt.application.appearance", "system")

    val koinApplication = startKoin {
        modules(appModule)
    }
    val koin = koinApplication.koin

    val configMigrator = koin.get<ConfigMigrator>()
    configMigrator.run()

    val updateChecker = koin.get<UpdateChecker>()
    val settingsManager = koin.get<DesktopSettingsManager>()
    if (BuildConfig.EnableUpdateChecker) {
        CoroutineScope(Dispatchers.Default).launch {
            settingsManager.checkUpdates.flow.collectLatest { enabled ->
                if (enabled) {
                    updateChecker.start()
                } else {
                    updateChecker.stop()
                }
            }
        }
    }

    val savedWindowState = settingsManager.windowState.value
    application {
        val windowState = rememberWindowState(
            placement = savedWindowState.placement,
            position = savedWindowState.position,
            size = savedWindowState.size,
        )
        LaunchedEffect(windowState) {
            snapshotFlow { WindowState(windowState.placement, windowState.position, windowState.size) }
                .debounce(200)
                .collect { settingsManager.windowState.value = it }
        }

        Window(
            onCloseRequest = ::exitApplication,
            title = stringResource(Res.string.app_name),
            icon = painterResource(Res.drawable.icon_rounded),
            state = windowState,
        ) {
            val density = LocalDensity.current
            LaunchedEffect(density) {
                window.minimumSize = with(density) {
                    Dimension(420.dp.toPx().toInt(), 360.dp.toPx().toInt())
                }
            }

            AppTheme {
                val backgroundColor = MaterialTheme.colorScheme.background
                LaunchedEffect(backgroundColor) {
                    window.background = Color(backgroundColor.toArgb())
                }

                var newVersionInfo: VersionInfo? by remember { mutableStateOf(null) }
                if (newVersionInfo != null) {
                    Dialog(
                        onDismissRequest = { newVersionInfo = null },
                        title = { Text(text = stringResource(Res.string.update_dialog_title)) },
                        text = {
                            Text(
                                text = rememberReplaceAndApplyStyle(
                                    text = stringResource(Res.string.update_dialog_message),
                                    oldValues = listOf("%1\$s", "%2\$s"),
                                    newValues = listOf(newVersionInfo?.version ?: "", BuildConfig.Version),
                                    styles = listOf(
                                        SpanStyle(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold),
                                        SpanStyle(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold),
                                    ),
                                ),
                            )
                        },
                        confirmButton = {
                            val uriHandler = LocalUriHandler.current
                            Button(
                                onClick = {
                                    try {
                                        uriHandler.openUri(newVersionInfo?.url ?: "")
                                    } catch (_: Exception) {
                                    }

                                    newVersionInfo = null
                                },
                            ) {
                                Text(text = stringResource(Res.string.update_dialog_download))
                            }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = { newVersionInfo = null },
                            ) {
                                Text(text = stringResource(Res.string.dialog_cancel))
                            }
                        },
                    )
                }

                LaunchedEffect(updateChecker.updateFlow) {
                    updateChecker.updateFlow.collectLatest { version ->
                        newVersionInfo = version
                    }
                }

                MainScreen()
            }
        }
    }
}
