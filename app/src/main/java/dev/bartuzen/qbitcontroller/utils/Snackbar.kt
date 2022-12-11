package dev.bartuzen.qbitcontroller.utils

import android.app.Activity
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar

fun Activity.showSnackbar(text: String, @BaseTransientBottomBar.Duration length: Int = Snackbar.LENGTH_LONG) {
    Snackbar.make(findViewById(android.R.id.content), text, length).show()
}

fun Activity.showSnackbar(@StringRes resId: Int, @BaseTransientBottomBar.Duration length: Int = Snackbar.LENGTH_LONG) {
    Snackbar.make(findViewById(android.R.id.content), resId, length).show()
}

fun Fragment.showSnackbar(text: String, @BaseTransientBottomBar.Duration length: Int = Snackbar.LENGTH_LONG) {
    view?.let { view ->
        Snackbar.make(requireContext(), view, text, length).show()
    }
}

fun Fragment.showSnackbar(@StringRes resId: Int, @BaseTransientBottomBar.Duration length: Int = Snackbar.LENGTH_LONG) {
    view?.let { view ->
        Snackbar.make(view, resId, length).show()
    }
}
