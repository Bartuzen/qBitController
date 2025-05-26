package dev.bartuzen.qbitcontroller

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.ComposeUIViewController
import dev.bartuzen.qbitcontroller.data.ConfigMigrator
import dev.bartuzen.qbitcontroller.di.appModule
import dev.bartuzen.qbitcontroller.ui.main.MainScreen
import org.koin.compose.KoinApplication
import org.koin.compose.koinInject

@Suppress("ktlint:standard:function-naming")
fun MainViewController() = ComposeUIViewController {
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

        MainScreen()
    }
}
