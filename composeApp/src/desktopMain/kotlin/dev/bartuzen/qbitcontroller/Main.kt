package dev.bartuzen.qbitcontroller

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import dev.bartuzen.qbitcontroller.data.ConfigMigrator
import dev.bartuzen.qbitcontroller.di.appModule
import dev.bartuzen.qbitcontroller.ui.main.MainScreen
import dev.bartuzen.qbitcontroller.ui.theme.AppTheme
import dev.bartuzen.qbitcontroller.utils.stringResource
import org.jetbrains.compose.resources.painterResource
import org.koin.core.context.startKoin
import qbitcontroller.composeapp.generated.resources.Res
import qbitcontroller.composeapp.generated.resources.app_name
import qbitcontroller.composeapp.generated.resources.icon_rounded
import java.awt.Color

fun main() {
    System.setProperty("apple.awt.application.appearance", "system")

    val koinApplication = startKoin {
        modules(appModule)
    }
    val koin = koinApplication.koin

    val configMigrator = koin.get<ConfigMigrator>()
    configMigrator.run()

    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = stringResource(Res.string.app_name),
            icon = painterResource(Res.drawable.icon_rounded),
        ) {
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
