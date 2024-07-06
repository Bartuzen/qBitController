package dev.bartuzen.qbitcontroller.ui.settings.addeditserver.advanced

import android.os.Bundle
import android.text.InputType
import android.view.View
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.preference.EditTextPreference
import androidx.preference.Preference.SummaryProvider
import androidx.preference.PreferenceDataStore
import androidx.preference.PreferenceFragmentCompat
import dagger.hilt.android.AndroidEntryPoint
import dev.bartuzen.qbitcontroller.R
import dev.bartuzen.qbitcontroller.model.BasicAuth
import dev.bartuzen.qbitcontroller.utils.PreferenceDSL
import dev.bartuzen.qbitcontroller.utils.applyNavigationBarInsets
import dev.bartuzen.qbitcontroller.utils.getParcelableCompat
import dev.bartuzen.qbitcontroller.utils.preferences

@AndroidEntryPoint
class AdvancedServerSettingsFragment() : PreferenceFragmentCompat() {
    private var basicAuth
        get() = arguments?.getParcelableCompat<BasicAuth>("basicAuth")!!
        set(value) {
            arguments?.putParcelable("basicAuth", value)
        }

    private val simpleSummaryProvider = SummaryProvider<EditTextPreference> { preference ->
        if (preference.text.isNullOrEmpty()) {
            null
        } else {
            preference.text
        }
    }

    private val passwordSummaryProvider = SummaryProvider<EditTextPreference> { preference ->
        val text = preference.text
        if (text.isNullOrEmpty()) {
            null
        } else {
            "*".repeat(text.length)
        }
    }

    constructor(basicAuth: BasicAuth) : this() {
        arguments = bundleOf("basicAuth" to basicAuth)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        listView.applyNavigationBarInsets()
        listView.clipToPadding = false
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceManager.preferenceDataStore = object : PreferenceDataStore() {
            override fun putString(key: String, value: String?) {
                when (key) {
                    "basicAuthUsername" -> basicAuth = basicAuth.copy(username = value.takeIf { it?.isNotBlank() == true })
                    "basicAuthPassword" -> basicAuth = basicAuth.copy(password = value.takeIf { it?.isNotBlank() == true })
                }
            }

            override fun getString(key: String, defValue: String?): String? {
                return when (key) {
                    "basicAuthUsername" -> basicAuth.username
                    "basicAuthPassword" -> basicAuth.password
                    else -> null
                }
            }

            override fun putBoolean(key: String, value: Boolean) {
                when (key) {
                    "basicAuthEnabled" -> basicAuth = basicAuth.copy(isEnabled = value)
                }
            }

            override fun getBoolean(key: String, defValue: Boolean) = when (key) {
                "basicAuthEnabled" -> basicAuth.isEnabled
                else -> null
            } ?: false
        }
        preferences {
            setUpBasicAuth()
        }
    }

    private fun PreferenceDSL.setUpBasicAuth() {
        category {
            setTitle(R.string.settings_server_advanced_basic_auth)
        }

        lateinit var usernamePreference: EditTextPreference
        lateinit var passwordPreference: EditTextPreference

        switch {
            key = "basicAuthEnabled"
            setTitle(R.string.settings_server_advanced_basic_auth_enabled)

            setOnPreferenceChangeListener { _, newValue ->
                val isEnabled = newValue as Boolean

                usernamePreference.isEnabled = isEnabled
                passwordPreference.isEnabled = isEnabled

                true
            }
        }

        editText {
            usernamePreference = this

            key = "basicAuthUsername"
            setTitle(R.string.settings_server_advanced_basic_auth_username)
            setDialogTitle(R.string.settings_server_advanced_basic_auth_username)

            isEnabled = basicAuth.isEnabled == true
            summaryProvider = simpleSummaryProvider
        }

        editText {
            passwordPreference = this

            key = "basicAuthPassword"
            setTitle(R.string.settings_server_advanced_basic_auth_password)
            setDialogTitle(R.string.settings_server_advanced_basic_auth_password)

            isEnabled = basicAuth.isEnabled == true
            summaryProvider = passwordSummaryProvider

            setOnBindEditTextListener { editText ->
                editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                editText.setSelection(editText.length())
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        setFragmentResult(
            requestKey = "advancedServerSettingsResult",
            result = bundleOf("basicAuth" to basicAuth),
        )
    }
}
