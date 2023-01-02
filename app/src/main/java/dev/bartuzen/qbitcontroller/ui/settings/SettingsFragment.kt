package dev.bartuzen.qbitcontroller.ui.settings

import android.os.Bundle
import android.text.InputType
import androidx.fragment.app.commit
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.preference.PreferenceFragmentCompat
import dagger.hilt.android.AndroidEntryPoint
import dev.bartuzen.qbitcontroller.R
import dev.bartuzen.qbitcontroller.data.Theme
import dev.bartuzen.qbitcontroller.ui.settings.addeditserver.AddEditServerFragment
import dev.bartuzen.qbitcontroller.utils.getSerializableCompat
import dev.bartuzen.qbitcontroller.utils.preferences
import dev.bartuzen.qbitcontroller.utils.requireAppCompatActivity
import dev.bartuzen.qbitcontroller.utils.setDefaultAnimations
import dev.bartuzen.qbitcontroller.utils.showSnackbar

@AndroidEntryPoint
class SettingsFragment : PreferenceFragmentCompat() {
    private val viewModel: SettingsViewModel by viewModels()

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceManager.sharedPreferencesName = "settings"
        initSettings()

        setFragmentResultListener("addEditServerResult") { _, bundle ->
            when (bundle.getSerializableCompat<AddEditServerFragment.Result>("result")) {
                AddEditServerFragment.Result.ADDED -> {
                    showSnackbar(R.string.settings_server_add_success)
                    initSettings()
                }
                AddEditServerFragment.Result.EDITED -> {
                    showSnackbar(R.string.settings_server_edit_success)
                    initSettings()
                }
                AddEditServerFragment.Result.DELETED -> {
                    showSnackbar(R.string.settings_server_remove_success)
                    initSettings()
                }
                null -> {}
            }
        }
    }

    private fun initSettings() = preferences {
        val servers = viewModel.getServers()

        category {
            setTitle(R.string.settings_servers)
            initialExpandedChildrenCount = servers.size + 1
        }

        servers.forEach { (_, serverConfig) ->
            preference {
                title = serverConfig.name
                summary = serverConfig.urlWithoutProtocol
                setOnPreferenceClickListener {
                    val fragment = AddEditServerFragment(serverConfig)
                    parentFragmentManager.commit {
                        setReorderingAllowed(true)
                        setDefaultAnimations()
                        replace(R.id.container, fragment)
                        addToBackStack(null)
                    }
                    true
                }
            }
        }

        preference {
            setTitle(R.string.settings_add_new_server)
            setOnPreferenceClickListener {
                val fragment = AddEditServerFragment()
                parentFragmentManager.commit {
                    setReorderingAllowed(true)
                    setDefaultAnimations()
                    replace(R.id.container, fragment)
                    addToBackStack(null)
                }
                true
            }
        }

        category {
            setTitle(R.string.settings_other)
            initialExpandedChildrenCount = 1
        }

        editText {
            key = "preferenceConnectionTimeout"
            setTitle(R.string.settings_connection_timeout)
            setDialogTitle(R.string.settings_connection_timeout)
            summary = resources.getQuantityString(
                R.plurals.settings_connection_timeout_desc,
                viewModel.connectionTimeout,
                viewModel.connectionTimeout
            )
            text = viewModel.connectionTimeout.toString()

            setOnBindEditTextListener { editText ->
                editText.inputType = InputType.TYPE_CLASS_NUMBER
                editText.setSelection(editText.text.length)
            }

            setOnPreferenceChangeListener { _, newValue ->
                val requestTimeout = newValue.toString().toIntOrNull() ?: -1
                if (requestTimeout in 1..3600) {
                    viewModel.connectionTimeout = requestTimeout

                    summary = resources.getQuantityString(
                        R.plurals.settings_connection_timeout_desc,
                        requestTimeout,
                        requestTimeout
                    )
                    text = requestTimeout.toString()
                }

                false
            }
        }

        list {
            key = "preferenceTheme"
            setTitle(R.string.settings_theme)
            setDialogTitle(R.string.settings_theme)
            entries = resources.getStringArray(R.array.settings_theme_entries)
            entryValues = arrayOf("LIGHT", "DARK", "SYSTEM_DEFAULT")
            value = viewModel.theme.name
            summary = resources.getStringArray(R.array.settings_theme_entries)[viewModel.theme.ordinal]

            setOnPreferenceChangeListener { _, newValue ->
                val theme = Theme.valueOf(newValue.toString())
                viewModel.theme = theme

                summary = resources.getStringArray(R.array.settings_theme_entries)[theme.ordinal]
                value = newValue.toString()
                false
            }
        }
    }

    override fun onResume() {
        super.onResume()
        requireAppCompatActivity().supportActionBar?.setTitle(R.string.settings_title)
    }
}
