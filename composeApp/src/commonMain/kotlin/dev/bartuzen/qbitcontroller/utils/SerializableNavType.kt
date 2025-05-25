package dev.bartuzen.qbitcontroller.utils

import androidx.navigation.NavType
import androidx.savedstate.SavedState
import androidx.savedstate.read
import androidx.savedstate.write
import io.ktor.http.decodeURLPart
import io.ktor.http.encodeURLParameter
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer

inline fun <reified T : Any> serializableNavType(): NavType<T> {
    val serializer = serializer<T>()
    return object : NavType<T>(false) {
        override fun put(bundle: SavedState, key: String, value: T) {
            val json = Json.encodeToString(serializer, value)
            bundle.write { putString(key, json.encodeURLParameter()) }
        }

        override fun get(bundle: SavedState, key: String): T {
            return Json.decodeFromString(serializer, bundle.read { getString(key) }.decodeURLPart())
        }

        override fun parseValue(value: String): T {
            return Json.decodeFromString(serializer, value.decodeURLPart())
        }

        override fun serializeAsValue(value: T): String {
            return Json.encodeToString(serializer, value).encodeURLParameter()
        }
    }
}
