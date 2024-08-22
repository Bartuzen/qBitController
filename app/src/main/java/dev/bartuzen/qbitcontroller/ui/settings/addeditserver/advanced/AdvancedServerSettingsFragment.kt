package dev.bartuzen.qbitcontroller.ui.settings.addeditserver.advanced

import android.os.Bundle
import android.text.InputType
import android.view.View
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.Preference.SummaryProvider
import androidx.preference.PreferenceDataStore
import androidx.preference.PreferenceFragmentCompat
import dagger.hilt.android.AndroidEntryPoint
import dev.bartuzen.qbitcontroller.R
import dev.bartuzen.qbitcontroller.model.BasicAuth
import dev.bartuzen.qbitcontroller.model.DnsOverHttps
import dev.bartuzen.qbitcontroller.utils.PreferenceDSL
import dev.bartuzen.qbitcontroller.utils.applySystemBarInsets
import dev.bartuzen.qbitcontroller.utils.getParcelableCompat
import dev.bartuzen.qbitcontroller.utils.getSerializableCompat
import dev.bartuzen.qbitcontroller.utils.preferences

@AndroidEntryPoint
class AdvancedServerSettingsFragment() : PreferenceFragmentCompat() {
    private var basicAuth
        get() = arguments?.getParcelableCompat<BasicAuth>("basicAuth")!!
        set(value) {
            arguments?.putParcelable("basicAuth", value)
        }

    private var dnsOverHttps
        get() = arguments?.getSerializableCompat<DnsOverHttps>("dnsOverHttps")
        set(value) {
            arguments?.putSerializable("dnsOverHttps", value)
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

    constructor(basicAuth: BasicAuth, dnsOverHttps: DnsOverHttps?) : this() {
        arguments = bundleOf(
            "basicAuth" to basicAuth,
            "dnsOverHttps" to dnsOverHttps,
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        listView.applySystemBarInsets(top = false)
        listView.clipToPadding = false
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceManager.preferenceDataStore = object : PreferenceDataStore() {
            override fun putString(key: String, value: String?) {
                when (key) {
                    "basicAuthUsername" -> basicAuth = basicAuth.copy(username = value.takeIf { it?.isNotBlank() == true })
                    "basicAuthPassword" -> basicAuth = basicAuth.copy(password = value.takeIf { it?.isNotBlank() == true })
                    "dnsOverHttps" -> dnsOverHttps = value?.takeIf { it != "Disabled" }?.let { DnsOverHttps.valueOf(it) }
                }
            }

            override fun getString(key: String, defValue: String?): String? {
                return when (key) {
                    "basicAuthUsername" -> basicAuth.username
                    "basicAuthPassword" -> basicAuth.password
                    "dnsOverHttps" -> dnsOverHttps?.name ?: "Disabled"
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

        divider()

        list {
            key = "dnsOverHttps"
            setTitle(R.string.settings_server_advanced_doh)
            setDialogTitle(R.string.settings_server_advanced_doh)

            entries = listOf(
                R.string.settings_disabled,
                R.string.settings_server_advanced_doh_cloudflare,
                R.string.settings_server_advanced_doh_google,
                R.string.settings_server_advanced_doh_adguard,
                R.string.settings_server_advanced_doh_quad9,
                R.string.settings_server_advanced_doh_alidns,
                R.string.settings_server_advanced_doh_dnspod,
                R.string.settings_server_advanced_doh_360,
                R.string.settings_server_advanced_doh_quad101,
                R.string.settings_server_advanced_doh_mullvad,
                R.string.settings_server_advanced_doh_controld,
                R.string.settings_server_advanced_doh_njalla,
                R.string.settings_server_advanced_doh_shecan,
            ).map { context.getString(it) }.toTypedArray()

            entryValues = arrayOf("Disabled") + DnsOverHttps.entries.map { it.name }.toTypedArray()

            summaryProvider = SummaryProvider<ListPreference> { it.entry }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        setFragmentResult(
            requestKey = "advancedServerSettingsResult",
            result = bundleOf(
                "basicAuth" to basicAuth,
                "dnsOverHttps" to dnsOverHttps,
            ),
        )
    }
}
