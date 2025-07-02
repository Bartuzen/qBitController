package dev.bartuzen.qbitcontroller.preferences

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun Preference(
    title: (@Composable () -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: @Composable (() -> Unit)? = null,
    summary: @Composable (() -> Unit)? = null,
    widgetContainer: @Composable (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    BasicPreference(
        textContainer = {
            val theme = LocalPreferenceTheme.current
            Column(
                modifier =
                    Modifier.padding(
                        theme.padding.copy(
                            start = if (icon != null) 0.dp else Dp.Unspecified,
                            end = if (widgetContainer != null) 0.dp else Dp.Unspecified,
                        )
                    )
            ) {
                if (title != null) {
                    CompositionLocalProvider(
                        LocalContentColor provides
                                theme.titleColor.let {
                                    if (enabled) it else it.copy(alpha = theme.disabledOpacity)
                                }
                    ) {
                        ProvideTextStyle(value = theme.titleTextStyle, content = title)
                    }
                }
                if (summary != null) {
                    CompositionLocalProvider(
                        LocalContentColor provides
                                theme.summaryColor.let {
                                    if (enabled) it else it.copy(alpha = theme.disabledOpacity)
                                }
                    ) {
                        ProvideTextStyle(value = theme.summaryTextStyle, content = summary)
                    }
                }
            }
        },
        modifier = modifier,
        enabled = enabled,
        iconContainer = {
            if (icon != null) {
                val theme = LocalPreferenceTheme.current
                Box(
                    modifier =
                        Modifier.widthIn(min = theme.iconContainerMinWidth)
                            .padding(theme.padding.copy(end = 0.dp)),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    CompositionLocalProvider(
                        LocalContentColor provides
                                theme.iconColor.let {
                                    if (enabled) it else it.copy(alpha = theme.disabledOpacity)
                                },
                        content = icon,
                    )
                }
            }
        },
        widgetContainer = { widgetContainer?.invoke() },
        onClick = onClick,
    )
}
