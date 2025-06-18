package dev.bartuzen.qbitcontroller.data

import com.russhwolf.settings.Settings
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import kotlin.io.path.exists

class JsonSettings(
    private val file: Path,
) : Settings {
    private val inMemoryMap = ConcurrentHashMap<String, JsonElement>()
    private val json = Json { prettyPrint = true }

    init {
        if (file.exists()) {
            val content = Files.readString(file)
            val jsonObject = json.decodeFromString<JsonObject>(content)
            inMemoryMap.putAll(jsonObject)
        } else {
            Files.createDirectories(file.parent)
        }
    }

    private fun saveToFile() {
        val jsonObject = JsonObject(inMemoryMap)
        val content = json.encodeToString(jsonObject)
        Files.writeString(file, content)
    }

    private fun putJsonElement(key: String, value: JsonElement) {
        inMemoryMap[key] = value
        saveToFile()
    }

    override val keys get() = inMemoryMap.keys

    override val size get() = inMemoryMap.size

    override fun clear() {
        inMemoryMap.clear()
        saveToFile()
    }

    override fun remove(key: String) {
        inMemoryMap.remove(key)
        saveToFile()
    }

    override fun hasKey(key: String) = inMemoryMap.containsKey(key)

    override fun putInt(key: String, value: Int) = putJsonElement(key, JsonPrimitive(value))

    override fun getInt(key: String, defaultValue: Int) = getIntOrNull(key) ?: defaultValue

    override fun getIntOrNull(key: String) = inMemoryMap[key]?.jsonPrimitive?.intOrNull

    override fun putLong(key: String, value: Long) = putJsonElement(key, JsonPrimitive(value))

    override fun getLong(key: String, defaultValue: Long) = getLongOrNull(key) ?: defaultValue

    override fun getLongOrNull(key: String) = inMemoryMap[key]?.jsonPrimitive?.longOrNull

    override fun putString(key: String, value: String) = putJsonElement(key, JsonPrimitive(value))

    override fun getString(key: String, defaultValue: String) = getStringOrNull(key) ?: defaultValue

    override fun getStringOrNull(key: String) = inMemoryMap[key]?.jsonPrimitive?.contentOrNull

    override fun putFloat(key: String, value: Float) = putJsonElement(key, JsonPrimitive(value))

    override fun getFloat(key: String, defaultValue: Float) = getFloatOrNull(key) ?: defaultValue

    override fun getFloatOrNull(key: String) = inMemoryMap[key]?.jsonPrimitive?.floatOrNull

    override fun putDouble(key: String, value: Double) = putJsonElement(key, JsonPrimitive(value))

    override fun getDouble(key: String, defaultValue: Double) = getDoubleOrNull(key) ?: defaultValue

    override fun getDoubleOrNull(key: String) = inMemoryMap[key]?.jsonPrimitive?.doubleOrNull

    override fun putBoolean(key: String, value: Boolean) = putJsonElement(key, JsonPrimitive(value))

    override fun getBoolean(key: String, defaultValue: Boolean) = getBooleanOrNull(key) ?: defaultValue

    override fun getBooleanOrNull(key: String) = inMemoryMap[key]?.jsonPrimitive?.booleanOrNull
}
