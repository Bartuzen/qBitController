package dev.bartuzen.qbitcontroller.network

import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readRemaining
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.io.decodeFromSource

actual suspend inline fun <reified T> decodeJson(
    channel: ByteReadChannel,
    deserializer: DeserializationStrategy<T>,
    json: Json,
): T? = json.decodeFromSource(deserializer, channel.readRemaining())
