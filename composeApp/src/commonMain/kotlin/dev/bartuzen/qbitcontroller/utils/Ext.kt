package dev.bartuzen.qbitcontroller.utils

import kotlin.math.floor

fun Double.floorToDecimal(decimals: Int): Double {
    var multiplier = 1.0
    repeat(decimals) {
        multiplier *= 10
    }
    return floor(this * multiplier) / multiplier
}
