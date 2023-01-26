package dev.bartuzen.qbitcontroller.data

import android.content.Context
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
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
            startMigration(oldVersion)

            sharedPref.edit()
                .putInt("configVersion", currentVersion)
                .apply()
        }
    }

    private fun startMigration(oldVersion: Int) {
        if (oldVersion < 2) {
            val sharedPref = context.getSharedPreferences("servers", Context.MODE_PRIVATE)
            val serverConfigs = sharedPref.getString("serverConfigs", null)

            if (serverConfigs != null) {
                val mapper = jacksonObjectMapper()
                    .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                val node = mapper.readTree(serverConfigs)

                for ((_, serverConfig) in node.fields()) {
                    val basicAuth = mapOf<String, Any?>("isEnabled" to false, "username" to null, "password" to null)
                    val basicAuthNode = mapper.valueToTree<ObjectNode>(basicAuth)
                    (serverConfig as ObjectNode).set<ObjectNode>("basicAuth", basicAuthNode)
                }

                sharedPref.edit()
                    .putString("serverConfigs", mapper.writeValueAsString(node))
                    .apply()
            }
        }
    }
}
