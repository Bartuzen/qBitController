package dev.bartuzen.qbitcontroller

import androidx.compose.ui.window.ComposeUIViewController
import dev.bartuzen.qbitcontroller.data.ConfigMigrator
import dev.bartuzen.qbitcontroller.di.appModule
import dev.bartuzen.qbitcontroller.ui.main.MainScreen
import org.koin.core.context.startKoin
import platform.UIKit.UIViewController

@Suppress("ktlint:standard:function-naming", "unused", "FunctionName")
fun MainViewController(): UIViewController {
    val koinApplication = startKoin {
        modules(appModule)
    }
    val koin = koinApplication.koin

    val configMigrator = koin.get<ConfigMigrator>()
    configMigrator.run()

    return ComposeUIViewController {
        MainScreen()
    }
}
