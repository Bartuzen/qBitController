package dev.bartuzen.qbitcontroller.di

import android.content.Context
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings
import dev.bartuzen.qbitcontroller.data.notification.AppNotificationManager
import dev.bartuzen.qbitcontroller.data.notification.TorrentDownloadedWorker
import org.koin.androidx.workmanager.dsl.workerOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.named
import org.koin.dsl.module

actual val platformModule = module {
    listOf("settings", "servers", "torrents").forEach { name ->
        single<Settings>(named(name)) {
            val context: Context = get()
            SharedPreferencesSettings(context.getSharedPreferences(name, Context.MODE_PRIVATE))
        }
    }

    singleOf(::AppNotificationManager)

    workerOf(::TorrentDownloadedWorker)
}
