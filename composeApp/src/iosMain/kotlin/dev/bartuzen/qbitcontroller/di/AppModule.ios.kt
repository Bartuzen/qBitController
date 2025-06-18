package dev.bartuzen.qbitcontroller.di

import com.russhwolf.settings.NSUserDefaultsSettings
import com.russhwolf.settings.Settings
import org.koin.core.qualifier.named
import org.koin.dsl.module
import platform.Foundation.NSUserDefaults

actual val platformModule = module {
    listOf("settings", "servers").forEach { name ->
        single<Settings>(named(name)) {
            NSUserDefaultsSettings(NSUserDefaults(suiteName = name))
        }
    }
}
