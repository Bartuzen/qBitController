package dev.bartuzen.qbitcontroller.utils

import java.io.Serializable

data class Quadruple<out A, out B, out C, out D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
) : Serializable {
    override fun toString() = "($first, $second, $third, $fourth)"
}
