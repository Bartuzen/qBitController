package dev.bartuzen.qbitcontroller.ui.search.result

import android.graphics.Rect
import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.coroutineScope
import androidx.recyclerview.widget.RecyclerView
import by.kirich1409.viewbindingdelegate.viewBinding
import dagger.hilt.android.AndroidEntryPoint
import dev.bartuzen.qbitcontroller.R
import dev.bartuzen.qbitcontroller.databinding.FragmentSearchResultBinding
import dev.bartuzen.qbitcontroller.utils.getErrorMessage
import dev.bartuzen.qbitcontroller.utils.launchAndCollectIn
import dev.bartuzen.qbitcontroller.utils.launchAndCollectLatestIn
import dev.bartuzen.qbitcontroller.utils.showSnackbar
import dev.bartuzen.qbitcontroller.utils.toPx
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SearchResultFragment() : Fragment(R.layout.fragment_search_result) {
    private val binding by viewBinding(FragmentSearchResultBinding::bind)

    private val viewModel: SearchResultViewModel by viewModels()

    private val serverId get() = arguments?.getInt("serverId", -1).takeIf { it != -1 }!!
    private val searchQuery get() = arguments?.getString("searchQuery", null)!!
    private val category get() = arguments?.getString("category", null)!!
    private val plugins get() = arguments?.getString("plugins", null)!!

    constructor(serverId: Int, searchQuery: String, category: String, plugins: String) : this() {
        arguments = bundleOf(
            "serverId" to serverId,
            "searchQuery" to searchQuery,
            "category" to category,
            "plugins" to plugins
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (savedInstanceState == null) {
            viewModel.startSearch(serverId, searchQuery, category, plugins)
        }

        val adapter = SearchResultAdapter()
        binding.recyclerTorrents.adapter = adapter
        binding.recyclerTorrents.addItemDecoration(object : RecyclerView.ItemDecoration() {
            override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
                val verticalPx = 8.toPx(requireContext())
                val horizontalPx = 8.toPx(requireContext())
                if (parent.getChildAdapterPosition(view) == 0) {
                    outRect.top = verticalPx
                }
                outRect.bottom = verticalPx
                outRect.left = horizontalPx
                outRect.right = horizontalPx
            }
        })

        viewModel.isSearchContinuing.launchAndCollectLatestIn(this) { isLoadingCompleted ->
            binding.progressIndicator.visibility = if (isLoadingCompleted) View.VISIBLE else View.GONE
        }

        viewModel.searchResult.filterNotNull().launchAndCollectLatestIn(this) { searchResult ->
            adapter.submitResults(searchResult.results)
        }

        viewLifecycleOwner.lifecycle.coroutineScope.launch {
            while (isActive) {
                delay(1000)
                if (!viewModel.isSearchContinuing.value) {
                    cancel()
                } else {
                    viewModel.loadResults(serverId)
                }
            }
        }

        viewModel.eventFlow.launchAndCollectIn(this) { event ->
            when (event) {
                is SearchResultViewModel.Event.Error -> {
                    showSnackbar(getErrorMessage(requireContext(), event.error))
                }
            }
        }
    }
}
