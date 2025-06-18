package dev.bartuzen.qbitcontroller.di

import com.russhwolf.settings.PreferencesSettings
import com.russhwolf.settings.Settings
import dev.bartuzen.qbitcontroller.data.DesktopSettingsManager
import dev.bartuzen.qbitcontroller.data.SettingsManager
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module
import java.util.prefs.Preferences

actual val platformModule = module {
    listOf("settings", "servers").forEach { name ->
        single<Settings>(named(name)) {
            PreferencesSettings(Preferences.userRoot().node(name))
        }
    }

    single { DesktopSettingsManager(get(named("settings"))) } bind SettingsManager::class
}
