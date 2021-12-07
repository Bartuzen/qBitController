package dev.bartuzen.qbitcontroller.ui.theme

import androidx.compose.material.Colors
import androidx.compose.ui.graphics.Color

val Blue300 = Color(0xFF4EC2F7)
val Blue500 = Color(0xFF03A9F4)
val Blue700 = Color(0xFF0487D1)

val Yellow600 = Color(0xFFFFB300)
val Yellow700 = Color(0xFFFFA000)

val Gray400 = Color(0xFFB1B1B1)
val Gray700 = Color(0xFF555555)

val Colors.pieceDownloaded: Color
    get() = if (isLight) Blue500 else Blue300

val Colors.pieceNotDownloaded: Color
    get() = if (isLight) Gray400 else Gray700

val Colors.selectedServerBackground: Color
    get() = if (isLight) Gray400 else Gray700