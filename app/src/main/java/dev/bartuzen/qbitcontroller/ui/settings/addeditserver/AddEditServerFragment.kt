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
import dev.bartuzen.qbitcontroller.databinding.FragmentSettingsAddServerBinding
import dev.bartuzen.qbitcontroller.model.Protocol
import dev.bartuzen.qbitcontroller.model.ServerConfig
import dev.bartuzen.qbitcontroller.ui.base.ArgsFragment
import dev.bartuzen.qbitcontroller.utils.requireAppCompatActivity

@FragmentWithArgs
@AndroidEntryPoint
class AddEditServerFragment : ArgsFragment(R.layout.fragment_settings_add_server) {
    private val binding by viewBinding(FragmentSettingsAddServerBinding::bind)

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
            binding.inputLayoutName.isHintAnimationEnabled = false
            binding.inputLayoutHost.isHintAnimationEnabled = false
            binding.inputLayoutPort.isHintAnimationEnabled = false
            binding.inputLayoutPath.isHintAnimationEnabled = false
            binding.inputLayoutUsername.isHintAnimationEnabled = false
            binding.inputLayoutPassword.isHintAnimationEnabled = false

            binding.editName.setText(config.name)
            binding.spinnerProtocol.setSelection(config.protocol.ordinal)
            binding.editHost.setText(config.host)
            binding.editPort.setText(config.port?.toString())
            binding.editPath.setText(config.path)
            binding.editUsername.setText(config.username)
            binding.editPassword.setText(config.password)

            binding.inputLayoutName.isHintAnimationEnabled = true
            binding.inputLayoutHost.isHintAnimationEnabled = true
            binding.inputLayoutPort.isHintAnimationEnabled = true
            binding.inputLayoutPath.isHintAnimationEnabled = true
            binding.inputLayoutUsername.isHintAnimationEnabled = true
            binding.inputLayoutPassword.isHintAnimationEnabled = true
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
            if (serverConfig == null) {
                viewModel.addServer(name, protocol, host, port, path, username, password)
                    .invokeOnCompletion {
                        finish(Result.ADDED)
                    }
            } else {
                val newConfig = serverConfig.copy(
                    name = name,
                    protocol = protocol,
                    host = host,
                    port = port,
                    path = path,
                    username = username,
                    password = password
                )
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
