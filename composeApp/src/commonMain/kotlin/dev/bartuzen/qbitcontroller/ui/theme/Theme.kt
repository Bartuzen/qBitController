package dev.bartuzen.qbitcontroller.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.materialkolor.rememberDynamicColorScheme
import dev.bartuzen.qbitcontroller.data.SettingsManager
import dev.bartuzen.qbitcontroller.data.Theme
import dev.bartuzen.qbitcontroller.preferences.LocalPreferenceTheme
import dev.bartuzen.qbitcontroller.preferences.preferenceTheme
import dev.bartuzen.qbitcontroller.utils.stringResource
import org.koin.compose.koinInject
import qbitcontroller.composeapp.generated.resources.Res
import qbitcontroller.composeapp.generated.resources.dialog_cancel
import qbitcontroller.composeapp.generated.resources.dialog_ok

private val primaryColor = Color(0xFF415F91)

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    val settingsManager = koinInject<SettingsManager>()
    val pureBlack by settingsManager.pureBlackDarkMode.flow.collectAsStateWithLifecycle()

    val darkTheme = isDarkTheme()
    val defaultColorSchema = rememberDynamicColorScheme(primary = primaryColor, isDark = darkTheme, isAmoled = false)
    val dynamicColorSchema = getDynamicColorScheme(darkTheme)

    val colorScheme = (dynamicColorSchema ?: defaultColorSchema).let {
        if (pureBlack && darkTheme) {
            val surfaceContainer = Color(0xFF0C0C0C)
            val surfaceContainerHigh = Color(0xFF131313)
            val surfaceContainerHighest = Color(0xFF1B1B1B)

            it.copy(
                background = Color.Black,
                onBackground = Color.White,
                surface = Color.Black,
                onSurface = Color.White,
                surfaceVariant = surfaceContainer,
                surfaceContainerLowest = surfaceContainer,
                surfaceContainerLow = surfaceContainer,
                surfaceContainer = surfaceContainer,
                surfaceContainerHigh = surfaceContainerHigh,
                surfaceContainerHighest = surfaceContainerHighest,
            )
        } else {
            it
        }
    }

    MaterialTheme(colorScheme = colorScheme) {
        val preferenceTheme = preferenceTheme(
            dialogOkText = stringResource(Res.string.dialog_ok),
            dialogCancelText = stringResource(Res.string.dialog_cancel),
            titleTextStyle = MaterialTheme.typography.titleLarge.copy(fontSize = 16.sp),
            summaryTextStyle = MaterialTheme.typography.bodySmall,
            iconColor = MaterialTheme.colorScheme.primary,
            useTextButtonForDialogConfirmation = false,
        )

        val customColorsPalette = if (darkTheme) darkCustomColors else lightCustomColors
        CompositionLocalProvider(
            LocalTextStyle provides MaterialTheme.typography.bodyMedium,
            LocalContentColor provides MaterialTheme.colorScheme.onBackground,
            LocalCustomColors provides customColorsPalette,
            LocalPreferenceTheme provides preferenceTheme,
            content = content,
        )
    }
}

@Composable
fun isDarkTheme(): Boolean {
    val settingsManager = koinInject<SettingsManager>()
    val theme by settingsManager.theme.flow.collectAsStateWithLifecycle()

    return when (theme) {
        Theme.LIGHT -> false
        Theme.DARK -> true
        Theme.SYSTEM_DEFAULT -> isSystemInDarkTheme()
    }
}

@Composable
expect fun getDynamicColorScheme(darkTheme: Boolean): ColorScheme?
