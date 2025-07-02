package dev.bartuzen.qbitcontroller.preferences

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp

@Composable
internal fun PaddingValues.copy(
    horizontal: Dp = Dp.Unspecified,
    vertical: Dp = Dp.Unspecified,
): PaddingValues = copy(start = horizontal, top = vertical, end = horizontal, bottom = vertical)

@Composable
internal fun PaddingValues.copy(
    start: Dp = Dp.Unspecified,
    top: Dp = Dp.Unspecified,
    end: Dp = Dp.Unspecified,
    bottom: Dp = Dp.Unspecified,
): PaddingValues  {
    val layoutDirection = LocalLayoutDirection.current
    return PaddingValues(
        start = if (start != Dp.Unspecified) start else calculateStartPadding(layoutDirection),
        top = if (top != Dp.Unspecified) top else calculateTopPadding(),
        end = if (end != Dp.Unspecified) end else calculateRightPadding(layoutDirection),
        bottom = if (bottom != Dp.Unspecified) bottom else calculateBottomPadding()
    )
}
