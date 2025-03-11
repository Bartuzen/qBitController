package dev.bartuzen.qbitcontroller.utils

import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePaddingRelative

fun View.applyInsets(top: Boolean, bottom: Boolean, start: Boolean, end: Boolean, typeMask: Int) {
    ViewCompat.setOnApplyWindowInsetsListener(this) { v, windowInsets ->
        val insets = windowInsets.getInsets(typeMask)
        val isLtr = layoutDirection == View.LAYOUT_DIRECTION_LTR

        val topPadding = if (top) insets.top else 0
        val bottomPadding = if (bottom) insets.bottom else 0

        val startPadding = if (start) {
            if (isLtr) insets.left else insets.right
        } else {
            0
        }

        val endPadding = if (end) {
            if (isLtr) insets.right else insets.left
        } else {
            0
        }

        v.updatePaddingRelative(
            top = topPadding,
            bottom = bottomPadding,
            start = startPadding,
            end = endPadding,
        )

        windowInsets
    }
}

fun View.applySafeDrawingInsets(top: Boolean = true, bottom: Boolean = true, start: Boolean = true, end: Boolean = true) {
    applyInsets(
        top = top,
        bottom = bottom,
        start = start,
        end = end,
        typeMask = WindowInsetsCompat.Type.systemBars() or
            WindowInsetsCompat.Type.displayCutout() or
            WindowInsetsCompat.Type.ime(),
    )
}
