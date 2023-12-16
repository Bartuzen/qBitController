package dev.bartuzen.qbitcontroller.ui.settings.addeditserver

import android.app.Activity
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.core.os.bundleOf
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import by.kirich1409.viewbindingdelegate.viewBinding
import dagger.hilt.android.AndroidEntryPoint
import dev.bartuzen.qbitcontroller.R
import dev.bartuzen.qbitcontroller.databinding.FragmentSettingsAddEditServerBinding
import dev.bartuzen.qbitcontroller.model.BasicAuth
import dev.bartuzen.qbitcontroller.model.Protocol
import dev.bartuzen.qbitcontroller.model.ServerConfig
import dev.bartuzen.qbitcontroller.ui.settings.addeditserver.advanced.AdvancedServerSettingsFragment
import dev.bartuzen.qbitcontroller.utils.getErrorMessage
import dev.bartuzen.qbitcontroller.utils.getParcelableCompat
import dev.bartuzen.qbitcontroller.utils.launchAndCollectIn
import dev.bartuzen.qbitcontroller.utils.launchAndCollectLatestIn
import dev.bartuzen.qbitcontroller.utils.requireAppCompatActivity
import dev.bartuzen.qbitcontroller.utils.setDefaultAnimations
import dev.bartuzen.qbitcontroller.utils.setTextWithoutAnimation
import dev.bartuzen.qbitcontroller.utils.showSnackbar
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

@AndroidEntryPoint
class AddEditServerFragment() : Fragment(R.layout.fragment_settings_add_edit_server) {
    private val binding by viewBinding(FragmentSettingsAddEditServerBinding::bind)

    private val viewModel: AddEditServerViewModel by viewModels()

    private val serverId get() = arguments?.getInt("serverId", -1).takeIf { it != -1 }

    private var basicAuth = BasicAuth(false, null, null)

    constructor(serverId: Int?) : this() {
        arguments = bundleOf("serverId" to serverId)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        requireAppCompatActivity().supportActionBar?.setTitle(
            if (serverId == null) {
                R.string.settings_server_title_add
            } else {
                R.string.settings_server_title_edit
            }
        )

        requireActivity().addMenuProvider(
            object : MenuProvider {
                override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                    menuInflater.inflate(R.menu.add_edit_server, menu)
                }

                override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                    when (menuItem.itemId) {
                        R.id.menu_save -> {
                            saveServerConfig()
                        }
                        R.id.menu_delete -> {
                            deleteServerConfig()
                        }
                        R.id.menu_advanced -> {
                            parentFragmentManager.commit {
                                setReorderingAllowed(true)
                                setDefaultAnimations()
                                replace(R.id.container, AdvancedServerSettingsFragment(basicAuth))
                                addToBackStack(null)
                            }
                        }
                        else -> return false
                    }
                    return true
                }

                override fun onPrepareMenu(menu: Menu) {
                    if (serverId == null) {
                        menu.findItem(R.id.menu_delete).isVisible = false
                    }
                }
            },
            viewLifecycleOwner,

            // If there are back stack entries, we will pop the fragment when we are done. In this case, an animation will
            // be played so menu entries should disappear the moment we pop the fragment, and not wait for the animation to
            // finish. Otherwise we will finish the activity. In this case, we should wait for activity animation to finish.
            if (parentFragmentManager.backStackEntryCount > 0) Lifecycle.State.RESUMED else Lifecycle.State.STARTED
        )

        setFragmentResultListener("advancedServerSettingsResult") { _, bundle ->
            basicAuth = bundle.getParcelableCompat("basicAuth")!!
        }

        binding.spinnerProtocol.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            listOf("HTTP", "HTTPS")
        )

        binding.spinnerProtocol.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                binding.checkboxTrustSelfSigned.isEnabled = position == 1
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        serverId?.let { id ->
            val serverConfig = viewModel.getServerConfig(id)
            basicAuth = serverConfig.basicAuth

            binding.inputLayoutName.setTextWithoutAnimation(serverConfig.name)
            binding.spinnerProtocol.setSelection(serverConfig.protocol.ordinal)
            binding.inputLayoutHost.setTextWithoutAnimation(serverConfig.host)
            binding.inputLayoutPort.setTextWithoutAnimation(serverConfig.port?.toString())
            binding.inputLayoutPath.setTextWithoutAnimation(serverConfig.path)
            binding.inputLayoutUsername.setTextWithoutAnimation(serverConfig.username)
            binding.inputLayoutPassword.setTextWithoutAnimation(serverConfig.password)
            binding.checkboxTrustSelfSigned.isChecked = serverConfig.trustSelfSignedCertificates
        }

        binding.buttonTest.setOnClickListener {
            val config = validateAndGetServerConfig() ?: return@setOnClickListener
            viewModel.testConnection(config)
        }

        viewModel.isTesting.launchAndCollectLatestIn(viewLifecycleOwner) { isTesting ->
            binding.progressIndicator.visibility = if (isTesting) View.VISIBLE else View.GONE
        }

        viewModel.eventFlow.launchAndCollectIn(viewLifecycleOwner) { event ->
            when (event) {
                is AddEditServerViewModel.Event.TestFailure -> {
                    showSnackbar(getErrorMessage(requireContext(), event.error))
                }
                AddEditServerViewModel.Event.TestSuccess -> {
                    showSnackbar(R.string.settings_server_connection_success)
                }
            }
        }
    }

    private fun validateAndGetServerConfig(): ServerConfig? {
        val name = binding.editName.text.toString().trim().ifEmpty { null }
        val protocol = Protocol.entries[binding.spinnerProtocol.selectedItemPosition]
        val host = binding.editHost.text.toString().trim().ifEmpty { null }
        val port = binding.editPort.text.toString().toIntOrNull()
        val path = binding.editPath.text.toString().trim().ifEmpty { null }
        val username = binding.editUsername.text.toString().ifEmpty { null }
        val password = binding.editPassword.text.toString().ifEmpty { null }
        val trustSelfSignedCertificates = binding.checkboxTrustSelfSigned.isChecked

        var isValid = true

        if (host == null) {
            binding.inputLayoutHost.error = getString(R.string.settings_server_required_field)
            isValid = false
        } else {
            binding.inputLayoutHost.isErrorEnabled = false
        }

        val portString = binding.editPort.text.toString()
        if (port != null && port !in 1..65535 || portString.length > 5) {
            binding.inputLayoutPort.error = getString(R.string.settings_server_port_must_be_between)
            isValid = false
        } else {
            binding.inputLayoutPort.isErrorEnabled = false
        }

        if (username != null && username.length < 3) {
            binding.inputLayoutUsername.error = getString(R.string.settings_server_username_min_character)
            isValid = false
        } else {
            binding.inputLayoutUsername.isErrorEnabled = false
        }

        if (password != null && password.length < 6) {
            binding.inputLayoutPassword.error = getString(R.string.settings_server_password_min_character)
            isValid = false
        } else {
            binding.inputLayoutPassword.isErrorEnabled = false
        }

        if (!isValid) {
            return null
        }

        val config = ServerConfig(
            id = serverId ?: -1,
            name = name,
            protocol = protocol,
            host = requireNotNull(host),
            port = port,
            path = path,
            username = username,
            password = password,
            trustSelfSignedCertificates = trustSelfSignedCertificates,
            basicAuth = basicAuth
        )

        if (config.url.toHttpUrlOrNull() == null) {
            showSnackbar(R.string.settings_server_url_configuration_not_valid)
            return null
        }

        return config
    }

    private fun saveServerConfig() {
        val config = validateAndGetServerConfig() ?: return

        if (config.id == -1) {
            viewModel.addServer(config).invokeOnCompletion {
                finish(Result.ADDED)
            }
        } else {
            viewModel.editServer(config).invokeOnCompletion {
                finish(Result.EDITED)
            }
        }
    }

    private fun deleteServerConfig() {
        val serverId = serverId ?: return
        viewModel.removeServer(serverId).invokeOnCompletion {
            finish(Result.DELETED)
        }
    }

    private fun finish(result: Result) {
        setFragmentResult("addEditServerResult", bundleOf("result" to result))
        if (parentFragmentManager.backStackEntryCount > 0) {
            parentFragmentManager.popBackStack()
        } else {
            requireActivity().finish()
        }
    }

    override fun onStop() {
        super.onStop()

        // hide the keyboard immediately, don't wait for animation to finish
        val windowToken = requireActivity().currentFocus?.windowToken
        if (windowToken != null) {
            val inputMethodManager = requireActivity().getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
            inputMethodManager.hideSoftInputFromWindow(windowToken, 0)
        }
    }

    enum class Result {
        ADDED,
        EDITED,
        DELETED
    }
}
