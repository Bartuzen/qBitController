package dev.bartuzen.qbitcontroller.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.intl.Locale
import kotlinx.datetime.Instant
import kotlinx.datetime.toJavaInstant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

actual fun Instant.formatDate(): String = DateTimeFormatter
    .ofLocalizedDateTime(FormatStyle.SHORT)
    .withZone(ZoneId.systemDefault())
    .format(toJavaInstant())

@Composable
actual fun getCountryName(countryCode: String): String =
    Locale("und-$countryCode").platformLocale.getDisplayCountry(Locale.current.platformLocale)
