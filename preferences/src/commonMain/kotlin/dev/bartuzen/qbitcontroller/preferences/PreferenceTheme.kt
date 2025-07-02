package dev.bartuzen.qbitcontroller.preferences

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Stable
class PreferenceTheme(
    val categoryPadding: PaddingValues,
    val categoryColor: Color,
    val categoryTextStyle: TextStyle,
    val padding: PaddingValues,
    val horizontalSpacing: Dp,
    val disabledOpacity: Float,
    val iconContainerMinWidth: Dp,
    val iconColor: Color,
    val titleColor: Color,
    val titleTextStyle: TextStyle,
    val summaryColor: Color,
    val summaryTextStyle: TextStyle,
    val useTextButtonForDialogConfirmation: Boolean,
    val dialogOkText: String,
    val dialogCancelText: String,
)

@Composable
fun preferenceTheme(
    categoryPadding: PaddingValues = PaddingValues(start = 16.dp, top = 24.dp, end = 16.dp, bottom = 8.dp),
    categoryColor: Color = MaterialTheme.colorScheme.secondary,
    categoryTextStyle: TextStyle = MaterialTheme.typography.labelLarge,
    padding: PaddingValues = PaddingValues(16.dp),
    horizontalSpacing: Dp = 16.dp,
    disabledOpacity: Float = 0.38f,
    iconContainerMinWidth: Dp = 56.dp,
    iconColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    titleTextStyle: TextStyle = MaterialTheme.typography.bodyLarge,
    summaryColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    summaryTextStyle: TextStyle = MaterialTheme.typography.bodyMedium,
    useTextButtonForDialogConfirmation: Boolean = true,
    dialogOkText: String = "OK",
    dialogCancelText: String = "Cancel",
) =
    PreferenceTheme(
        categoryPadding = categoryPadding,
        categoryColor = categoryColor,
        categoryTextStyle = categoryTextStyle,
        padding = padding,
        horizontalSpacing = horizontalSpacing,
        disabledOpacity = disabledOpacity,
        iconContainerMinWidth = iconContainerMinWidth,
        iconColor = iconColor,
        titleColor = titleColor,
        titleTextStyle = titleTextStyle,
        summaryColor = summaryColor,
        summaryTextStyle = summaryTextStyle,
        useTextButtonForDialogConfirmation = useTextButtonForDialogConfirmation,
        dialogOkText = dialogOkText,
        dialogCancelText = dialogCancelText,
    )

val LocalPreferenceTheme = compositionLocalOf<PreferenceTheme> { error("Not provided") }
