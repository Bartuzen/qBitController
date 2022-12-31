package dev.bartuzen.qbitcontroller.utils

import java.io.Serializable

data class Quintuple<out A, out B, out C, out D, out E>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
    val fifth: E
) : Serializable {
    override fun toString() = "($first, $second, $third, $fourth, $fifth)"
}
