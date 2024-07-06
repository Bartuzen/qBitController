package dev.bartuzen.qbitcontroller.utils

import android.view.View
import androidx.annotation.Dimension
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePaddingRelative

fun View.applyInsets(
    applyTop: Boolean,
    applyBottom: Boolean,
    applyStart: Boolean,
    applyEnd: Boolean,
    @Dimension(unit = Dimension.DP) paddingTop: Int,
    @Dimension(unit = Dimension.DP) paddingBottom: Int,
    @Dimension(unit = Dimension.DP) paddingStart: Int,
    @Dimension(unit = Dimension.DP) paddingEnd: Int,
    typeMask: Int,
) {
    ViewCompat.setOnApplyWindowInsetsListener(this) { v, windowInsets ->
        val insets = windowInsets.getInsets(typeMask)
        val isLtr = layoutDirection == View.LAYOUT_DIRECTION_LTR

        val paddingTopPx = paddingTop.toPx(context)
        val paddingBottomPx = paddingBottom.toPx(context)
        val paddingStartPx = paddingStart.toPx(context)
        val paddingEndPx = paddingEnd.toPx(context)

        val top = if (applyTop) paddingTopPx + insets.top else paddingTopPx
        val bottom = if (applyBottom) paddingBottomPx + insets.bottom else paddingBottomPx

        val start = paddingStartPx + if (applyStart) {
            if (isLtr) insets.left else insets.right
        } else {
            0
        }

        val end = paddingEndPx + if (applyEnd) {
            if (isLtr) insets.right else insets.left
        } else {
            0
        }

        v.updatePaddingRelative(
            top = top,
            bottom = bottom,
            start = start,
            end = end,
        )

        windowInsets
    }
}

fun View.applyNavigationBarInsets(
    top: Boolean = false,
    bottom: Boolean = true,
    start: Boolean = true,
    end: Boolean = true,
    @Dimension(unit = Dimension.DP) paddingTop: Int = 0,
    @Dimension(unit = Dimension.DP) paddingBottom: Int = 0,
    @Dimension(unit = Dimension.DP) paddingStart: Int = 0,
    @Dimension(unit = Dimension.DP) paddingEnd: Int = 0,
) {
    applyInsets(
        top,
        bottom,
        start,
        end,
        paddingTop,
        paddingBottom,
        paddingStart,
        paddingEnd,
        WindowInsetsCompat.Type.navigationBars(),
    )
}

fun View.applyNavigationBarInsets(
    top: Boolean = false,
    bottom: Boolean = true,
    start: Boolean = true,
    end: Boolean = true,
    @Dimension(unit = Dimension.DP) padding: Int = 0,
) {
    applyInsets(
        top,
        bottom,
        start,
        end,
        padding,
        padding,
        padding,
        padding,
        WindowInsetsCompat.Type.navigationBars(),
    )
}

fun View.applySystemBarInsets(
    top: Boolean = true,
    bottom: Boolean = false,
    start: Boolean = true,
    end: Boolean = true,
    @Dimension(unit = Dimension.DP) paddingTop: Int = 0,
    @Dimension(unit = Dimension.DP) paddingBottom: Int = 0,
    @Dimension(unit = Dimension.DP) paddingStart: Int = 0,
    @Dimension(unit = Dimension.DP) paddingEnd: Int = 0,
) {
    applyInsets(
        top,
        bottom,
        start,
        end,
        paddingTop,
        paddingBottom,
        paddingStart,
        paddingEnd, WindowInsetsCompat.Type.systemBars(),
    )
}

fun View.applySystemBarInsets(
    top: Boolean = true,
    bottom: Boolean = false,
    start: Boolean = true,
    end: Boolean = true,
    @Dimension(unit = Dimension.DP) padding: Int = 0,
) {
    applyInsets(
        top,
        bottom,
        start,
        end,
        padding,
        padding,
        padding,
        padding,
        WindowInsetsCompat.Type.systemBars(),
    )
}
