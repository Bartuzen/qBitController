package dev.bartuzen.qbitcontroller.di

import com.russhwolf.settings.NSUserDefaultsSettings
import com.russhwolf.settings.Settings
import org.koin.core.qualifier.named
import org.koin.dsl.module
import platform.Foundation.NSUserDefaults

actual val localStorageModule = module {
    single<Settings>(named("settings")) {
        NSUserDefaultsSettings(NSUserDefaults(suiteName = "settings"))
    }
    single<Settings>(named("servers")) {
        NSUserDefaultsSettings(NSUserDefaults(suiteName = "servers"))
    }
    single<Settings>(named("torrents")) {
        NSUserDefaultsSettings(NSUserDefaults(suiteName = "torrents"))
    }
}
