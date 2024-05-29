package dev.bartuzen.qbitcontroller.utils

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.os.Parcelable
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.IntRange
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import com.google.android.material.color.MaterialColors
import com.google.android.material.progressindicator.BaseProgressIndicator
import com.google.android.material.progressindicator.BaseProgressIndicatorSpec
import dev.bartuzen.qbitcontroller.R
import java.io.Serializable
import kotlin.math.ceil

fun Context.getColorCompat(@ColorRes id: Int) = ContextCompat.getColor(this, id)

fun Context.getDrawableCompat(@DrawableRes id: Int) = ContextCompat.getDrawable(this, id)

fun Int.toPx(context: Context) = ceil(this * context.resources.displayMetrics.density).toInt()

fun Int.toDp(context: Context) = ceil(this / context.resources.displayMetrics.density).toInt()

inline fun <reified T : Parcelable> Bundle.getParcelableCompat(name: String) =
    if (VERSION.SDK_INT >= VERSION_CODES.TIRAMISU) {
        getParcelable(name, T::class.java)
    } else {
        @Suppress("DEPRECATION")
        getParcelable(name)
    }

inline fun <reified T : Serializable> Bundle.getSerializableCompat(name: String) =
    if (VERSION.SDK_INT >= VERSION_CODES.TIRAMISU) {
        getSerializable(name, T::class.java)
    } else {
        @Suppress("DEPRECATION")
        getSerializable(name) as? T
    }

val Activity.view: View get() = findViewById<ViewGroup>(android.R.id.content).getChildAt(0)

fun Fragment.requireAppCompatActivity() = requireActivity() as AppCompatActivity

fun FragmentTransaction.setDefaultAnimations() {
    setCustomAnimations(
        R.anim.slide_in_right,
        R.anim.slide_out_left,
        R.anim.slide_in_left,
        R.anim.slide_out_right
    )
}

fun Context.copyToClipboard(text: String, label: String? = null) {
    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText(label, text)
    clipboard.setPrimaryClip(clip)
}

fun <T : BaseProgressIndicatorSpec> BaseProgressIndicator<T>.setColor(color: Int, harmonize: Boolean = true) {
    val indicatorColor = if (harmonize) {
        MaterialColors.harmonizeWithPrimary(context, color)
    } else {
        color
    }

    setIndicatorColor(indicatorColor)
    trackColor = MaterialColors.compositeARGBWithAlpha(indicatorColor, (MaterialColors.ALPHA_DISABLED * 255).toInt())
}

typealias themeColors = com.google.android.material.R.attr

fun Context.getThemeColor(
    color: Int,
    @IntRange(from = 0, to = 255) alpha: Int = 255,
    defaultColor: Int = Color.TRANSPARENT
) = MaterialColors.getColor(this, color, defaultColor).let {
    MaterialColors.compositeARGBWithAlpha(it, alpha)
}
