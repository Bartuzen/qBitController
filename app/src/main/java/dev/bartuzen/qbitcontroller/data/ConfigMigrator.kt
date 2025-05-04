package dev.bartuzen.qbitcontroller.data

import android.content.Context
import com.russhwolf.settings.SharedPreferencesSettings
import com.russhwolf.settings.get
import com.russhwolf.settings.set
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConfigMigrator @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val currentVersion = 3

    fun run() {
        val settings = SharedPreferencesSettings(context.getSharedPreferences("settings", Context.MODE_PRIVATE))
        val oldVersion = settings["configVersion", -1].takeIf { it != -1 }

        if (oldVersion == null) {
            settings["configVersion"] = currentVersion
            return
        }

        if (oldVersion < currentVersion) {
            startMigration(oldVersion)

            settings["configVersion"] = currentVersion
        }
    }

    private fun startMigration(oldVersion: Int) {
        if (oldVersion < 3) {
            val settings = SharedPreferencesSettings(context.getSharedPreferences("settings", Context.MODE_PRIVATE))
            val notificationCheckInterval = settings["notificationCheckInterval", -1].takeIf { it != -1 }
            if (notificationCheckInterval != null && notificationCheckInterval < 15) {
                settings["notificationCheckInterval"] = 15
            }
        }
    }
}
