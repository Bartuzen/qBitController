package dev.bartuzen.qbitcontroller.di

import dev.bartuzen.qbitcontroller.data.SettingsManager
import org.koin.core.qualifier.named
import org.koin.dsl.module

actual val platformModule = module {
    single { SettingsManager(get(named("settings"))) }
}
