package dev.bartuzen.qbitcontroller.utils

import android.app.Activity
import android.view.View
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar

fun Activity.showSnackbar(
    text: String,
    @BaseTransientBottomBar.Duration length: Int = Snackbar.LENGTH_LONG,
    view: View = findViewById(android.R.id.content),
) {
    Snackbar.make(view, text, length).show()
}

fun Activity.showSnackbar(
    @StringRes resId: Int,
    @BaseTransientBottomBar.Duration length: Int = Snackbar.LENGTH_LONG,
    view: View = findViewById(android.R.id.content),
) {
    Snackbar.make(view, resId, length).show()
}

fun Fragment.showSnackbar(
    text: String,
    @BaseTransientBottomBar.Duration length: Int = Snackbar.LENGTH_LONG,
    view: View = requireView(),
) {
    Snackbar.make(view, text, length).show()
}

fun Fragment.showSnackbar(
    @StringRes resId: Int,
    @BaseTransientBottomBar.Duration length: Int = Snackbar.LENGTH_LONG,
    view: View = requireView(),
) {
    Snackbar.make(view, resId, length).show()
}
