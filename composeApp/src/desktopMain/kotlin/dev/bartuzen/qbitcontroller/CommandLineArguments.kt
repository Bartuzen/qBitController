package dev.bartuzen.qbitcontroller

data class CommandLineArguments(
    val density: Float?,
    val fontSize: Float?,
    val densityMultiplier: Float?,
    val fontSizeMultiplier: Float?,
) {
    companion object {
        fun parse(args: Array<String>): CommandLineArguments {
            var density: Float? = null
            var fontSize: Float? = null
            var densityMultiplier: Float? = null
            var fontSizeMultiplier: Float? = null

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
                }

                i++
            }

            return CommandLineArguments(
                density = density,
                fontSize = fontSize,
                densityMultiplier = densityMultiplier,
                fontSizeMultiplier = fontSizeMultiplier,
            )
        }
    }
}
