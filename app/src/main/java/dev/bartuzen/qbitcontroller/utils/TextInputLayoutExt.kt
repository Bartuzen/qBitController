package dev.bartuzen.qbitcontroller.utils

import androidx.annotation.StringRes
import com.google.android.material.textfield.TextInputLayout

fun TextInputLayout.setTextWithoutAnimation(text: String?, putCursorToEnd: Boolean = true) {
    isHintAnimationEnabled = false
    editText?.setText(text)
    if (putCursorToEnd) {
        editText?.setSelection(text?.length ?: 0)
    }
    isHintAnimationEnabled = true
}

fun TextInputLayout.setTextWithoutAnimation(@StringRes id: Int, putCursorToEnd: Boolean = true) {
    setTextWithoutAnimation(context.getString(id), putCursorToEnd)
}
