package dev.bartuzen.qbitcontroller.ui.settings.addeditserver

import android.app.Activity
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import androidx.core.os.bundleOf
import androidx.core.view.MenuProvider
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import by.kirich1409.viewbindingdelegate.viewBinding
import com.hannesdorfmann.fragmentargs.annotation.Arg
import com.hannesdorfmann.fragmentargs.annotation.FragmentWithArgs
import dagger.hilt.android.AndroidEntryPoint
import dev.bartuzen.qbitcontroller.R
import dev.bartuzen.qbitcontroller.databinding.FragmentSettingsAddEditServerBinding
import dev.bartuzen.qbitcontroller.model.Protocol
import dev.bartuzen.qbitcontroller.model.ServerConfig
import dev.bartuzen.qbitcontroller.ui.base.ArgsFragment
import dev.bartuzen.qbitcontroller.utils.requireAppCompatActivity
import dev.bartuzen.qbitcontroller.utils.setTextWithoutAnimation

@FragmentWithArgs
@AndroidEntryPoint
class AddEditServerFragment : ArgsFragment(R.layout.fragment_settings_add_edit_server) {
    private val binding by viewBinding(FragmentSettingsAddEditServerBinding::bind)

    private val viewModel: AddEditServerViewModel by viewModels()

    @Arg(required = false)
    var serverConfig: ServerConfig? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        requireAppCompatActivity().supportActionBar?.setTitle(
            if (serverConfig == null)
                R.string.settings_server_title_add
            else {
                R.string.settings_server_title_edit
            }
        )

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
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)

        binding.spinnerProtocol.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            listOf("HTTP", "HTTPS")
        )

        serverConfig?.let { config ->
            binding.inputLayoutName.setTextWithoutAnimation(config.name)
            binding.spinnerProtocol.setSelection(config.protocol.ordinal)
            binding.inputLayoutHost.setTextWithoutAnimation(config.host)
            binding.inputLayoutPort.setTextWithoutAnimation(config.port?.toString())
            binding.inputLayoutPath.setTextWithoutAnimation(config.path)
            binding.inputLayoutUsername.setTextWithoutAnimation(config.username)
            binding.inputLayoutPassword.setTextWithoutAnimation(config.password)
        }
    }

    private fun saveServerConfig() {
        val name = binding.editName.text.toString().trim().ifEmpty { null }
        val protocol = Protocol.values()[binding.spinnerProtocol.selectedItemPosition]
        val host = binding.editHost.text.toString().trim().ifEmpty { null }
        val port = binding.editPort.text.toString().toIntOrNull()
        val path = binding.editPath.text.toString().trim().ifEmpty { null }
        val username = binding.editUsername.text.toString().ifEmpty { null }
        val password = binding.editPassword.text.toString().ifEmpty { null }

        if (host == null) {
            binding.inputLayoutHost.error = getString(R.string.settings_server_required_field)
        } else {
            binding.inputLayoutHost.isErrorEnabled = false
        }
        if (username == null) {
            binding.inputLayoutUsername.error = getString(R.string.settings_server_required_field)
        } else if (username.length < 3) {
            binding.inputLayoutUsername.error =
                getString(R.string.settings_server_username_min_character)
        } else {
            binding.inputLayoutUsername.isErrorEnabled = false
        }
        if (password == null) {
            binding.inputLayoutPassword.error = getString(R.string.settings_server_required_field)
        } else if (password.length < 6) {
            binding.inputLayoutPassword.error =
                getString(R.string.settings_server_password_min_character)
        } else {
            binding.inputLayoutPassword.isErrorEnabled = false
        }

        if (host != null && username != null && username.length >= 3 && password != null && password.length >= 6) {
            val serverConfig = serverConfig
            val newConfig = ServerConfig(
                id = serverConfig?.id ?: -1,
                name = name,
                protocol = protocol,
                host = host,
                port = port,
                path = path,
                username = username,
                password = password
            )

            if (serverConfig == null) {
                viewModel.addServer(newConfig).invokeOnCompletion {
                    finish(Result.ADDED)
                }
            } else {
                viewModel.editServer(newConfig).invokeOnCompletion {
                    finish(Result.EDITED)
                }
            }
        }
    }

    private fun deleteServerConfig() {
        val serverConfig = serverConfig ?: return
        viewModel.removeServer(serverConfig).invokeOnCompletion {
            finish(Result.DELETED)
        }
    }

    private fun finish(result: Result) {
        setFragmentResult("addEditServerResult", bundleOf("result" to result))
        parentFragmentManager.popBackStack()
    }

    override fun onStop() {
        super.onStop()

        // hide the keyboard immediately, don't wait for animation to finish
        val windowToken = requireActivity().currentFocus?.windowToken
        if (windowToken != null) {
            val inputMethodManager =
                requireActivity().getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
            inputMethodManager.hideSoftInputFromWindow(windowToken, 0)
        }
    }

    enum class Result {
        ADDED, EDITED, DELETED
    }
}
