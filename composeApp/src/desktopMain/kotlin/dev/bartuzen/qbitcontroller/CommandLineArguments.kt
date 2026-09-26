package dev.bartuzen.qbitcontroller

import java.io.File

data class CommandLineArguments(
    val density: Float?,
    val fontSize: Float?,
    val densityMultiplier: Float?,
    val fontSizeMultiplier: Float?,
    val torrentUrls: List<String>?,
    val torrentFileUris: List<String>?,
) {
    val hasTorrentLaunch = torrentUrls != null || torrentFileUris != null

    companion object {
        fun parse(args: Array<String>): CommandLineArguments {
            var density: Float? = null
            var fontSize: Float? = null
            var densityMultiplier: Float? = null
            var fontSizeMultiplier: Float? = null
            val torrentUrls = mutableListOf<String>()
            val torrentFileUris = mutableListOf<String>()

            var i = 0
            while (i < args.size) {
                val arg = args[i]

                when (arg) {
                    "--density" -> {
                        density = args.getOrNull(i + 1)?.toFloat()
                        i++
                    }
                    "--font-size" -> {
                        fontSize = args.getOrNull(i + 1)?.toFloat()
                        i++
                    }
                    "--density-multiplier" -> {
                        densityMultiplier = args.getOrNull(i + 1)?.toFloat()
                        i++
                    }
                    "--font-size-multiplier" -> {
                        fontSizeMultiplier = args.getOrNull(i + 1)?.toFloat()
                        i++
                    }
                    else -> {
                        when {
                            arg.startsWith("magnet:", ignoreCase = true) -> torrentUrls += arg
                            arg.endsWith(".torrent", ignoreCase = true) && File(arg).isFile -> torrentFileUris += arg
                        }
                    }
                }

                i++
            }

            return CommandLineArguments(
                density = density,
                fontSize = fontSize,
                densityMultiplier = densityMultiplier,
                fontSizeMultiplier = fontSizeMultiplier,
                torrentUrls = torrentUrls.takeIf { it.isNotEmpty() },
                torrentFileUris = torrentFileUris.takeIf { it.isNotEmpty() },
            )
        }
    }
}
