package dev.bartuzen.qbitcontroller.utils

import androidx.annotation.StringRes
import com.google.android.material.textfield.TextInputLayout

fun TextInputLayout.setTextWithoutAnimation(text: String?, putCursorToEnd: Boolean = true) {
    val editText = editText
    if (editText != null) {
        isHintAnimationEnabled = false
        editText.setText(text)
        if (putCursorToEnd) {
            editText.setSelection(editText.length())
        }
        isHintAnimationEnabled = true
    }
}

fun TextInputLayout.setTextWithoutAnimation(@StringRes id: Int, putCursorToEnd: Boolean = true) {
    setTextWithoutAnimation(context.getString(id), putCursorToEnd)
}

var TextInputLayout.text
    get() = editText!!.text.toString()
    set(value) {
        editText?.setText(value)
    }
