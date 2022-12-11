package dev.bartuzen.qbitcontroller.utils

import android.app.Activity
import android.widget.Toast
import androidx.annotation.StringRes
import com.google.android.material.snackbar.BaseTransientBottomBar

fun Activity.showToast(text: String, @BaseTransientBottomBar.Duration length: Int = Toast.LENGTH_LONG) {
    Toast.makeText(this, text, length).show()
}

fun Activity.showToast(@StringRes resId: Int, @BaseTransientBottomBar.Duration length: Int = Toast.LENGTH_LONG) {
    Toast.makeText(this, resId, length).show()
}
