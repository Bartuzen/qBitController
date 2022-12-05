package dev.bartuzen.qbitcontroller.ui.settings

import android.os.Bundle
import androidx.fragment.app.commit
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.preference.ListPreference
import androidx.preference.PreferenceFragmentCompat
import dagger.hilt.android.AndroidEntryPoint
import dev.bartuzen.qbitcontroller.R
import dev.bartuzen.qbitcontroller.ui.settings.addeditserver.AddEditServerFragment
import dev.bartuzen.qbitcontroller.ui.settings.addeditserver.AddEditServerFragmentBuilder
import dev.bartuzen.qbitcontroller.utils.getSerializableCompat
import dev.bartuzen.qbitcontroller.utils.preferences
import dev.bartuzen.qbitcontroller.utils.requireAppCompatActivity
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
                summary = serverConfig.host
                setOnPreferenceClickListener {
                    val fragment = AddEditServerFragmentBuilder()
                        .serverConfig(serverConfig)
                        .build()
                    parentFragmentManager.commit {
                        setReorderingAllowed(true)
                        setCustomAnimations(
                            R.anim.slide_in_right,
                            R.anim.slide_out_left,
                            R.anim.slide_in_left,
                            R.anim.slide_out_right,
                        )
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
                val fragment = AddEditServerFragmentBuilder()
                    .build()
                parentFragmentManager.commit {
                    setReorderingAllowed(true)
                    setCustomAnimations(
                        R.anim.slide_in_right,
                        R.anim.slide_out_left,
                        R.anim.slide_in_left,
                        R.anim.slide_out_right,
                    )
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

        list {
            key = "theme"
            setTitle(R.string.settings_theme)
            summaryProvider = ListPreference.SimpleSummaryProvider.getInstance()
            entries = resources.getStringArray(R.array.settings_theme_entries)
            entryValues = arrayOf("LIGHT", "DARK", "SYSTEM_DEFAULT")
            setDefaultValue("SYSTEM_DEFAULT")
        }
    }

    override fun onResume() {
        super.onResume()
        requireAppCompatActivity().supportActionBar?.setTitle(R.string.settings_title)
    }
}
