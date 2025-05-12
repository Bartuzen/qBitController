package dev.bartuzen.qbitcontroller.di

import com.russhwolf.settings.PreferencesSettings
import com.russhwolf.settings.Settings
import org.koin.core.qualifier.named
import org.koin.dsl.module
import java.util.prefs.Preferences

actual val localStorageModule = module {
    single<Settings>(named("settings")) {
        PreferencesSettings(Preferences.userRoot().node("settings"))
    }
    single<Settings>(named("servers")) {
        PreferencesSettings(Preferences.userRoot().node("servers"))
    }
    single<Settings>(named("torrents")) {
        PreferencesSettings(Preferences.userRoot().node("torrents"))
    }
}
