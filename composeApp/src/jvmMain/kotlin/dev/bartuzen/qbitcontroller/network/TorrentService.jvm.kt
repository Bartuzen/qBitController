package dev.bartuzen.qbitcontroller.network

import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.InternalAPI
import io.ktor.utils.io.jvm.javaio.toInputStream
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream

@OptIn(InternalAPI::class)
actual suspend inline fun <reified T> decodeJson(
    channel: ByteReadChannel,
    deserializer: DeserializationStrategy<T>,
    json: Json,
): T? = json.decodeFromStream(deserializer, channel.toInputStream())
