package dev.bartuzen.qbitcontroller.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.os.Parcelable
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import java.io.Serializable
import kotlin.math.ceil

fun Activity.showToast(@StringRes resId: Int) {
    Toast.makeText(this, resId, Toast.LENGTH_LONG).show()
}

fun Activity.showSnackbar(text: String) {
    Snackbar.make(findViewById(android.R.id.content), text, Snackbar.LENGTH_LONG).show()
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

fun Context.getColorCompat(@ColorRes id: Int) = ContextCompat.getColor(this, id)

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

fun Int.toPx(context: Context) = ceil(this * context.resources.displayMetrics.density).toInt()

fun Int.toDp(context: Context) = ceil(this / context.resources.displayMetrics.density).toInt()

inline fun <reified T : Parcelable> Intent.getParcelable(name: String) =
    if (VERSION.SDK_INT >= VERSION_CODES.TIRAMISU) {
        getParcelableExtra(name, T::class.java)
    } else {
        @Suppress("DEPRECATION")
        getParcelableExtra(name)
    }

inline fun <reified T : Serializable> Bundle.getSerializableCompat(name: String) =
    if (VERSION.SDK_INT >= VERSION_CODES.TIRAMISU) {
        getSerializable(name, T::class.java)
    } else {
        @Suppress("DEPRECATION")
        getSerializable(name)
    }

val Activity.view: View get() = findViewById<ViewGroup>(android.R.id.content).getChildAt(0)
