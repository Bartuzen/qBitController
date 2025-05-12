package dev.bartuzen.qbitcontroller.utils

import androidx.navigation.NavType
import androidx.savedstate.SavedState
import androidx.savedstate.read
import androidx.savedstate.write
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import java.net.URLDecoder
import java.net.URLEncoder

inline fun <reified T : Any> serializableNavType(): NavType<T> {
    val serializer = serializer<T>()
    return object : NavType<T>(false) {
        override fun put(bundle: SavedState, key: String, value: T) {
            val json = Json.encodeToString(serializer, value)
            bundle.write { putString(key, URLEncoder.encode(json, "UTF-8")) }
        }

        override fun get(bundle: SavedState, key: String): T {
            return Json.decodeFromString(serializer, URLDecoder.decode(bundle.read { getString(key) }, "UTF-8"))
        }

        override fun parseValue(value: String): T {
            return Json.decodeFromString(serializer, URLDecoder.decode(value, "UTF-8"))
        }

        override fun serializeAsValue(value: T): String {
            return URLEncoder.encode(Json.encodeToString(serializer, value), "UTF-8")
        }
    }
}
