package dev.bartuzen.qbitcontroller.model

data class QBittorrentVersion(
    val major: Int,
    val minor: Int,
    val patch: Int,
) : Comparable<QBittorrentVersion> {
    companion object {
        val Invalid = QBittorrentVersion(Int.MAX_VALUE, Int.MAX_VALUE, Int.MAX_VALUE)

        private val versionRegex = """v?(\d+)\.(\d+)\.(\d+)""".toRegex()

        fun fromString(version: String) = versionRegex.matchEntire(version)?.destructured?.let { (major, minor, patch) ->
            QBittorrentVersion(
                major.toInt(),
                minor.toInt(),
                patch.toInt(),
            )
        } ?: Invalid
    }

    override operator fun compareTo(other: QBittorrentVersion): Int {
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
