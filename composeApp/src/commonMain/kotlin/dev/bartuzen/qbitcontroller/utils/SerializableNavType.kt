package dev.bartuzen.qbitcontroller.utils

import androidx.navigation.NavType
import androidx.savedstate.SavedState
import androidx.savedstate.read
import androidx.savedstate.write
import io.ktor.util.decodeBase64String
import io.ktor.util.encodeBase64
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer

inline fun <reified T : Any> serializableNavType(): NavType<T> {
    val serializer = serializer<T>()
    return object : NavType<T>(false) {
        override fun put(bundle: SavedState, key: String, value: T) {
            val json = Json.encodeToString(serializer, value)
            bundle.write { putString(key, json.encodeBase64()) }
        }

        override fun get(bundle: SavedState, key: String): T {
            return Json.decodeFromString(serializer, bundle.read { getString(key) }.decodeBase64String())
        }

        override fun parseValue(value: String): T {
            return Json.decodeFromString(serializer, value.decodeBase64String())
        }

        override fun serializeAsValue(value: T): String {
            return Json.encodeToString(serializer, value).encodeBase64()
        }
    }
}
