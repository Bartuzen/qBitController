package dev.bartuzen.qbitcontroller.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.intl.Locale
import kotlinx.datetime.Instant
import kotlinx.datetime.toNSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSDateFormatterShortStyle
import platform.Foundation.NSLocaleCountryCode
import platform.Foundation.NSNumberFormatter

actual fun Instant.formatDate() = NSDateFormatter().apply {
    dateStyle = NSDateFormatterShortStyle
    timeStyle = NSDateFormatterShortStyle
}.stringFromDate(toNSDate())

@Composable
actual fun getCountryName(countryCode: String) =
    Locale.current.platformLocale.displayNameForKey(NSLocaleCountryCode, countryCode) ?: countryCode

actual fun getDecimalSeparator() = NSNumberFormatter().apply {
    locale = Locale.current.platformLocale
}.decimalSeparator.first()
