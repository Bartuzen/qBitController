package dev.bartuzen.qbitcontroller.ui.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastForEachIndexed
import androidx.compose.ui.window.DialogProperties
import kotlin.math.max

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun Dialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    textHorizontalPadding: Dp = 24.dp,
    scrollSate: ScrollState = rememberScrollState(),
    dismissButton: @Composable (() -> Unit)? = null,
    icon: @Composable (() -> Unit)? = null,
    title: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
    shape: Shape = AlertDialogDefaults.shape,
    containerColor: Color = AlertDialogDefaults.containerColor,
    iconContentColor: Color = AlertDialogDefaults.iconContentColor,
    titleContentColor: Color = AlertDialogDefaults.titleContentColor,
    textContentColor: Color = AlertDialogDefaults.textContentColor,
    buttonContentColor: Color = MaterialTheme.colorScheme.primary,
    tonalElevation: Dp = AlertDialogDefaults.TonalElevation,
    properties: DialogProperties = DialogProperties(),
) {
    val config = LocalConfiguration.current
    val maxWidth = config.screenWidthDp.dp - 64.dp
    val maxHeight = config.screenHeightDp.dp - 64.dp

    BasicAlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = modifier
            .sizeIn(maxHeight = maxHeight, maxWidth = maxWidth)
            .fillMaxWidth(),
        properties = properties,
    ) {
        Surface(
            modifier = Modifier,
            shape = shape,
            color = containerColor,
            tonalElevation = tonalElevation,
        ) {
            Column(modifier = Modifier.padding(vertical = 24.dp)) {
                icon?.let {
                    CompositionLocalProvider(LocalContentColor provides iconContentColor) {
                        Box(
                            Modifier
                                .padding(horizontal = 24.dp)
                                .padding(bottom = 16.dp)
                                .align(Alignment.CenterHorizontally),
                        ) {
                            icon()
                        }
                    }
                }
                title?.let {
                    CompositionLocalProvider(
                        LocalContentColor provides titleContentColor,
                        LocalTextStyle provides LocalTextStyle.current.merge(MaterialTheme.typography.headlineSmall),
                    ) {
                        Box(
                            Modifier
                                .padding(horizontal = 24.dp)
                                .padding(bottom = 16.dp)
                                .align(
                                    if (icon == null) {
                                        Alignment.Start
                                    } else {
                                        Alignment.CenterHorizontally
                                    },
                                ),
                        ) {
                            title()
                        }
                    }
                }
                text?.let {
                    CompositionLocalProvider(
                        LocalContentColor provides textContentColor,
                        LocalTextStyle provides LocalTextStyle.current.merge(MaterialTheme.typography.bodyMedium),
                    ) {
                        Box(
                            Modifier
                                .weight(weight = 1f, fill = false)
                                .padding(horizontal = textHorizontalPadding)
                                .padding(bottom = 24.dp)
                                .align(Alignment.Start)
                                .verticalScroll(scrollSate),
                        ) {
                            text()
                        }
                    }
                }
                Box(
                    modifier = Modifier
                        .padding(horizontal = 24.dp)
                        .align(Alignment.End),
                ) {
                    CompositionLocalProvider(
                        LocalContentColor provides buttonContentColor,
                        LocalTextStyle provides LocalTextStyle.current.merge(MaterialTheme.typography.labelLarge),
                    ) {
                        AlertDialogFlowRow(
                            mainAxisSpacing = 8.dp,
                            crossAxisSpacing = 12.dp,
                        ) {
                            dismissButton?.invoke()
                            confirmButton()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AlertDialogFlowRow(mainAxisSpacing: Dp, crossAxisSpacing: Dp, content: @Composable () -> Unit) {
    Layout(content) { measurables, constraints ->
        val sequences = mutableListOf<List<Placeable>>()
        val crossAxisSizes = mutableListOf<Int>()
        val crossAxisPositions = mutableListOf<Int>()

        var mainAxisSpace = 0
        var crossAxisSpace = 0

        val currentSequence = mutableListOf<Placeable>()
        var currentMainAxisSize = 0
        var currentCrossAxisSize = 0

        fun canAddToCurrentSequence(placeable: Placeable) = currentSequence.isEmpty() ||
            currentMainAxisSize + mainAxisSpacing.roundToPx() + placeable.width <=
            constraints.maxWidth

        fun startNewSequence() {
            if (sequences.isNotEmpty()) {
                crossAxisSpace += crossAxisSpacing.roundToPx()
            }
            @Suppress("ListIterator")
            sequences.add(0, currentSequence.toList())
            crossAxisSizes += currentCrossAxisSize
            crossAxisPositions += crossAxisSpace

            crossAxisSpace += currentCrossAxisSize
            mainAxisSpace = max(mainAxisSpace, currentMainAxisSize)

            currentSequence.clear()
            currentMainAxisSize = 0
            currentCrossAxisSize = 0
        }

        measurables.fastForEach { measurable ->
            val placeable = measurable.measure(constraints)

            if (!canAddToCurrentSequence(placeable)) startNewSequence()

            if (currentSequence.isNotEmpty()) {
                currentMainAxisSize += mainAxisSpacing.roundToPx()
            }
            currentSequence.add(placeable)
            currentMainAxisSize += placeable.width
            currentCrossAxisSize = max(currentCrossAxisSize, placeable.height)
        }

        if (currentSequence.isNotEmpty()) startNewSequence()

        val mainAxisLayoutSize = max(mainAxisSpace, constraints.minWidth)

        val crossAxisLayoutSize = max(crossAxisSpace, constraints.minHeight)

        layout(mainAxisLayoutSize, crossAxisLayoutSize) {
            sequences.fastForEachIndexed { i, placeables ->
                val childrenMainAxisSizes =
                    IntArray(placeables.size) { j ->
                        placeables[j].width +
                            if (j < placeables.lastIndex) mainAxisSpacing.roundToPx() else 0
                    }
                val arrangement = Arrangement.End
                val mainAxisPositions = IntArray(childrenMainAxisSizes.size) { 0 }
                with(arrangement) {
                    arrange(
                        mainAxisLayoutSize,
                        childrenMainAxisSizes,
                        layoutDirection,
                        mainAxisPositions,
                    )
                }
                placeables.fastForEachIndexed { j, placeable ->
                    placeable.place(x = mainAxisPositions[j], y = crossAxisPositions[i])
                }
            }
        }
    }
}
