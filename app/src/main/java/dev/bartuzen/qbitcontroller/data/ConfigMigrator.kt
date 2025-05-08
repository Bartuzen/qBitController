package dev.bartuzen.qbitcontroller.data

import android.content.Context
import com.russhwolf.settings.SharedPreferencesSettings
import com.russhwolf.settings.get
import com.russhwolf.settings.set
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConfigMigrator @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val currentVersion = 4

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

        if (oldVersion < 4) {
            migrateServerConfigs()
        }
    }

    private fun migrateServerConfigs() {
        val servers = SharedPreferencesSettings(context.getSharedPreferences("servers", Context.MODE_PRIVATE))
        val serverConfigsStr: String = servers["serverConfigs"] ?: return

        val json = Json { ignoreUnknownKeys = true }
        val serverConfigsJson = json.parseToJsonElement(serverConfigsStr).jsonObject
        val updatedConfigs = buildJsonObject {
            serverConfigsJson.forEach { (serverId, serverConfigElement) ->
                val serverConfig = serverConfigElement.jsonObject

                val protocol = serverConfig["protocol"]?.jsonPrimitive?.contentOrNull?.lowercase() ?: return@forEach
                val host = serverConfig["host"]?.jsonPrimitive?.contentOrNull ?: return@forEach
                val port = serverConfig["port"]?.jsonPrimitive?.contentOrNull
                val path = serverConfig["path"]?.jsonPrimitive?.contentOrNull

                val url = buildString {
                    append("$protocol://$host")
                    if (port != null) {
                        append(":$port")
                    }
                    if (path != null) {
                        append("/$path")
                    }
                }

                val trustSelfSignedCertificates = serverConfig["trustSelfSignedCertificates"]?.jsonPrimitive?.booleanOrNull
                val basicAuthObject = serverConfig["basicAuth"]?.jsonObject
                val dnsOverHttps = serverConfig["dnsOverHttps"]?.jsonPrimitive?.contentOrNull

                val advancedSettings = buildJsonObject {
                    trustSelfSignedCertificates?.let { put("trustSelfSignedCertificates", JsonPrimitive(it)) }
                    basicAuthObject?.let { put("basicAuth", it) }
                    dnsOverHttps?.let { put("dnsOverHttps", JsonPrimitive(it)) }
                }

                val updatedServerConfig = buildJsonObject {
                    serverConfig.forEach { (key, value) ->
                        when (key) {
                            "protocol",
                            "host",
                            "port",
                            "path",
                            "trustSelfSignedCertificates",
                            "basicAuth",
                            "dnsOverHttps",
                            -> {}
                            else -> put(key, value)
                        }
                    }
                    put("url", JsonPrimitive(url))
                    put("advanced", advancedSettings)
                }

                put(serverId, updatedServerConfig)
            }
        }

        servers["serverConfigs"] = updatedConfigs.toString()
    }
}
