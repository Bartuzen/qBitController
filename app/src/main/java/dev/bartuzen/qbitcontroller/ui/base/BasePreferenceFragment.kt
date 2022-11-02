package dev.bartuzen.qbitcontroller.ui.base

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import com.hannesdorfmann.fragmentargs.FragmentArgs

abstract class BasePreferenceFragment : PreferenceFragmentCompat() {
    override fun onCreate(savedInstanceState: Bundle?) {
        FragmentArgs.inject(this)
        super.onCreate(savedInstanceState)
    }

    abstract override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?)
}
