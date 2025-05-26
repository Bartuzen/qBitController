package dev.bartuzen.qbitcontroller

import androidx.compose.ui.window.ComposeUIViewController
import dev.bartuzen.qbitcontroller.di.appModule
import dev.bartuzen.qbitcontroller.ui.main.MainScreen
import org.koin.compose.KoinApplication

@Suppress("ktlint:standard:function-naming")
fun MainViewController() = ComposeUIViewController {
    KoinApplication(
        application = {
            modules(appModule)
        },
    ) {
        MainScreen()
    }
}
