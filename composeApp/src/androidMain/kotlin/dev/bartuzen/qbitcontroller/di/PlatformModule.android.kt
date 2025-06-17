package dev.bartuzen.qbitcontroller.di

import dev.bartuzen.qbitcontroller.data.SettingsManager
import dev.bartuzen.qbitcontroller.data.notification.AppNotificationManager
import dev.bartuzen.qbitcontroller.data.notification.TorrentDownloadedWorker
import org.koin.androidx.workmanager.dsl.workerOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.named
import org.koin.dsl.module

actual val platformModule = module {
    single { SettingsManager(get(named("settings"))) }
    singleOf(::AppNotificationManager)

    workerOf(::TorrentDownloadedWorker)
}
