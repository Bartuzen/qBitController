package dev.bartuzen.qbitcontroller.ui.settings

import android.os.Bundle
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.preference.*
import dagger.hilt.android.AndroidEntryPoint
import dev.bartuzen.qbitcontroller.R
import dev.bartuzen.qbitcontroller.data.dataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

@AndroidEntryPoint
class SettingsFragment : PreferenceFragmentCompat() {
    private val viewModel: SettingsViewModel by viewModels(
        ownerProducer = { requireActivity() }
    )

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceManager.preferenceDataStore =
            SettingsDataStore(preferenceManager.context.dataStore)

        initSettings()

        lifecycleScope.launchWhenStarted {
            viewModel.settingsFragmentEvent.collect { event ->
                when (event) {
                    is SettingsViewModel.SettingsFragmentEvent.AddEditServerCompleted ->
                        initSettings()
                }
            }
        }
    }

    private fun initSettings() {
        val context = preferenceManager.context
        val screen = preferenceManager.createPreferenceScreen(context)

        val servers = viewModel.getServers()

        screen += PreferenceCategory(context).apply {
            setTitle(R.string.settings_servers)
            initialExpandedChildrenCount = servers.size + 1
        }

        servers.forEach { (_, serverConfig) ->
            screen += Preference(context).apply {
                title = serverConfig.name
                summary = serverConfig.host
                setOnPreferenceClickListener {
                    val fragment = AddEditServerFragmentBuilder()
                        .serverConfig(serverConfig)
                        .build()
                    viewModel.movePage(fragment)
                    true
                }
            }
        }

        screen += Preference(context).apply {
            setTitle(R.string.settings_add_new_server)
            setOnPreferenceClickListener {
                val fragment = AddEditServerFragmentBuilder()
                    .build()
                viewModel.movePage(fragment)
                true
            }
        }

        screen += PreferenceCategory(context).apply {
            setTitle(R.string.settings_other)
            initialExpandedChildrenCount = 1
        }

        screen += ListPreference(context).apply {
            key = "theme"
            setTitle(R.string.settings_theme)
            summaryProvider = ListPreference.SimpleSummaryProvider.getInstance()
            entries = resources.getStringArray(R.array.settings_theme_entries)
            entryValues = arrayOf("LIGHT", "DARK", "SYSTEM_DEFAULT")
            setDefaultValue("SYSTEM_DEFAULT")
        }

        preferenceScreen = screen
    }

    class SettingsDataStore(private val dataStore: DataStore<Preferences>) : PreferenceDataStore() {

        override fun putString(key: String, value: String?) {
            CoroutineScope(Dispatchers.IO).launch {
                dataStore.edit {
                    it[stringPreferencesKey(key)] = value!!
                }
            }
        }

        override fun getString(key: String, defValue: String?) = runBlocking {
            dataStore.data.map {
                val pref = it[stringPreferencesKey(key)]
                if (pref != null) {
                    pref
                } else {
                    dataStore.edit { settings ->
                        settings[stringPreferencesKey(key)] = defValue!!
                    }
                    defValue!!
                }
            }.first()
        }
    }
}