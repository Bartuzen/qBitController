package dev.bartuzen.qbitcontroller.ui.search.start

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.core.os.bundleOf
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import by.kirich1409.viewbindingdelegate.viewBinding
import dagger.hilt.android.AndroidEntryPoint
import dev.bartuzen.qbitcontroller.R
import dev.bartuzen.qbitcontroller.databinding.FragmentSearchStartBinding
import dev.bartuzen.qbitcontroller.ui.search.plugins.SearchPluginsFragment
import dev.bartuzen.qbitcontroller.ui.search.result.SearchResultFragment
import dev.bartuzen.qbitcontroller.utils.getErrorMessage
import dev.bartuzen.qbitcontroller.utils.launchAndCollectIn
import dev.bartuzen.qbitcontroller.utils.launchAndCollectLatestIn
import dev.bartuzen.qbitcontroller.utils.setDefaultAnimations
import dev.bartuzen.qbitcontroller.utils.showSnackbar
import kotlinx.coroutines.flow.filterNotNull

@AndroidEntryPoint
class SearchStartFragment() : Fragment(R.layout.fragment_search_start) {
    private val binding by viewBinding(FragmentSearchStartBinding::bind)

    private val viewModel: SearchStartViewModel by viewModels()

    private val serverId get() = arguments?.getInt("serverId", -1).takeIf { it != -1 }!!

    constructor(serverId: Int) : this() {
        arguments = bundleOf("serverId" to serverId)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val adapter = SearchStartAdapter()
        binding.recyclerSearch.adapter = adapter
        restoreState(adapter)

        requireActivity().addMenuProvider(
            object : MenuProvider {
                override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                    menuInflater.inflate(R.menu.search_start, menu)
                }

                override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                    when (menuItem.itemId) {
                        R.id.menu_plugins -> {
                            saveState()
                            val fragment = SearchPluginsFragment(serverId)
                            parentFragmentManager.commit {
                                setReorderingAllowed(true)
                                setDefaultAnimations()
                                replace(R.id.container, fragment)
                                addToBackStack(null)
                            }
                        }
                        R.id.menu_search_start -> {
                            startSearch(adapter)
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
                is SearchStartViewModel.Event.Error -> {
                    showSnackbar(getErrorMessage(requireContext(), event.error))
                }
            }
        }
    }

    private fun startSearch(adapter: SearchStartAdapter) {
        val category = when (val position = adapter.selectedCategoryPosition) {
            0 -> "all"
            1 -> "anime"
            2 -> "books"
            3 -> "games"
            4 -> "movies"
            5 -> "music"
            6 -> "pictures"
            7 -> "software"
            8 -> "tv"
            else -> throw IllegalStateException("Unknown category position: $position")
        }

        val plugins = when (adapter.selectedPluginOption) {
            SearchStartAdapter.PluginSelection.ENABLED -> "enabled"
            SearchStartAdapter.PluginSelection.ALL -> "all"
            SearchStartAdapter.PluginSelection.SELECTED -> adapter.selectedPlugins.joinToString("|")
        }

        saveState()

        val fragment = SearchResultFragment(
            serverId = serverId,
            searchQuery = adapter.searchQuery,
            category = category,
            plugins = plugins
        )
        parentFragmentManager.commit {
            setReorderingAllowed(true)
            setDefaultAnimations()
            replace(R.id.container, fragment)
            addToBackStack(null)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        // binding may be deleted before this function is called
        try {
            saveState()
        } catch (_: IllegalStateException) {
        }
    }

    private fun saveState() {
        val adapter = binding.recyclerSearch.adapter as? SearchStartAdapter ?: return

        viewModel.saveState(
            searchQuery = adapter.searchQuery,
            selectedCategoryPosition = adapter.selectedCategoryPosition,
            selectedPluginOption = adapter.selectedPluginOption,
            selectedPlugins = adapter.selectedPlugins
        )
    }

    private fun restoreState(adapter: SearchStartAdapter) {
        val state = viewModel.getState() ?: return
        adapter.restoreState(
            searchQuery = state.searchQuery,
            selectedCategoryPosition = state.selectedCategoryPosition,
            selectedPluginOption = state.selectedPluginOption,
            selectedPlugins = state.selectedPlugins
        )
    }
}
