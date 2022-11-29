package dev.bartuzen.qbitcontroller.utils

import androidx.annotation.StringRes
import com.google.android.material.textfield.TextInputLayout

fun TextInputLayout.setTextWithoutAnimation(text: String?) {
    isHintAnimationEnabled = false
    editText?.setText(text)
    isHintAnimationEnabled = true
}

fun TextInputLayout.setTextWithoutAnimation(@StringRes id: Int) {
    setTextWithoutAnimation(context.getString(id))
}
