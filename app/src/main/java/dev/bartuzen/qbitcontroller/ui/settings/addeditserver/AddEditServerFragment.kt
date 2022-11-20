package dev.bartuzen.qbitcontroller.ui.settings.addeditserver

import android.os.Bundle
import android.text.InputType
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.core.os.bundleOf
import androidx.core.view.MenuProvider
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceDataStore
import com.hannesdorfmann.fragmentargs.annotation.Arg
import com.hannesdorfmann.fragmentargs.annotation.FragmentWithArgs
import dagger.hilt.android.AndroidEntryPoint
import dev.bartuzen.qbitcontroller.R
import dev.bartuzen.qbitcontroller.model.ServerConfig
import dev.bartuzen.qbitcontroller.ui.base.BasePreferenceFragment
import dev.bartuzen.qbitcontroller.utils.preferences
import dev.bartuzen.qbitcontroller.utils.showSnackbar
import dev.bartuzen.qbitcontroller.utils.toAsterisks

@FragmentWithArgs
@AndroidEntryPoint
class AddEditServerFragment : BasePreferenceFragment() {
    private val viewModel: AddEditServerViewModel by viewModels()

    private lateinit var dataStore: AddEditDataStore

    @Arg(required = false)
    var serverConfig: ServerConfig? = null

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) = preferences {
        dataStore = AddEditDataStore().apply {
            serverConfig?.let { config ->
                fromServerConfig(config)
            }
        }
        preferenceManager.preferenceDataStore = dataStore

        editText {
            key = "name"
            setTitle(R.string.settings_torrent_name)
            setDialogTitle(R.string.settings_torrent_name)
            summaryProvider = customSummaryProvider
            isSingleLineTitle = true
        }

        editText {
            key = "host"
            setTitle(R.string.settings_host)
            setDialogTitle(R.string.settings_host)
            summaryProvider = customSummaryProvider
            isSingleLineTitle = true
        }

        editText {
            key = "username"
            setTitle(R.string.settings_username)
            setDialogTitle(R.string.settings_username)
            summaryProvider = customSummaryProvider
            isSingleLineTitle = true
        }

        editText {
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
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.add_edit_server_menu, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                when (menuItem.itemId) {
                    R.id.menu_save -> {
                        saveServerConfig()
                    }
                    R.id.menu_delete -> {
                        deleteServerConfig()
                    }
                    else -> return false
                }
                return true
            }

            override fun onPrepareMenu(menu: Menu) {
                if (serverConfig == null) {
                    menu.findItem(R.id.menu_delete).isVisible = false
                }
            }
        }, viewLifecycleOwner)
    }

    private fun saveServerConfig() {
        val host = dataStore.getString("host", "")
        val username = dataStore.getString("username", "")
        val password = dataStore.getString("password", "")
        val name = dataStore.getString("name", "")

        if (host.isNotBlank() && username.isNotBlank() && password.isNotBlank()) {
            if (serverConfig == null) {
                val config = ServerConfig(-1, host, username, password, name.ifEmpty { null })
                viewModel.addServer(config).invokeOnCompletion {
                    setFragmentResult(
                        "addEditServerResult", bundleOf("result" to Result.ADDED)
                    )
                    parentFragmentManager.popBackStack()
                }
            } else {
                serverConfig?.let { config ->
                    dataStore.toServerConfig(config)
                    viewModel.editServer(config).invokeOnCompletion {
                        setFragmentResult(
                            "addEditServerResult", bundleOf("result" to Result.EDITED)
                        )
                        parentFragmentManager.popBackStack()
                    }
                }
            }
        } else {
            showSnackbar(R.string.settings_fill_blank_fields)
        }
    }

    private fun deleteServerConfig() {
        serverConfig?.let { config ->
            viewModel.removeServer(config).invokeOnCompletion {
                setFragmentResult(
                    "addEditServerResult", bundleOf("result" to Result.DELETED)
                )
                parentFragmentManager.popBackStack()
            }
        }
    }

    private val customSummaryProvider =
        Preference.SummaryProvider<EditTextPreference> { preference ->
            if (preference.key != "password")
                preference.text
            else
                preference.text?.toAsterisks()
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
            name = serverConfig.name ?: ""
            host = serverConfig.host
            username = serverConfig.username
            password = serverConfig.password
        }

        fun toServerConfig(serverConfig: ServerConfig) {
            serverConfig.name = name.ifBlank { null }
            serverConfig.host = host
            serverConfig.username = username
            serverConfig.password = password
        }
    }

    enum class Result {
        ADDED, EDITED, DELETED
    }
}
