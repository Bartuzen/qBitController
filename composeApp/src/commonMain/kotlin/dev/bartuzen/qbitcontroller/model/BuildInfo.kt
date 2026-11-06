package dev.bartuzen.qbitcontroller.model

import kotlinx.serialization.Serializable

/**
 * Response of `/api/v2/app/buildInfo`. Contains the versions of the libraries the server was built with.
 */
@Serializable
data class BuildInfo(
    val libtorrent: String? = null,
    val platform: String? = null,
)

data class LibtorrentVersion(
    val major: Int,
    val minor: Int,
    val patch: Int = 0,
) : Comparable<LibtorrentVersion> {
    companion object {
        private val versionRegex = """(\d+)\.(\d+)(?:\.(\d+))?""".toRegex()

        fun fromString(version: String?): LibtorrentVersion? =
            version?.let { versionRegex.find(it) }?.destructured?.let { (major, minor, patch) ->
                LibtorrentVersion(
                    major = major.toInt(),
                    minor = minor.toInt(),
                    patch = patch.toIntOrNull() ?: 0,
                )
            }
    }

    override operator fun compareTo(other: LibtorrentVersion): Int {
        val majorComparison = major.compareTo(other.major)
        if (majorComparison != 0) {
            return majorComparison
        }

        val minorComparison = minor.compareTo(other.minor)
        if (minorComparison != 0) {
            return minorComparison
        }

        return patch.compareTo(other.patch)
    }
}
