package dev.bartuzen.qbitcontroller.utils

import java.util.SortedMap
import kotlin.math.floor

fun String.toAsterisks() = "*".repeat(this.length)

fun Double.floorToDecimal(decimals: Int): Double {
    var multiplier = 1.0
    repeat(decimals) {
        multiplier *= 10
    }
    return floor(this * multiplier) / multiplier
}

fun <K, V> SortedMap<K, V>.first(): V? = firstKey().let { key ->
    if (this.containsKey(key)) {
        this[key]
    } else {
        null
    }
}