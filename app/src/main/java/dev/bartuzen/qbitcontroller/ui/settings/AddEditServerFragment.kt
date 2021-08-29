package dev.bartuzen.qbitcontroller.ui.settings

import android.os.Bundle
import android.text.InputType
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.fragment.app.viewModels
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceDataStore
import androidx.preference.plusAssign
import com.hannesdorfmann.fragmentargs.annotation.Arg
import com.hannesdorfmann.fragmentargs.annotation.FragmentWithArgs
import dagger.hilt.android.AndroidEntryPoint
import dev.bartuzen.qbitcontroller.R
import dev.bartuzen.qbitcontroller.model.ServerConfig
import dev.bartuzen.qbitcontroller.ui.base.BasePreferenceFragment
import dev.bartuzen.qbitcontroller.utils.showSnackbar
import dev.bartuzen.qbitcontroller.utils.toAsterisks

@FragmentWithArgs
@AndroidEntryPoint
class AddEditServerFragment : BasePreferenceFragment() {
    private val viewModel: SettingsViewModel by viewModels(
        ownerProducer = { requireActivity() }
    )

    private lateinit var dataStore: AddEditDataStore

    @Arg(required = false)
    var serverConfig: ServerConfig? = null

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        dataStore = AddEditDataStore().apply {
            serverConfig?.let { config ->
                fromServerConfig(config)
            }
        }
        preferenceManager.preferenceDataStore = dataStore

        setHasOptionsMenu(true)

        val context = preferenceManager.context
        val screen = preferenceManager.createPreferenceScreen(context)

        screen += EditTextPreference(context).apply {
            key = "name"
            setTitle(R.string.settings_torrent_name)
            setDialogTitle(R.string.settings_torrent_name)
            summaryProvider = customSummaryProvider
            isSingleLineTitle = true
        }

        screen += EditTextPreference(context).apply {
            key = "host"
            setTitle(R.string.settings_host)
            setDialogTitle(R.string.settings_host)
            summaryProvider = customSummaryProvider
            isSingleLineTitle = true
        }

        screen += EditTextPreference(context).apply {
            key = "username"
            setTitle(R.string.settings_username)
            setDialogTitle(R.string.settings_username)
            summaryProvider = customSummaryProvider
            isSingleLineTitle = true
        }

        screen += EditTextPreference(context).apply {
            key = "password"
            setTitle(R.string.settings_password)
            setDialogTitle(R.string.settings_password)
            summaryProvider = customSummaryProvider
            isSingleLineTitle = true
            setOnBindEditTextListener { editText ->
                editText.inputType =
                    InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            }
        }

        preferenceScreen = screen
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.add_edit_server_menu, menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        if (serverConfig == null) {
            menu.findItem(R.id.menu_delete).isVisible = false
        }
    }

    override fun onOptionsItemSelected(item: MenuItem) =
        when (item.itemId) {
            R.id.menu_save -> {
                val host = dataStore.getString("host", "")
                val username = dataStore.getString("username", "")
                val password = dataStore.getString("password", "")
                val name = dataStore.getString("name", "")

                val view = requireActivity().findViewById<View>(android.R.id.content)
                if (host.isNotBlank() && username.isNotBlank() && password.isNotBlank()) {
                    if (serverConfig == null) {
                        val config = ServerConfig(
                            -1,
                            host,
                            username,
                            password,
                            if (name.isNotBlank()) name else getString(R.string.settings_default_server_name)
                        )
                        viewModel.addServer(config)
                        showSnackbar(R.string.settings_server_add_success, view)
                    } else {
                        serverConfig?.let { config ->
                            dataStore.toServerConfig(config)
                            viewModel.editServer(config)
                        }
                        showSnackbar(R.string.settings_server_edit_success, view)
                    }
                } else {
                    showSnackbar(R.string.settings_fill_blank_fields, view)
                }
                true
            }
            R.id.menu_delete -> {
                serverConfig?.let { config ->
                    viewModel.removeServer(config)
                    val view = requireActivity().findViewById<View>(R.id.content)
                    showSnackbar(R.string.settings_server_remove_success, view)
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }


    private val customSummaryProvider =
        Preference.SummaryProvider<EditTextPreference> { preference ->
            if (preference.key != "password")
                preference.text
            else
                preference.text.toAsterisks()
        }

    inner class AddEditDataStore : PreferenceDataStore() {
        private var name: String = ""
        private var host: String = ""
        private var username: String = ""
        private var password: String = ""

        override fun putString(key: String?, value: String?) {
            val nonNullValue = value ?: ""

            when (key) {
                "name" -> name = nonNullValue
                "host" -> host = nonNullValue
                "username" -> username = nonNullValue
                "password" -> password = nonNullValue
            }
        }

        override fun getString(key: String?, defValue: String?) =
            when (key) {
                "name" -> name
                "host" -> host
                "username" -> username
                "password" -> password
                else -> ""
            }

        fun fromServerConfig(serverConfig: ServerConfig) {
            name = serverConfig.name
            host = serverConfig.host
            username = serverConfig.username
            password = serverConfig.password
        }

        fun toServerConfig(serverConfig: ServerConfig) {
            serverConfig.name =
                if (name.isNotBlank()) name else getString(R.string.settings_default_server_name)
            serverConfig.host = host
            serverConfig.username = username
            serverConfig.password = password
        }
    }
}