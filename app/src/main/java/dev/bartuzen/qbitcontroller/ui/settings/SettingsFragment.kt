package dev.bartuzen.qbitcontroller.ui.settings

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.text.InputType
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.NotificationManagerCompat
import androidx.fragment.app.commit
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.preference.EditTextPreference
import androidx.preference.PreferenceDataStore
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.get
import dagger.hilt.android.AndroidEntryPoint
import dev.bartuzen.qbitcontroller.R
import dev.bartuzen.qbitcontroller.data.Theme
import dev.bartuzen.qbitcontroller.ui.settings.addeditserver.AddEditServerFragment
import dev.bartuzen.qbitcontroller.utils.getSerializableCompat
import dev.bartuzen.qbitcontroller.utils.launchAndCollectLatestIn
import dev.bartuzen.qbitcontroller.utils.preferences
import dev.bartuzen.qbitcontroller.utils.requireAppCompatActivity
import dev.bartuzen.qbitcontroller.utils.setDefaultAnimations
import dev.bartuzen.qbitcontroller.utils.showSnackbar

@AndroidEntryPoint
class SettingsFragment : PreferenceFragmentCompat() {
    private val viewModel: SettingsViewModel by viewModels()

    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceManager.preferenceDataStore = object : PreferenceDataStore() {
            override fun getString(key: String, defValue: String?): String? {
                return when (key) {
                    "connectionTimeout" -> viewModel.connectionTimeout.toString()
                    "autoRefreshInterval" -> viewModel.autoRefreshInterval.toString()
                    "notificationCheckInterval" -> viewModel.notificationCheckInterval.toString()
                    "theme" -> viewModel.theme.toString()
                    else -> defValue
                }
            }

            override fun putString(key: String, value: String?) {
                when (key) {
                    "connectionTimeout" -> {
                        val num = value?.toIntOrNull() ?: return
                        viewModel.connectionTimeout = when {
                            num > 3600 -> 3600
                            num < 1 -> 1
                            else -> num
                        }
                    }
                    "autoRefreshInterval" -> {
                        val num = value?.toIntOrNull() ?: return
                        viewModel.autoRefreshInterval = when {
                            num > 3600 -> 3600
                            num < 0 -> 0
                            else -> num
                        }
                    }
                    "notificationCheckInterval" -> {
                        val num = value?.toIntOrNull() ?: return
                        viewModel.notificationCheckInterval = when {
                            num > 24 * 60 -> 24 * 60
                            num < 1 -> 1
                            else -> num
                        }
                    }
                    "theme" -> {
                        if (value != null) {
                            viewModel.theme = Theme.valueOf(value)
                        }
                    }
                }
            }

            override fun getBoolean(key: String, defValue: Boolean): Boolean {
                return when (key) {
                    "autoRefreshHideLoadingBar" -> viewModel.autoRefreshHideLoadingBar
                    "areTorrentSwipeActionsEnabled" -> viewModel.areTorrentSwipeActionsEnabled
                    else -> defValue
                }
            }

            override fun putBoolean(key: String, value: Boolean) {
                when (key) {
                    "autoRefreshHideLoadingBar" -> viewModel.autoRefreshHideLoadingBar = value
                    "areTorrentSwipeActionsEnabled" -> viewModel.areTorrentSwipeActionsEnabled = value
                }
            }
        }

        initSettings()

        setFragmentResultListener("addEditServerResult") { _, bundle ->
            when (bundle.getSerializableCompat<AddEditServerFragment.Result>("result")) {
                AddEditServerFragment.Result.ADDED -> {
                    showSnackbar(R.string.settings_server_add_success)
                    initSettings()

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        if (!shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
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
        }

        servers.forEach { (_, serverConfig) ->
            preference {
                title = serverConfig.name
                summary = serverConfig.visibleUrl
                setOnPreferenceClickListener {
                    val fragment = AddEditServerFragment(serverConfig.id)
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
        }

        editText {
            key = "connectionTimeout"
            setTitle(R.string.settings_connection_timeout)
            setDialogTitle(R.string.settings_connection_timeout)

            setSummaryProvider {
                resources.getQuantityString(
                    R.plurals.settings_connection_timeout_description,
                    viewModel.connectionTimeout,
                    viewModel.connectionTimeout
                )
            }

            setOnBindEditTextListener { editText ->
                val text = viewModel.connectionTimeout.toString()
                editText.inputType = InputType.TYPE_CLASS_NUMBER
                editText.setText(text)
                editText.setSelection(text.length)
            }
        }

        editText {
            key = "autoRefreshInterval"
            setTitle(R.string.settings_auto_refresh_interval)
            setDialogTitle(R.string.settings_auto_refresh_interval)

            setSummaryProvider {
                if (viewModel.autoRefreshInterval == 0) {
                    getString(R.string.settings_disabled)
                } else {
                    resources.getQuantityString(
                        R.plurals.settings_auto_refresh_interval_description,
                        viewModel.autoRefreshInterval,
                        viewModel.autoRefreshInterval
                    )
                }
            }

            setOnBindEditTextListener { editText ->
                val text = viewModel.autoRefreshInterval.toString()
                editText.inputType = InputType.TYPE_CLASS_NUMBER
                editText.setText(text)
                editText.setSelection(text.length)
            }
        }

        switch {
            key = "autoRefreshHideLoadingBar"
            setTitle(R.string.settings_auto_refresh_hide_loading_bar)
            setSummary(R.string.settings_auto_refresh_hide_loading_bar_description)

            viewModel.autoRefreshIntervalFlow.launchAndCollectLatestIn(this@SettingsFragment) { autoRefreshInterval ->
                isEnabled = autoRefreshInterval != 0
            }
        }

        editText {
            key = "notificationCheckInterval"
            setTitle(R.string.settings_notification_check_interval)
            setDialogTitle(R.string.settings_notification_check_interval)

            isEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()

            setSummaryProvider {
                resources.getQuantityString(
                    R.plurals.settings_notification_check_interval_description,
                    viewModel.notificationCheckInterval,
                    viewModel.notificationCheckInterval
                )
            }

            setOnBindEditTextListener { editText ->
                val text = viewModel.notificationCheckInterval.toString()
                editText.inputType = InputType.TYPE_CLASS_NUMBER
                editText.setText(text)
                editText.setSelection(text.length)
            }
        }

        switch {
            key = "areTorrentSwipeActionsEnabled"
            setTitle(R.string.settings_enable_torrent_swipe_actions)
        }

        list {
            key = "theme"
            setTitle(R.string.settings_theme)
            setDialogTitle(R.string.settings_theme)
            entries = arrayOf(
                context.getString(R.string.settings_theme_light),
                context.getString(R.string.settings_theme_dark),
                context.getString(R.string.settings_theme_system_default)
            )
            entryValues = arrayOf("LIGHT", "DARK", "SYSTEM_DEFAULT")

            setSummaryProvider {
                entries[viewModel.theme.ordinal]
            }
        }
    }

    override fun onResume() {
        super.onResume()
        requireAppCompatActivity().supportActionBar?.setTitle(R.string.settings_title)

        preferenceScreen.get<EditTextPreference>("notificationCheckInterval")?.isEnabled =
            NotificationManagerCompat.from(requireContext()).areNotificationsEnabled()
    }
}
