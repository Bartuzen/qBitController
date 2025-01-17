package dev.bartuzen.qbitcontroller.utils

import android.graphics.drawable.AdaptiveIconDrawable
import android.os.Build
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.expandIn
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkOut
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.node.Ref
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toBitmap
import com.google.android.material.color.MaterialColors

@Composable
fun harmonizeWithPrimary(color: Color) = Color(
    MaterialColors.harmonize(color.toArgb(), MaterialTheme.colorScheme.primary.toArgb()),
)

@Composable
inline fun <T> AnimatedNullableVisibility(
    value: T?,
    modifier: Modifier = Modifier,
    enter: EnterTransition = fadeIn() + expandIn(),
    exit: ExitTransition = fadeOut() + shrinkOut(),
    crossinline content: @Composable (scope: AnimatedVisibilityScope, T) -> Unit,
) {
    val ref = remember { Ref<T>() }
    ref.value = value ?: ref.value

    AnimatedVisibility(
        modifier = modifier,
        visible = value != null,
        enter = enter,
        exit = exit,
        content = {
            ref.value?.let { value ->
                content(this, value)
            }
        },
    )
}

@Composable
fun measureTextWidth(text: String, style: TextStyle = LocalTextStyle.current): Dp {
    val textMeasurer = rememberTextMeasurer()
    val widthInPixels = textMeasurer.measure(text, style).size.width
    return with(LocalDensity.current) { widthInPixels.toDp() }
}

@Composable
fun rememberApplyStyle(text: String, textToStyle: String, style: SpanStyle) = remember(text, textToStyle, style) {
    buildAnnotatedString {
        var currentIndex = 0
        val searchText = textToStyle.lowercase()

        while (currentIndex < text.length) {
            val index = text.indexOf(searchText, currentIndex, ignoreCase = true)
            if (index == -1) {
                append(text.substring(currentIndex))
                break
            }
            append(text.substring(currentIndex, index))
            withStyle(style) {
                append(text.substring(index, index + textToStyle.length))
            }
            currentIndex = index + textToStyle.length
        }
    }
}

@Composable
fun rememberReplaceAndApplyStyle(text: String, oldValue: String, newValue: String, style: SpanStyle) =
    remember(text, oldValue, newValue, style) {
        buildAnnotatedString {
            val parts = text.split(oldValue)
            parts.forEachIndexed { index, part ->
                append(part)
                if (index != parts.lastIndex) {
                    withStyle(style) {
                        append(newValue)
                    }
                }
            }
        }
    }

@Composable
fun adaptiveIconPainterResource(@DrawableRes id: Int): Painter {
    val context = LocalContext.current
    val res = context.resources
    val theme = context.theme

    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val adaptiveIcon = remember(id) {
            ResourcesCompat.getDrawable(res, id, theme) as? AdaptiveIconDrawable
        }
        if (adaptiveIcon != null) {
            remember(id) {
                BitmapPainter(adaptiveIcon.toBitmap().asImageBitmap())
            }
        } else {
            painterResource(id)
        }
    } else {
        painterResource(id)
    }
}

// https://issuetracker.google.com/issues/382564794
@Composable
fun Modifier.dropdownMenuHeight() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
    val insets = WindowInsets.systemBars.asPaddingValues()
    val availableHeight = LocalConfiguration.current.screenHeightDp.dp -
        insets.calculateTopPadding() - insets.calculateBottomPadding()

    heightIn(max = availableHeight)
} else {
    this
}
