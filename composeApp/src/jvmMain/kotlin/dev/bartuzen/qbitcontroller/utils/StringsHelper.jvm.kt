package dev.bartuzen.qbitcontroller.utils

import kotlinx.datetime.Instant
import kotlinx.datetime.toJavaInstant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

actual fun Instant.formatDate(): String = DateTimeFormatter
    .ofLocalizedDateTime(FormatStyle.SHORT)
    .withZone(ZoneId.systemDefault())
    .format(toJavaInstant())
