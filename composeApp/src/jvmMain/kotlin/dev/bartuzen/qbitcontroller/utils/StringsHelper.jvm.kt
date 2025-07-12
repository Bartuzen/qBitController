package dev.bartuzen.qbitcontroller.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.intl.Locale
import java.text.DecimalFormatSymbols
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.time.Instant
import kotlin.time.toJavaInstant

actual fun Instant.formatDate(): String = DateTimeFormatter
    .ofLocalizedDateTime(FormatStyle.SHORT)
    .withZone(ZoneId.systemDefault())
    .format(toJavaInstant())

@Composable
actual fun getCountryName(countryCode: String): String =
    Locale("und-$countryCode").platformLocale.getDisplayCountry(Locale.current.platformLocale)

actual fun getDecimalSeparator(): Char = DecimalFormatSymbols.getInstance().decimalSeparator
