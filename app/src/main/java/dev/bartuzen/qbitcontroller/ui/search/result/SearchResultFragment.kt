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
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.iterator
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import by.kirich1409.viewbindingdelegate.viewBinding
import dagger.hilt.android.AndroidEntryPoint
import dev.bartuzen.qbitcontroller.R
import dev.bartuzen.qbitcontroller.data.SearchSort
import dev.bartuzen.qbitcontroller.databinding.DialogSearchFilterBinding
import dev.bartuzen.qbitcontroller.databinding.FragmentSearchResultBinding
import dev.bartuzen.qbitcontroller.model.Search
import dev.bartuzen.qbitcontroller.ui.addtorrent.AddTorrentActivity
import dev.bartuzen.qbitcontroller.utils.getErrorMessage
import dev.bartuzen.qbitcontroller.utils.launchAndCollectIn
import dev.bartuzen.qbitcontroller.utils.launchAndCollectLatestIn
import dev.bartuzen.qbitcontroller.utils.setNegativeButton
import dev.bartuzen.qbitcontroller.utils.setPositiveButton
import dev.bartuzen.qbitcontroller.utils.showDialog
import dev.bartuzen.qbitcontroller.utils.showSnackbar
import dev.bartuzen.qbitcontroller.utils.text
import dev.bartuzen.qbitcontroller.utils.toPx
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.isActive

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

                    viewModel.searchSort.launchAndCollectLatestIn(viewLifecycleOwner) { sort ->
                        val selectedSort = when (sort) {
                            SearchSort.NAME -> R.id.menu_sort_name
                            SearchSort.SIZE -> R.id.menu_sort_size
                            SearchSort.SEEDERS -> R.id.menu_sort_seeders
                            SearchSort.LEECHERS -> R.id.menu_sort_leechers
                            SearchSort.SEARCH_ENGINE -> R.id.menu_sort_search_engine
                        }
                        menu.findItem(selectedSort)?.isChecked = true
                    }

                    viewModel.isReverseSearchSorting.launchAndCollectLatestIn(viewLifecycleOwner) { isReverseSearchSort ->
                        menu.findItem(R.id.menu_sort_reverse)?.isChecked = isReverseSearchSort
                    }

                    val searchItem = menu.findItem(R.id.menu_search)

                    val searchView = searchItem.actionView as SearchView
                    searchView.queryHint = getString(R.string.search_filter_results)
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
                        R.id.menu_filter -> {
                            showFilterDialog()
                        }
                        R.id.menu_sort_name -> {
                            viewModel.setSearchSort(SearchSort.NAME)
                        }
                        R.id.menu_sort_size -> {
                            viewModel.setSearchSort(SearchSort.SIZE)
                        }
                        R.id.menu_sort_seeders -> {
                            viewModel.setSearchSort(SearchSort.SEEDERS)
                        }
                        R.id.menu_sort_leechers -> {
                            viewModel.setSearchSort(SearchSort.LEECHERS)
                        }
                        R.id.menu_sort_search_engine -> {
                            viewModel.setSearchSort(SearchSort.SEARCH_ENGINE)
                        }
                        R.id.menu_sort_reverse -> {
                            viewModel.changeReverseSorting()
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
                outRect.bottom = verticalPx
                outRect.left = horizontalPx
                outRect.right = horizontalPx
            }
        })

        viewModel.isSearchContinuing.launchAndCollectLatestIn(this) { isSearchContinuing ->
            binding.progressIndicator.visibility = if (isSearchContinuing) View.VISIBLE else View.GONE
            binding.swipeRefresh.isEnabled = !isSearchContinuing
        }

        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refresh(serverId, searchQuery, category, plugins)
        }

        viewModel.isRefreshing.launchAndCollectLatestIn(this) { isRefreshing ->
            binding.swipeRefresh.isRefreshing = isRefreshing
        }

        combine(viewModel.searchResults, viewModel.searchCount) { searchResults, searchCount ->
            searchResults to searchCount
        }.launchAndCollectLatestIn(this) { (searchResults, searchCount) ->
            val layoutManager = binding.recyclerTorrents.layoutManager as LinearLayoutManager
            val position = layoutManager.findFirstVisibleItemPosition()
            val offset = layoutManager.findViewByPosition(position)?.top

            adapter.submitList(searchResults) {
                if (offset != null) {
                    layoutManager.scrollToPositionWithOffset(position, offset)
                }
            }

            binding.textCount.text = getString(R.string.search_showing_count, searchResults.size, searchCount)
        }

        viewModel.isSearchContinuing.launchAndCollectLatestIn(viewLifecycleOwner) { isSearchContinuing ->
            if (isSearchContinuing) {
                while (isActive) {
                    delay(1000)
                    if (viewModel.isSearchContinuing.value) {
                        viewModel.loadResults(serverId)
                    }
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

    private fun showFilterDialog() {
        lateinit var dialogBinding: DialogSearchFilterBinding

        val dialog = showDialog(DialogSearchFilterBinding::inflate) { binding ->
            dialogBinding = binding

            binding.dropdownSizeMinUnit.setItems(
                R.string.size_bytes,
                R.string.size_kibibytes,
                R.string.size_mebibytes,
                R.string.size_gibibytes,
                R.string.size_tebibytes,
                R.string.size_pebibytes,
                R.string.size_exbibytes
            )

            binding.dropdownSizeMaxUnit.setItems(
                R.string.size_bytes,
                R.string.size_kibibytes,
                R.string.size_mebibytes,
                R.string.size_gibibytes,
                R.string.size_tebibytes,
                R.string.size_pebibytes,
                R.string.size_exbibytes
            )

            val filter = viewModel.filter.value
            binding.inputLayoutSeedsMin.text = filter.seedsMin?.toString() ?: ""
            binding.inputLayoutSeedsMax.text = filter.seedsMax?.toString() ?: ""
            binding.inputLayoutSizeMin.text = filter.sizeMin?.toString() ?: ""
            binding.inputLayoutSizeMax.text = filter.sizeMax?.toString() ?: ""
            binding.dropdownSizeMinUnit.setPosition(filter.sizeMinUnit)
            binding.dropdownSizeMaxUnit.setPosition(filter.sizeMaxUnit)

            setTitle(R.string.search_filter)
            setPositiveButton { _, _ ->
                val newFilter = SearchResultViewModel.Filter(
                    seedsMin = binding.inputLayoutSeedsMin.text.toIntOrNull(),
                    seedsMax = binding.inputLayoutSeedsMax.text.toIntOrNull(),
                    sizeMin = binding.inputLayoutSizeMin.text.toLongOrNull(),
                    sizeMax = binding.inputLayoutSizeMax.text.toLongOrNull(),
                    sizeMinUnit = binding.dropdownSizeMinUnit.position,
                    sizeMaxUnit = binding.dropdownSizeMaxUnit.position
                )
                viewModel.setFilter(newFilter)
            }
            setNegativeButton()
        }

        dialogBinding.dropdownSizeMinUnit.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                val window = dialog.window ?: return@setOnFocusChangeListener
                WindowCompat.getInsetsController(window, window.decorView).hide(WindowInsetsCompat.Type.ime())
            }
        }
        dialogBinding.dropdownSizeMaxUnit.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                val window = dialog.window ?: return@setOnFocusChangeListener
                WindowCompat.getInsetsController(window, window.decorView).hide(WindowInsetsCompat.Type.ime())
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.deleteSearch(serverId)
    }
}
