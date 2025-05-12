package dev.bartuzen.qbitcontroller

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import dev.bartuzen.qbitcontroller.di.appModule
import dev.bartuzen.qbitcontroller.ui.main.MainScreen
import dev.bartuzen.qbitcontroller.utils.stringResource
import org.koin.compose.KoinApplication
import qbitcontroller.composeapp.generated.resources.Res
import qbitcontroller.composeapp.generated.resources.app_name

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
            MainScreen()
        }
    }
}
