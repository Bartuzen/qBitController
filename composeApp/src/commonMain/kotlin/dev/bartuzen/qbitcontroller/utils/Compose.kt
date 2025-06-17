package dev.bartuzen.qbitcontroller.utils

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandIn
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkOut
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.layout
import androidx.compose.ui.node.Ref
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp

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
inline fun <T> AnimatedNullableVisibility(
    values: List<T?>,
    modifier: Modifier = Modifier,
    enter: EnterTransition = fadeIn() + expandIn(),
    exit: ExitTransition = fadeOut() + shrinkOut(),
    crossinline content: @Composable (scope: AnimatedVisibilityScope, List<T>) -> Unit,
) {
    val ref = remember { Ref<List<T>>() }
    val nonNullValues = values.filterNotNull()

    if (nonNullValues.size == values.size) {
        ref.value = nonNullValues
    }

    AnimatedVisibility(
        modifier = modifier,
        visible = nonNullValues.size == values.size,
        enter = enter,
        exit = exit,
        content = {
            ref.value?.let { values ->
                content(this, values)
            }
        },
    )
}

@Composable
inline fun <T> AnimatedListVisibility(
    items: List<T>,
    modifier: Modifier = Modifier,
    enter: EnterTransition = fadeIn() + expandIn(),
    exit: ExitTransition = fadeOut() + shrinkOut(),
    crossinline content: @Composable AnimatedVisibilityScope.(List<T>) -> Unit,
) {
    val displayedItems = remember { mutableStateOf(items) }

    LaunchedEffect(items) {
        if (items.isNotEmpty()) {
            displayedItems.value = items
        }
    }

    val visibleItems = displayedItems.value

    AnimatedVisibility(
        modifier = modifier,
        visible = items.isNotEmpty(),
        enter = enter,
        exit = exit,
    ) {
        content(this, visibleItems)
    }
}

@Composable
fun measureTextWidth(text: String, style: TextStyle = LocalTextStyle.current): Dp {
    val textMeasurer = rememberTextMeasurer()
    val widthInPixels = textMeasurer.measure(text, style).size.width
    return with(LocalDensity.current) { widthInPixels.toDp() }
}

@Composable
fun rememberSearchStyle(text: String, searchQuery: String, style: SpanStyle) = remember(text, searchQuery, style) {
    buildAnnotatedString {
        val terms = searchQuery
            .split(" ")
            .filter { it.isNotEmpty() && it != "-" }
        val separators = setOf(' ', '-', '_', '.')

        var currentIndex = 0
        while (currentIndex < text.length) {
            val firstMatch = terms.mapNotNull { token ->
                text.indexOf(token, currentIndex, ignoreCase = true).takeIf { it != -1 }?.let { it to token }
            }.minByOrNull { it.first }
            if (firstMatch == null) {
                append(text.substring(currentIndex))
                break
            }
            val (matchIndex, token) = firstMatch
            append(text.substring(currentIndex, matchIndex))
            val highlightStart = matchIndex
            var highlightEnd = matchIndex + token.length
            while (true) {
                val nextMatch = terms.mapNotNull { token ->
                    text.indexOf(token, highlightEnd, ignoreCase = true).takeIf { it != -1 }?.let { it to token }
                }.minByOrNull { it.first } ?: break
                val (nextMatchIndex, nextToken) = nextMatch
                val gap = text.substring(highlightEnd, nextMatchIndex)
                if (gap.isNotEmpty() && gap.all { it in separators }) {
                    highlightEnd = nextMatchIndex + nextToken.length
                } else {
                    break
                }
            }
            withStyle(style) {
                append(text.substring(highlightStart, highlightEnd))
            }
            currentIndex = highlightEnd
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
fun rememberReplaceAndApplyStyle(text: String, oldValues: List<String>, newValues: List<String>, styles: List<SpanStyle>) =
    remember(text, oldValues, newValues, styles) {
        require(oldValues.size == newValues.size && newValues.size == styles.size) {
            "Lists of oldValues, newValues, and styles must have the same size"
        }

        buildAnnotatedString {
            var startIndex = 0

            while (startIndex < text.length) {
                var earliestMatch: Triple<Int, String, Int>? = null

                oldValues.forEachIndexed { valueIndex, oldValue ->
                    if (oldValue.isNotEmpty()) {
                        val index = text.indexOf(oldValue, startIndex)
                        if (index != -1 && (earliestMatch == null || index < earliestMatch.first)) {
                            earliestMatch = Triple(index, newValues[valueIndex], valueIndex)
                        }
                    }
                }

                if (earliestMatch != null) {
                    val (index, newValue, styleIndex) = earliestMatch
                    append(text.substring(startIndex, index))

                    withStyle(styles[styleIndex]) {
                        append(newValue)
                    }

                    val oldValue = oldValues[styleIndex]
                    startIndex = index + oldValue.length
                } else {
                    append(text.substring(startIndex))
                    break
                }
            }
        }
    }

fun Modifier.fillWidthOfParent(parentPadding: Dp) = this.layout { measurable, constraints ->
    val placeable = measurable.measure(constraints.copy(maxWidth = constraints.maxWidth + parentPadding.roundToPx()))
    layout(placeable.width, placeable.height) {
        placeable.place(0, 0)
    }
}

@Composable
fun PaddingValues.excludeBottom(): PaddingValues {
    val layoutDirection = LocalLayoutDirection.current
    return PaddingValues(
        start = calculateLeftPadding(layoutDirection),
        end = calculateRightPadding(layoutDirection),
        top = calculateTopPadding(),
    )
}

@Composable
expect fun calculateWindowSizeClass(): WindowSizeClass

@Composable
fun appBarColor(
    scrollBehavior: TopAppBarScrollBehavior,
    colors: TopAppBarColors = TopAppBarDefaults.topAppBarColors(),
): Color {
    val colorTransitionFraction by remember(scrollBehavior) {
        derivedStateOf {
            val overlappingFraction = scrollBehavior.state.overlappedFraction
            if (overlappingFraction > 0.01f) 1f else 0f
        }
    }

    val appBarContainerColor by animateColorAsState(
        targetValue = lerp(
            colors.containerColor,
            colors.scrolledContainerColor,
            FastOutLinearInEasing.transform(colorTransitionFraction),
        ),
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
    )
    return appBarContainerColor
}

@Composable
operator fun PaddingValues.plus(other: PaddingValues): PaddingValues {
    val layoutDirection = LocalLayoutDirection.current
    return PaddingValues(
        start = calculateStartPadding(layoutDirection) + other.calculateStartPadding(layoutDirection),
        top = calculateTopPadding() + other.calculateTopPadding(),
        end = calculateEndPadding(layoutDirection) + other.calculateEndPadding(layoutDirection),
        bottom = calculateBottomPadding() + other.calculateBottomPadding(),
    )
}
