package dev.bartuzen.qbitcontroller.ui.search.start

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import by.kirich1409.viewbindingdelegate.viewBinding
import dagger.hilt.android.AndroidEntryPoint
import dev.bartuzen.qbitcontroller.R
import dev.bartuzen.qbitcontroller.databinding.FragmentSearchStartBinding
import dev.bartuzen.qbitcontroller.utils.launchAndCollectLatestIn
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
    }
}
