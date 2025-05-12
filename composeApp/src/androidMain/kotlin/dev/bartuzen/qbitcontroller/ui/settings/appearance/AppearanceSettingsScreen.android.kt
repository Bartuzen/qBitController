package dev.bartuzen.qbitcontroller.ui.settings.appearance

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.core.os.LocaleListCompat
import dev.bartuzen.qbitcontroller.generated.SupportedLanguages
import dev.bartuzen.qbitcontroller.utils.stringResource
import me.zhanghai.compose.preference.ListPreference
import me.zhanghai.compose.preference.Preference
import qbitcontroller.composeapp.generated.resources.Res
import qbitcontroller.composeapp.generated.resources.settings_language
import qbitcontroller.composeapp.generated.resources.settings_language_system_default
import java.util.Locale

@Composable
actual fun LanguagePreference() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val context = LocalContext.current
        Preference(
            title = { Text(text = stringResource(Res.string.settings_language)) },
            summary = {
                Text(
                    text = getLanguageDisplayName(getLanguageCode(AppCompatDelegate.getApplicationLocales()[0]))
                        ?: stringResource(Res.string.settings_language_system_default),
                )
            },
            onClick = {
                val intent = Intent(Settings.ACTION_APP_LOCALE_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                }
                context.startActivity(intent)
            },
        )
    } else {
        val locales = mapOf("" to stringResource(Res.string.settings_language_system_default)) + SupportedLanguages
        ListPreference(
            value = getLanguageCode(AppCompatDelegate.getApplicationLocales()[0]),
            onValueChange = { AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(it)) },
            values = locales.keys.toList(),
            title = { Text(text = stringResource(Res.string.settings_language)) },
            summary = locales[getLanguageCode(AppCompatDelegate.getApplicationLocales()[0])]?.let {
                {
                    Text(text = it)
                }
            },
            valueToText = { AnnotatedString(locales[it] ?: "") },
        )
    }
}

private fun getLanguageDisplayName(language: String?): String? {
    val locale = when (language) {
        null, "" -> return null
        "zh-CN" -> Locale.forLanguageTag("zh-Hans")
        "zh-TW" -> Locale.forLanguageTag("zh-Hant")
        else -> Locale.forLanguageTag(language)
    }
    return locale.getDisplayName(locale).replaceFirstChar { it.uppercase() }
}

private fun getLanguageCode(locale: Locale?): String {
    if (locale == null) {
        return ""
    }
    return locale.toString().replace("_", "-")
}
