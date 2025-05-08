package dev.bartuzen.qbitcontroller.ui.settings.appearance

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.bartuzen.qbitcontroller.R
import dev.bartuzen.qbitcontroller.data.Theme
import me.zhanghai.compose.preference.ListPreference
import me.zhanghai.compose.preference.Preference
import org.koin.compose.viewmodel.koinViewModel
import org.xmlpull.v1.XmlPullParser
import java.util.Locale

@Composable
fun AppearanceSettingsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AppearanceSettingsViewModel = koinViewModel(),
) {
    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings_category_appearance),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            contentPadding = innerPadding,
            modifier = Modifier.fillMaxSize(),
        ) {
            item {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val context = LocalContext.current
                    Preference(
                        title = { Text(text = stringResource(R.string.settings_language)) },
                        summary = {
                            Text(
                                text = getLanguageDisplayName(getLanguageCode(AppCompatDelegate.getApplicationLocales()[0]))
                                    ?: stringResource(R.string.settings_language_system_default),
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
                    val locales = getLocales()
                    ListPreference(
                        value = getLanguageCode(AppCompatDelegate.getApplicationLocales()[0]),
                        onValueChange = { AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(it)) },
                        values = locales.keys.toList(),
                        title = { Text(text = stringResource(R.string.settings_language)) },
                        summary = locales[getLanguageCode(AppCompatDelegate.getApplicationLocales()[0])]?.let {
                            {
                                Text(text = it)
                            }
                        },
                        valueToText = { AnnotatedString(locales[it] ?: "") },
                    )
                }
            }

            item {
                val theme by viewModel.theme.flow.collectAsStateWithLifecycle()

                val lightString = stringResource(R.string.settings_theme_light)
                val darkString = stringResource(R.string.settings_theme_dark)
                val systemString = stringResource(R.string.settings_theme_system_default)

                ListPreference(
                    value = theme,
                    onValueChange = { viewModel.theme.value = it },
                    values = listOf(Theme.LIGHT, Theme.DARK, Theme.SYSTEM_DEFAULT),
                    title = { Text(text = stringResource(R.string.settings_theme)) },
                    valueToText = { themeValue ->
                        AnnotatedString(
                            when (themeValue) {
                                Theme.LIGHT -> lightString
                                Theme.DARK -> darkString
                                Theme.SYSTEM_DEFAULT -> systemString
                            },
                        )
                    },
                    summary = {
                        Text(
                            text = when (theme) {
                                Theme.LIGHT -> lightString
                                Theme.DARK -> darkString
                                Theme.SYSTEM_DEFAULT -> systemString
                            },
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun getLocales(): Map<String, String> {
    val systemDefaultString = stringResource(R.string.settings_language_system_default)
    val context = LocalContext.current
    val languages = remember {
        val languages = mutableListOf<Pair<String, String>>()
        val resources = context.resources
        val parser = resources.getXml(R.xml.locales_config)

        while (parser.eventType != XmlPullParser.END_DOCUMENT) {
            if (parser.eventType == XmlPullParser.START_TAG && parser.name == "locale") {
                repeat(parser.attributeCount) { i ->
                    val tag = parser.getAttributeValue(i)
                    val displayName = getLanguageDisplayName(tag)

                    if (displayName != null) {
                        languages.add(tag to displayName)
                    }
                }
            }

            parser.next()
        }

        languages.sortBy { (_, displayName) -> displayName }
        languages.add(0, "" to systemDefaultString)

        languages.toMap()
    }

    return languages
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
