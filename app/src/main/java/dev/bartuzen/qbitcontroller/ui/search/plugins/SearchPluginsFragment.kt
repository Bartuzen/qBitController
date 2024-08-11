package dev.bartuzen.qbitcontroller.ui.search.plugins

import android.app.AlertDialog
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.core.os.bundleOf
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import by.kirich1409.viewbindingdelegate.viewBinding
import com.google.android.material.divider.MaterialDividerItemDecoration
import dagger.hilt.android.AndroidEntryPoint
import dev.bartuzen.qbitcontroller.R
import dev.bartuzen.qbitcontroller.databinding.DialogPluginInstallBinding
import dev.bartuzen.qbitcontroller.databinding.FragmentSearchPluginsBinding
import dev.bartuzen.qbitcontroller.utils.applySystemBarInsets
import dev.bartuzen.qbitcontroller.utils.getErrorMessage
import dev.bartuzen.qbitcontroller.utils.launchAndCollectIn
import dev.bartuzen.qbitcontroller.utils.launchAndCollectLatestIn
import dev.bartuzen.qbitcontroller.utils.setNegativeButton
import dev.bartuzen.qbitcontroller.utils.setPositiveButton
import dev.bartuzen.qbitcontroller.utils.showDialog
import dev.bartuzen.qbitcontroller.utils.showSnackbar
import dev.bartuzen.qbitcontroller.utils.text
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SearchPluginsFragment() : Fragment(R.layout.fragment_search_plugins) {
    private val binding by viewBinding(FragmentSearchPluginsBinding::bind)

    private val viewModel: SearchPluginsViewModel by viewModels()

    private val serverId get() = arguments?.getInt("serverId", -1).takeIf { it != -1 }!!

    constructor(serverId: Int) : this() {
        arguments = bundleOf("serverId" to serverId)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.recyclerPlugins.applySystemBarInsets(top = false)

        val adapter = SearchPluginsAdapter()
        binding.recyclerPlugins.adapter = adapter
        binding.recyclerPlugins.addItemDecoration(
            MaterialDividerItemDecoration(requireContext(), LinearLayoutManager.VERTICAL).apply {
                isLastItemDecorated = false
            },
        )

        requireActivity().addMenuProvider(
            object : MenuProvider {
                override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                    menuInflater.inflate(R.menu.search_plugins, menu)
                }

                override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                    when (menuItem.itemId) {
                        R.id.menu_save -> {
                            viewModel.savePlugins(serverId, adapter.pluginsEnabledState, adapter.pluginsToDelete)
                        }
                        R.id.menu_install_plugins -> {
                            showInstallPluginDialog()
                        }
                        R.id.menu_update_plugins -> {
                            viewModel.updateAllPlugins(serverId)
                        }
                        else -> return false
                    }
                    return true
                }
            },
            viewLifecycleOwner,
            Lifecycle.State.RESUMED,
        )

        if (!viewModel.isInitialLoadStarted) {
            viewModel.isInitialLoadStarted = true
            viewModel.loadPlugins(serverId)
        }

        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refreshPlugins(serverId)
        }

        binding.progressIndicator.setVisibilityAfterHide(View.GONE)
        viewModel.isLoading.launchAndCollectLatestIn(this) { isLoading ->
            if (isLoading) {
                binding.progressIndicator.show()
            } else {
                binding.progressIndicator.hide()
            }
        }

        viewModel.isRefreshing.launchAndCollectLatestIn(this) { isRefreshing ->
            binding.swipeRefresh.isRefreshing = isRefreshing
        }

        viewModel.plugins.filterNotNull().launchAndCollectLatestIn(viewLifecycleOwner) { plugins ->
            adapter.submitPlugins(plugins)
        }

        viewModel.eventFlow.launchAndCollectIn(this) { event ->
            when (event) {
                is SearchPluginsViewModel.Event.Error -> {
                    showSnackbar(getErrorMessage(requireContext(), event.error))
                }
                SearchPluginsViewModel.Event.PluginsStateUpdated -> {
                    showSnackbar(R.string.search_plugins_save_success)
                    viewModel.loadPlugins(serverId)
                }
                SearchPluginsViewModel.Event.PluginsInstalled -> {
                    showSnackbar(R.string.search_plugins_install_success)

                    viewLifecycleOwner.lifecycleScope.launch {
                        delay(1000)
                        viewModel.loadPlugins(serverId)
                    }
                }
                SearchPluginsViewModel.Event.PluginsUpdated -> {
                    showSnackbar(R.string.search_plugins_update_success)
                }
            }
        }
    }

    private fun showInstallPluginDialog() {
        lateinit var dialogBinding: DialogPluginInstallBinding

        val dialog = showDialog(DialogPluginInstallBinding::inflate) { binding ->
            dialogBinding = binding

            setTitle(R.string.search_plugins_action_install_plugins)
            setPositiveButton()
            setNegativeButton()
        }

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val sources = dialogBinding.inputLayoutSources.text

            if (sources.isNotBlank()) {
                viewModel.installPlugin(serverId, sources.split("\n"))
                dialog.dismiss()
            } else {
                dialogBinding.inputLayoutSources.error = getString(R.string.search_plugins_cannot_be_empty)
            }
        }
    }
}
