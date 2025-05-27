package dev.bartuzen.qbitcontroller.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.bartuzen.qbitcontroller.data.SettingsManager
import dev.bartuzen.qbitcontroller.data.Theme
import dev.bartuzen.qbitcontroller.utils.stringResource
import kotlinx.coroutines.flow.flow
import me.zhanghai.compose.preference.LocalPreferenceTheme
import me.zhanghai.compose.preference.preferenceTheme
import org.koin.compose.koinInject
import qbitcontroller.composeapp.generated.resources.Res
import qbitcontroller.composeapp.generated.resources.dialog_cancel
import qbitcontroller.composeapp.generated.resources.dialog_ok

private val lightScheme = lightColorScheme(
    primary = primaryLight,
    onPrimary = onPrimaryLight,
    primaryContainer = primaryContainerLight,
    onPrimaryContainer = onPrimaryContainerLight,
    secondary = secondaryLight,
    onSecondary = onSecondaryLight,
    secondaryContainer = secondaryContainerLight,
    onSecondaryContainer = onSecondaryContainerLight,
    tertiary = tertiaryLight,
    onTertiary = onTertiaryLight,
    tertiaryContainer = tertiaryContainerLight,
    onTertiaryContainer = onTertiaryContainerLight,
    error = errorLight,
    onError = onErrorLight,
    errorContainer = errorContainerLight,
    onErrorContainer = onErrorContainerLight,
    background = backgroundLight,
    onBackground = onBackgroundLight,
    surface = surfaceLight,
    onSurface = onSurfaceLight,
    surfaceVariant = surfaceVariantLight,
    onSurfaceVariant = onSurfaceVariantLight,
    outline = outlineLight,
    outlineVariant = outlineVariantLight,
    scrim = scrimLight,
    inverseSurface = inverseSurfaceLight,
    inverseOnSurface = inverseOnSurfaceLight,
    inversePrimary = inversePrimaryLight,
    surfaceDim = surfaceDimLight,
    surfaceBright = surfaceBrightLight,
    surfaceContainerLowest = surfaceContainerLowestLight,
    surfaceContainerLow = surfaceContainerLowLight,
    surfaceContainer = surfaceContainerLight,
    surfaceContainerHigh = surfaceContainerHighLight,
    surfaceContainerHighest = surfaceContainerHighestLight,
)

private val darkScheme = darkColorScheme(
    primary = primaryDark,
    onPrimary = onPrimaryDark,
    primaryContainer = primaryContainerDark,
    onPrimaryContainer = onPrimaryContainerDark,
    secondary = secondaryDark,
    onSecondary = onSecondaryDark,
    secondaryContainer = secondaryContainerDark,
    onSecondaryContainer = onSecondaryContainerDark,
    tertiary = tertiaryDark,
    onTertiary = onTertiaryDark,
    tertiaryContainer = tertiaryContainerDark,
    onTertiaryContainer = onTertiaryContainerDark,
    error = errorDark,
    onError = onErrorDark,
    errorContainer = errorContainerDark,
    onErrorContainer = onErrorContainerDark,
    background = backgroundDark,
    onBackground = onBackgroundDark,
    surface = surfaceDark,
    onSurface = onSurfaceDark,
    surfaceVariant = surfaceVariantDark,
    onSurfaceVariant = onSurfaceVariantDark,
    outline = outlineDark,
    outlineVariant = outlineVariantDark,
    scrim = scrimDark,
    inverseSurface = inverseSurfaceDark,
    inverseOnSurface = inverseOnSurfaceDark,
    inversePrimary = inversePrimaryDark,
    surfaceDim = surfaceDimDark,
    surfaceBright = surfaceBrightDark,
    surfaceContainerLowest = surfaceContainerLowestDark,
    surfaceContainerLow = surfaceContainerLowDark,
    surfaceContainer = surfaceContainerDark,
    surfaceContainerHigh = surfaceContainerHighDark,
    surfaceContainerHighest = surfaceContainerHighestDark,
)

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    val settingsManager = koinInject<SettingsManager>()
    val theme by settingsManager.theme.flow.collectAsStateWithLifecycle()
    val pureBlack by settingsManager.pureBlackDarkMode.flow.collectAsStateWithLifecycle()

    val darkTheme = when (theme) {
        Theme.LIGHT -> false
        Theme.DARK -> true
        Theme.SYSTEM_DEFAULT -> isSystemInDarkTheme()
    }

    val colorScheme = (getDynamicColorScheme(darkTheme) ?: if (darkTheme) darkScheme else lightScheme).let {
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
expect fun getDynamicColorScheme(darkTheme: Boolean): ColorScheme?
