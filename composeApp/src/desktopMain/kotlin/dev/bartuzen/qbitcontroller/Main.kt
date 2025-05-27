package dev.bartuzen.qbitcontroller

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import dev.bartuzen.qbitcontroller.data.ConfigMigrator
import dev.bartuzen.qbitcontroller.di.appModule
import dev.bartuzen.qbitcontroller.ui.main.MainScreen
import dev.bartuzen.qbitcontroller.ui.theme.AppTheme
import dev.bartuzen.qbitcontroller.utils.stringResource
import org.koin.compose.KoinApplication
import org.koin.compose.koinInject
import qbitcontroller.composeapp.generated.resources.Res
import qbitcontroller.composeapp.generated.resources.app_name
import java.awt.Color

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = stringResource(Res.string.app_name),
    ) {
        KoinApplication(
            application = {
                modules(appModule)
            },
        ) {
            val configMigrator = koinInject<ConfigMigrator>()
            var ranConfigMigration by remember { mutableStateOf(false) }
            if (!ranConfigMigration) {
                configMigrator.run()
                ranConfigMigration = true
            }

            AppTheme {
                val backgroundColor = MaterialTheme.colorScheme.background
                LaunchedEffect(backgroundColor) {
                    window.background = Color(backgroundColor.toArgb())
                }

                MainScreen()
            }
        }
    }
}
