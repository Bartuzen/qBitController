package dev.bartuzen.qbitcontroller.data

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConfigMigrator @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val currentVersion = 3

    fun run() {
        val sharedPref = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        val oldVersion = sharedPref.getInt("configVersion", -1).takeIf { it != -1 }

        if (oldVersion == null) {
            sharedPref.edit {
                putInt("configVersion", currentVersion)
            }
            return
        }

        if (oldVersion < currentVersion) {
            startMigration(oldVersion)

            sharedPref.edit {
                putInt("configVersion", currentVersion)
            }
        }
    }

    private fun startMigration(oldVersion: Int) {
        if (oldVersion < 3) {
            val sharedPref = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
            val notificationCheckInterval = sharedPref.getInt("notificationCheckInterval", -1).takeIf { it != -1 }
            if (notificationCheckInterval != null && notificationCheckInterval < 15) {
                sharedPref.edit {
                    putInt("notificationCheckInterval", 15)
                }
            }
        }
    }
}
