package dev.bartuzen.qbitcontroller.utils

import androidx.preference.CheckBoxPreference
import androidx.preference.DropDownPreference
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.MultiSelectListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SeekBarPreference
import androidx.preference.SwitchPreference

class PreferenceDSL(private val fragment: PreferenceFragmentCompat) {
    private val context = fragment.preferenceManager.context
    private val screen = fragment.preferenceManager.createPreferenceScreen(context)

    fun preference(block: Preference.() -> Unit) {
        screen.addPreference(Preference(context).apply(block))
    }

    fun category(block: PreferenceCategory.() -> Unit) {
        screen.addPreference(PreferenceCategory(context).apply(block))
    }

    fun checkBox(block: CheckBoxPreference.() -> Unit) {
        screen.addPreference(CheckBoxPreference(context).apply(block))
    }

    fun dropDown(block: DropDownPreference.() -> Unit) {
        screen.addPreference(DropDownPreference(context).apply(block))
    }

    fun editText(block: EditTextPreference.() -> Unit) {
        screen.addPreference(EditTextPreference(context).apply(block))
    }

    fun list(block: ListPreference.() -> Unit) {
        screen.addPreference(ListPreference(context).apply(block))
    }

    fun multiSelectList(block: MultiSelectListPreference.() -> Unit) {
        screen.addPreference(MultiSelectListPreference(context).apply(block))
    }

    fun seekBar(block: SeekBarPreference.() -> Unit) {
        screen.addPreference(SeekBarPreference(context).apply(block))
    }

    fun switch(block: SwitchPreference.() -> Unit) {
        screen.addPreference(SwitchPreference(context).apply(block))
    }

    fun build() {
        fragment.preferenceScreen = screen
    }
}

fun PreferenceFragmentCompat.preferences(block: PreferenceDSL.() -> Unit) =
    PreferenceDSL(this).apply(block).build()
