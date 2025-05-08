package dev.bartuzen.qbitcontroller.utils

import android.content.Intent
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.Parcelable

inline fun <reified T : Parcelable> Intent.getParcelableArrayListExtraCompat(name: String) =
    if (VERSION.SDK_INT >= VERSION_CODES.TIRAMISU) {
        getParcelableArrayListExtra(name, T::class.java)
    } else {
        @Suppress("DEPRECATION")
        getParcelableArrayListExtra(name)
    }

inline fun <reified T : Parcelable> Intent.getParcelableExtraCompat(name: String) =
    if (VERSION.SDK_INT >= VERSION_CODES.TIRAMISU) {
        getParcelableExtra(name, T::class.java)
    } else {
        @Suppress("DEPRECATION")
        getParcelableExtra(name) as? T
    }
