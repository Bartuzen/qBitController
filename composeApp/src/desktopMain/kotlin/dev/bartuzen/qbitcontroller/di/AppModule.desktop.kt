package dev.bartuzen.qbitcontroller.di

import com.russhwolf.settings.Settings
import dev.bartuzen.qbitcontroller.data.DesktopSettingsManager
import dev.bartuzen.qbitcontroller.data.JsonSettings
import dev.bartuzen.qbitcontroller.data.SettingsManager
import dev.bartuzen.qbitcontroller.utils.Platform
import dev.bartuzen.qbitcontroller.utils.currentPlatform
import dev.bartuzen.qbitcontroller.utils.getString
import kotlinx.coroutines.runBlocking
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module
import qbitcontroller.composeapp.generated.resources.Res
import qbitcontroller.composeapp.generated.resources.app_name
import java.nio.file.Paths

actual val platformModule = module {
    listOf("settings", "servers").forEach { name ->
        single<Settings>(named(name)) {
            val path = when (currentPlatform) {
                Platform.Desktop.Windows -> Paths.get(System.getenv("APPDATA"))
                Platform.Desktop.Linux -> Paths.get(System.getProperty("user.home"), ".config")
                Platform.Desktop.MacOS -> Paths.get(System.getProperty("user.home"), "Library", "Application Support")
                else -> throw NotImplementedError("Unknown platform: $currentPlatform")
            }.let { path ->
                val appName = runBlocking { getString(Res.string.app_name) }
                path.resolve(appName).resolve("$name.json")
            }

            JsonSettings(path)
        }
    }

    single { DesktopSettingsManager(get(named("settings"))) } bind SettingsManager::class
}
