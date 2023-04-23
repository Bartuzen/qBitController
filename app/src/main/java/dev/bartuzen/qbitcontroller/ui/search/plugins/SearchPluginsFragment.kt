package dev.bartuzen.qbitcontroller.ui.search.plugins

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
import by.kirich1409.viewbindingdelegate.viewBinding
import dagger.hilt.android.AndroidEntryPoint
import dev.bartuzen.qbitcontroller.R
import dev.bartuzen.qbitcontroller.databinding.FragmentSearchPluginsBinding
import dev.bartuzen.qbitcontroller.utils.getErrorMessage
import dev.bartuzen.qbitcontroller.utils.launchAndCollectIn
import dev.bartuzen.qbitcontroller.utils.launchAndCollectLatestIn
import dev.bartuzen.qbitcontroller.utils.showSnackbar
import kotlinx.coroutines.flow.filterNotNull

@AndroidEntryPoint
class SearchPluginsFragment() : Fragment(R.layout.fragment_search_plugins) {
    private val binding by viewBinding(FragmentSearchPluginsBinding::bind)

    private val viewModel: SearchPluginsViewModel by viewModels()

    private val serverId get() = arguments?.getInt("serverId", -1).takeIf { it != -1 }!!

    constructor(serverId: Int) : this() {
        arguments = bundleOf("serverId" to serverId)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val adapter = SearchPluginsAdapter()
        binding.recyclerPlugins.adapter = adapter

        requireActivity().addMenuProvider(
            object : MenuProvider {
                override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                    menuInflater.inflate(R.menu.search_plugins, menu)
                }

                override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                    when (menuItem.itemId) {
                        R.id.menu_update -> {
                            viewModel.updatePluginStates(serverId, adapter.pluginsEnabledState, adapter.pluginsToDelete)
                        }
                        else -> return false
                    }
                    return true
                }
            },
            viewLifecycleOwner,
            Lifecycle.State.RESUMED
        )

        if (!viewModel.isInitialLoadStarted) {
            viewModel.isInitialLoadStarted = true
            viewModel.loadPlugins(serverId)
        }

        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refreshPlugins(serverId)
        }

        viewModel.isLoading.launchAndCollectLatestIn(this) { isLoading ->
            binding.progressIndicator.visibility = if (isLoading) View.VISIBLE else View.GONE
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
                    showSnackbar(R.string.search_plugins_update_success)
                    viewModel.loadPlugins(serverId)
                }
            }
        }
    }
}
