package dev.bartuzen.qbitcontroller.network

import dev.bartuzen.qbitcontroller.generated.BuildConfig
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy
import kotlin.time.Duration.Companion.hours

class UpdateChecker {
    private val httpClient = HttpClient {
        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                    namingStrategy = JsonNamingStrategy.SnakeCase
                },
            )
        }
    }

    private val updateChannel = Channel<VersionInfo>(Channel.CONFLATED)
    val updateFlow = updateChannel.receiveAsFlow()

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var updateJob: Job? = null

    fun start() {
        updateJob?.cancel()
        updateJob = scope.launch {
            while (isActive) {
                checkUpdates()
                delay(1.hours)
            }
        }
    }

    fun stop() {
        updateJob?.cancel()
    }

    private suspend fun checkUpdates() {
        try {
            val response = httpClient.get(BuildConfig.LatestReleaseUrl)
            val release = response.body<Release>()
            val version = release.tagName.drop(1)

            val isCurrentVersionLatest = isCurrentVersionLatest(BuildConfig.Version, version)
            if (!isCurrentVersionLatest) {
                val versionInfo = VersionInfo(version, release.htmlUrl)
                updateChannel.send(versionInfo)
            }
        } catch (e: Exception) {
            if (e is CancellationException) {
                throw e
            }
            println("Unable to check for updates: ${e.message}")
        }
    }

    fun isCurrentVersionLatest(currentVersion: String, latestVersion: String): Boolean {
        val currentParts = currentVersion.split(".")
        val latestParts = latestVersion.split(".")
        val maxLength = maxOf(currentParts.size, latestParts.size)

        for (i in 0 until maxLength) {
            val current = currentParts.getOrNull(i)?.toIntOrNull() ?: 0
            val latest = latestParts.getOrNull(i)?.toIntOrNull() ?: 0

            when {
                latest > current -> return false
                latest < current -> return true
            }
        }

        return true
    }
}

@Serializable
data class Release(
    val tagName: String,
    val htmlUrl: String,
)

data class VersionInfo(
    val version: String,
    val url: String,
)
