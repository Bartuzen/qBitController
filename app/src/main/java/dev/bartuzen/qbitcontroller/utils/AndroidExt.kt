package dev.bartuzen.qbitcontroller.utils

import android.app.Activity
import android.content.Context
import android.graphics.Rect
import android.util.DisplayMetrics
import android.view.View
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import kotlin.math.ceil

fun Activity.showToast(@StringRes resId: Int) {
    Toast.makeText(this, resId, Toast.LENGTH_LONG).show()
}

fun Fragment.showSnackbar(text: String, view: View? = this.view) {
    view?.let {
        Snackbar.make(requireContext(), it, text, Snackbar.LENGTH_LONG).show()
    }
}

fun Fragment.showSnackbar(@StringRes resId: Int, view: View? = this.view) {
    view?.let {
        Snackbar.make(it, resId, Snackbar.LENGTH_LONG).show()
    }
}

fun RecyclerView.setItemMargin(vertical: Int, horizontal: Int) {
    addItemDecoration(object : RecyclerView.ItemDecoration() {
        override fun getItemOffsets(
            outRect: Rect,
            view: View,
            parent: RecyclerView,
            state: RecyclerView.State
        ) {
            outRect.apply {
                val verticalPx = vertical.toPx(context)
                val horizontalPx = (horizontal / 2).toPx(context)
                if (parent.getChildAdapterPosition(view) == 0) {
                    top = verticalPx
                }
                bottom = verticalPx
                left = horizontalPx
                right = horizontalPx
            }
        }
    })
}

fun Int.toPx(context: Context): Int =
    ceil(this * (context.resources.displayMetrics.densityDpi.toDouble() / DisplayMetrics.DENSITY_DEFAULT)).toInt()