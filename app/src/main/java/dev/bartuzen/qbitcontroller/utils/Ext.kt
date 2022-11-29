package dev.bartuzen.qbitcontroller.utils

import java.util.SortedMap
import kotlin.math.floor

fun Double.floorToDecimal(decimals: Int): Double {
    var multiplier = 1.0
    repeat(decimals) {
        multiplier *= 10
    }
    return floor(this * multiplier) / multiplier
}

fun <K, V> SortedMap<K, V>.first(): V? = get(firstKey())
