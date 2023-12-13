package dev.bartuzen.qbitcontroller.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConfigMigrator @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val currentVersion = 2

    fun run() {
        val sharedPref = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        val oldVersion = sharedPref.getInt("configVersion", -1).takeIf { it != -1 }

        if (oldVersion == null) {
            sharedPref.edit()
                .putInt("configVersion", currentVersion)
                .apply()
            return
        }

        if (oldVersion < currentVersion) {
//            startMigration(oldVersion)

            sharedPref.edit()
                .putInt("configVersion", currentVersion)
                .apply()
        }
    }

//    private fun startMigration(oldVersion: Int) {
//        if (oldVersion < 3) {
//
//        }
//    }
}
