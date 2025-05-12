package dev.bartuzen.qbitcontroller.di

import android.content.Context
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings
import org.koin.core.qualifier.named
import org.koin.dsl.module

actual val localStorageModule = module {
    single<Settings>(named("settings")) {
        val context: Context = get()
        SharedPreferencesSettings(context.getSharedPreferences("settings", Context.MODE_PRIVATE))
    }
    single<Settings>(named("servers")) {
        val context: Context = get()
        SharedPreferencesSettings(context.getSharedPreferences("servers", Context.MODE_PRIVATE))
    }
    single<Settings>(named("torrents")) {
        val context: Context = get()
        SharedPreferencesSettings(context.getSharedPreferences("torrents", Context.MODE_PRIVATE))
    }
}
