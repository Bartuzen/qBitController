package dev.bartuzen.qbitcontroller.utils

import android.net.Uri
import android.os.Bundle
import androidx.navigation.NavType
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer

inline fun <reified T : Any> serializableNavType(): NavType<T> {
    val serializer = serializer<T>()
    return object : NavType<T>(false) {
        override fun put(bundle: Bundle, key: String, value: T) {
            val json = Json.encodeToString(serializer, value)
            bundle.putString(key, Uri.encode(json))
        }

        override fun get(bundle: Bundle, key: String): T {
            return Json.decodeFromString(serializer, Uri.decode(bundle.getString(key)!!))
        }

        override fun parseValue(value: String): T {
            return Json.decodeFromString(serializer, Uri.decode(value))
        }

        override fun serializeAsValue(value: T): String {
            return Uri.encode(Json.encodeToString(serializer, value))
        }
    }
}
