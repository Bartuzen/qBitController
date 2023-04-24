package dev.bartuzen.qbitcontroller.ui.search.result

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.SearchView
import androidx.core.os.bundleOf
import androidx.core.view.MenuProvider
import androidx.core.view.iterator
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.coroutineScope
import androidx.recyclerview.widget.RecyclerView
import by.kirich1409.viewbindingdelegate.viewBinding
import dagger.hilt.android.AndroidEntryPoint
import dev.bartuzen.qbitcontroller.R
import dev.bartuzen.qbitcontroller.databinding.FragmentSearchResultBinding
import dev.bartuzen.qbitcontroller.model.Search
import dev.bartuzen.qbitcontroller.ui.addtorrent.AddTorrentActivity
import dev.bartuzen.qbitcontroller.utils.getErrorMessage
import dev.bartuzen.qbitcontroller.utils.launchAndCollectIn
import dev.bartuzen.qbitcontroller.utils.launchAndCollectLatestIn
import dev.bartuzen.qbitcontroller.utils.showDialog
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
        requireActivity().addMenuProvider(
            object : MenuProvider {
                override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                    menuInflater.inflate(R.menu.search_result, menu)

                    val searchItem = menu.findItem(R.id.menu_search)

                    val searchView = searchItem.actionView as SearchView
                    searchView.queryHint = getString(R.string.search_filter)
                    searchView.isSubmitButtonEnabled = false
                    searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                        override fun onQueryTextSubmit(query: String?) = false

                        override fun onQueryTextChange(newText: String?): Boolean {
                            viewModel.setSearchQuery(newText ?: "")
                            return true
                        }
                    })

                    searchItem.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
                        override fun onMenuItemActionExpand(item: MenuItem): Boolean {
                            for (menuItem in menu.iterator()) {
                                menuItem.isVisible = false
                            }

                            searchView.maxWidth = Integer.MAX_VALUE
                            return true
                        }

                        override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
                            requireActivity().invalidateOptionsMenu()
                            return true
                        }
                    })
                }

                override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                    when (menuItem.itemId) {
                        R.id.menu_search_stop -> {
                            viewModel.stopSearch(serverId)
                        }
                        else -> return false
                    }
                    return true
                }
            },
            viewLifecycleOwner,
            Lifecycle.State.RESUMED
        )

        if (savedInstanceState == null) {
            viewModel.startSearch(serverId, searchQuery, category, plugins)
        }

        val adapter = SearchResultAdapter(
            onClick = { searchResult ->
                showSearchResultDialog(searchResult)
            }
        )
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

        viewModel.searchResults.filterNotNull().launchAndCollectLatestIn(this) { searchResults ->
            adapter.submitResults(searchResults)
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
                SearchResultViewModel.Event.SearchStopped -> {
                    showSnackbar(R.string.search_stop_success)
                }
            }
        }
    }

    private fun showSearchResultDialog(searchResult: Search.Result) {
        showDialog {
            setMessage(searchResult.fileName)
            setPositiveButton(R.string.search_download) { _, _ ->
                val intent = Intent(requireActivity(), AddTorrentActivity::class.java).apply {
                    putExtra(AddTorrentActivity.Extras.SERVER_ID, serverId)
                    putExtra(AddTorrentActivity.Extras.TORRENT_URL, searchResult.fileUrl)
                }
                startActivity(intent)
            }
            setNeutralButton(R.string.search_open_description) { _, _ ->
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(searchResult.descriptionLink))
                    startActivity(intent)
                } catch (_: ActivityNotFoundException) {
                    showSnackbar(R.string.search_no_browser)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.deleteSearch(serverId)
    }
}
