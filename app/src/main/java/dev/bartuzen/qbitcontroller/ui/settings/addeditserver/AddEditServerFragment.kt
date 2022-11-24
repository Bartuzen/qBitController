package dev.bartuzen.qbitcontroller.ui.settings.addeditserver

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import androidx.core.os.bundleOf
import androidx.core.view.MenuProvider
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
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
import dev.bartuzen.qbitcontroller.utils.showSnackbar

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
        }, viewLifecycleOwner)

        binding.spinnerProtocol.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            listOf("HTTP", "HTTPS")
        )

        serverConfig?.let { config ->
            binding.inputLayoutName.isHintAnimationEnabled = false
            binding.inputLayoutHost.isHintAnimationEnabled = false
            binding.inputLayoutPort.isHintAnimationEnabled = false
            binding.inputLayoutUsername.isHintAnimationEnabled = false
            binding.inputLayoutPassword.isHintAnimationEnabled = false

            binding.editName.setText(config.name)
            binding.spinnerProtocol.setSelection(config.protocol.ordinal)
            binding.editHost.setText(config.host)
            binding.editPort.setText(config.port.toString())
            binding.editUsername.setText(config.username)
            binding.editPassword.setText(config.password)

            binding.inputLayoutName.isHintAnimationEnabled = true
            binding.inputLayoutHost.isHintAnimationEnabled = true
            binding.inputLayoutPort.isHintAnimationEnabled = true
            binding.inputLayoutUsername.isHintAnimationEnabled = true
            binding.inputLayoutPassword.isHintAnimationEnabled = true
        }
    }

    private fun saveServerConfig() {
        val name = binding.editName.text.toString().ifBlank { null }
        val protocol = Protocol.values()[binding.spinnerProtocol.selectedItemPosition]
        val host = binding.editHost.text.toString().ifBlank { null }
        val port = binding.editPort.text.toString().toIntOrNull()
        val username = binding.editUsername.text.toString().ifBlank { null }
        val password = binding.editPassword.text.toString().ifBlank { null }

        if (host != null && port != null && username != null && password != null) {
            val serverConfig = serverConfig
            if (serverConfig == null) {
                viewModel.addServer(name, protocol, host, port, username, password)
                    .invokeOnCompletion {
                        finish(Result.ADDED)
                    }
            } else {
                val newConfig = serverConfig.copy(
                    name = name,
                    protocol = protocol,
                    host = host,
                    port = port,
                    username = username,
                    password = password
                )
                viewModel.editServer(newConfig).invokeOnCompletion {
                    finish(Result.EDITED)
                }
            }
        } else {
            showSnackbar(R.string.settings_fill_blank_fields)
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

    enum class Result {
        ADDED, EDITED, DELETED
    }
}
