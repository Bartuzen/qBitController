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
        requireActivity().addMenuProvider(
            object : MenuProvider {
                override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                    menuInflater.inflate(R.menu.search_start, menu)
                }

                override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                    when (menuItem.itemId) {
                        R.id.menu_search_start -> {
                            startSearch()
                        }
                        else -> return false
                    }
                    return true
                }
            },
            viewLifecycleOwner,
            Lifecycle.State.RESUMED
        )

        val adapter = SearchStartAdapter()
        binding.recyclerPlugins.adapter = adapter

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

    private fun startSearch() {
        val viewHolder =
            binding.recyclerPlugins.findViewHolderForAdapterPosition(0) as SearchStartAdapter.HeaderViewHolder

        val fragment = SearchResultFragment(
            serverId = serverId,
            searchQuery = viewHolder.searchQuery,
            category = viewHolder.category,
            plugins = viewHolder.plugins
        )
        parentFragmentManager.commit {
            setReorderingAllowed(true)
            setDefaultAnimations()
            replace(R.id.container, fragment)
            addToBackStack(null)
        }
    }
}
