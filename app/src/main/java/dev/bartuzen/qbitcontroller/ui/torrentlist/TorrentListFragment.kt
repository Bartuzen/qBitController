package dev.bartuzen.qbitcontroller.ui.torrentlist

import android.app.Activity
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.view.ActionMode
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.MenuItem.OnActionExpandListener
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.SearchView.OnQueryTextListener
import androidx.core.view.MenuProvider
import androidx.core.view.iterator
import androidx.drawerlayout.widget.DrawerLayout.DrawerListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import by.kirich1409.viewbindingdelegate.viewBinding
import com.hannesdorfmann.fragmentargs.annotation.Arg
import com.hannesdorfmann.fragmentargs.annotation.FragmentWithArgs
import dagger.hilt.android.AndroidEntryPoint
import dev.bartuzen.qbitcontroller.R
import dev.bartuzen.qbitcontroller.data.TorrentSort
import dev.bartuzen.qbitcontroller.databinding.ActivityMainBinding
import dev.bartuzen.qbitcontroller.databinding.DialogCreateCategoryBinding
import dev.bartuzen.qbitcontroller.databinding.DialogCreateTagBinding
import dev.bartuzen.qbitcontroller.databinding.DialogTorrentDeleteBinding
import dev.bartuzen.qbitcontroller.databinding.FragmentTorrentListBinding
import dev.bartuzen.qbitcontroller.model.ServerConfig
import dev.bartuzen.qbitcontroller.ui.addtorrent.AddTorrentActivity
import dev.bartuzen.qbitcontroller.ui.base.ArgsFragment
import dev.bartuzen.qbitcontroller.ui.main.MainActivity
import dev.bartuzen.qbitcontroller.ui.torrent.TorrentActivity
import dev.bartuzen.qbitcontroller.utils.Quadruple
import dev.bartuzen.qbitcontroller.utils.getErrorMessage
import dev.bartuzen.qbitcontroller.utils.launchAndCollectIn
import dev.bartuzen.qbitcontroller.utils.launchAndCollectLatestIn
import dev.bartuzen.qbitcontroller.utils.setNegativeButton
import dev.bartuzen.qbitcontroller.utils.setPositiveButton
import dev.bartuzen.qbitcontroller.utils.showDialog
import dev.bartuzen.qbitcontroller.utils.showSnackbar
import dev.bartuzen.qbitcontroller.utils.toPx
import dev.bartuzen.qbitcontroller.utils.view
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

@FragmentWithArgs
@AndroidEntryPoint
class TorrentListFragment : ArgsFragment(R.layout.fragment_torrent_list) {
    private val binding by viewBinding(FragmentTorrentListBinding::bind)
    private val activityBinding by viewBinding(ActivityMainBinding::bind, viewProvider = { requireActivity().view })

    private val viewModel: TorrentListViewModel by viewModels()

    private lateinit var categoryTagAdapter: CategoryTagAdapter

    @Arg
    lateinit var serverConfig: ServerConfig

    private lateinit var drawerListener: DrawerListener

    private val startTorrentActivity =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val isTorrentDeleted = result.data?.getBooleanExtra(
                    TorrentActivity.Extras.TORRENT_DELETED,
                    false
                ) ?: false
                if (isTorrentDeleted) {
                    viewModel.loadTorrentList(serverConfig)
                    showSnackbar(R.string.torrent_deleted_success)
                }
            }
        }

    private val startAddTorrentActivity =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val isAdded = result.data?.getBooleanExtra(
                    AddTorrentActivity.Extras.IS_ADDED,
                    false
                ) ?: false
                if (isAdded) {
                    viewModel.loadTorrentList(serverConfig)
                    showSnackbar(R.string.torrent_add_success)
                }
            }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        requireActivity().addMenuProvider(
            object : MenuProvider {
                override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                    menuInflater.inflate(R.menu.torrent_list_menu, menu)

                    viewModel.torrentSort.launchAndCollectLatestIn(viewLifecycleOwner) { sort ->
                        val selectedSort = when (sort) {
                            TorrentSort.HASH -> R.id.menu_sort_hash
                            TorrentSort.NAME -> R.id.menu_sort_name
                            TorrentSort.DOWNLOAD_SPEED -> R.id.menu_sort_dlspeed
                            TorrentSort.UPLOAD_SPEED -> R.id.menu_sort_upspeed
                            TorrentSort.PRIORITY -> R.id.menu_sort_priority
                        }
                        menu.findItem(selectedSort).isChecked = true
                    }

                    viewModel.isReverseSorting.launchAndCollectLatestIn(viewLifecycleOwner) { isReverseSorting ->
                        menu.findItem(R.id.menu_sort_reverse).isChecked = isReverseSorting
                    }

                    val searchItem = menu.findItem(R.id.menu_search)

                    val searchView = searchItem.actionView as SearchView
                    searchView.queryHint = getString(R.string.torrent_list_search_torrents)
                    searchView.isSubmitButtonEnabled = false
                    searchView.setOnQueryTextListener(object : OnQueryTextListener {
                        override fun onQueryTextSubmit(query: String?) = false

                        override fun onQueryTextChange(newText: String?): Boolean {
                            viewModel.setSearchQuery(newText ?: "")
                            return true
                        }
                    })

                    searchItem.setOnActionExpandListener(object : OnActionExpandListener {
                        override fun onMenuItemActionExpand(item: MenuItem): Boolean {
                            // we need to make other items invisible when search bar
                            // is expanded otherwise they act oddly
                            for (menuItem in menu.iterator()) {
                                menuItem.isVisible = false
                            }

                            // SearchView does not completely expand without doing this
                            searchView.maxWidth = Integer.MAX_VALUE
                            return true
                        }

                        override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
                            requireActivity().invalidateOptionsMenu()
                            return true
                        }
                    })
                }

                override fun onMenuItemSelected(menuItem: MenuItem) = when (menuItem.itemId) {
                    R.id.menu_add -> {
                        val intent = Intent(requireActivity(), AddTorrentActivity::class.java).apply {
                            putExtra(AddTorrentActivity.Extras.SERVER_CONFIG, serverConfig)
                        }
                        startAddTorrentActivity.launch(intent)
                        true
                    }
                    R.id.menu_sort_name -> {
                        viewModel.setTorrentSort(TorrentSort.NAME)
                        true
                    }
                    R.id.menu_sort_hash -> {
                        viewModel.setTorrentSort(TorrentSort.HASH)
                        true
                    }
                    R.id.menu_sort_dlspeed -> {
                        viewModel.setTorrentSort(TorrentSort.DOWNLOAD_SPEED)
                        true
                    }
                    R.id.menu_sort_upspeed -> {
                        viewModel.setTorrentSort(TorrentSort.UPLOAD_SPEED)
                        true
                    }
                    R.id.menu_sort_priority -> {
                        viewModel.setTorrentSort(TorrentSort.PRIORITY)
                        true
                    }
                    R.id.menu_sort_reverse -> {
                        viewModel.changeReverseSorting()
                        true
                    }
                    else -> false
                }
            },
            viewLifecycleOwner
        )

        var actionMode: ActionMode? = null
        val adapter = TorrentListAdapter().apply {
            onClick { torrent ->
                val intent = Intent(context, TorrentActivity::class.java).apply {
                    putExtra(TorrentActivity.Extras.TORRENT_HASH, torrent.hash)
                    putExtra(TorrentActivity.Extras.SERVER_CONFIG, serverConfig)
                }
                startTorrentActivity.launch(intent)
            }
            onSelectionModeStart {
                actionMode = requireActivity().startActionMode(object : ActionMode.Callback {
                    override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
                        mode.menuInflater.inflate(R.menu.torrent_list_selection_menu, menu)
                        return true
                    }

                    override fun onPrepareActionMode(mode: ActionMode, menu: Menu) = false

                    override fun onActionItemClicked(mode: ActionMode, item: MenuItem) = when (item.itemId) {
                        R.id.menu_delete -> {
                            showDeleteTorrentsDialog(this@apply, actionMode)
                            true
                        }
                        R.id.menu_pause -> {
                            viewModel.pauseTorrents(serverConfig, selectedItems.toList())
                            finishSelection()
                            actionMode?.finish()
                            true
                        }
                        R.id.menu_resume -> {
                            viewModel.resumeTorrents(serverConfig, selectedItems.toList())
                            finishSelection()
                            actionMode?.finish()
                            true
                        }
                        R.id.menu_priority_increase -> {
                            viewModel.increaseTorrentPriority(
                                serverConfig,
                                selectedItems.toList()
                            )
                            actionMode?.finish()
                            true
                        }
                        R.id.menu_priority_decrease -> {
                            viewModel.decreaseTorrentPriority(
                                serverConfig,
                                selectedItems.toList()
                            )
                            actionMode?.finish()
                            true
                        }
                        R.id.menu_priority_maximize -> {
                            viewModel.maximizeTorrentPriority(
                                serverConfig,
                                selectedItems.toList()
                            )
                            actionMode?.finish()
                            true
                        }
                        R.id.menu_priority_minimize -> {
                            viewModel.minimizeTorrentPriority(
                                serverConfig,
                                selectedItems.toList()
                            )
                            actionMode?.finish()
                            true
                        }
                        R.id.menu_select_all -> {
                            selectAll()
                            true
                        }
                        R.id.menu_select_inverse -> {
                            selectInverse()
                            true
                        }
                        else -> false
                    }

                    override fun onDestroyActionMode(mode: ActionMode) {
                        finishSelection()
                        actionMode = null
                    }
                })
            }
            onSelectionModeEnd {
                actionMode?.finish()
            }
            onUpdateSelection {
                val itemCount = selectedItemCount
                if (itemCount != 0) {
                    actionMode?.title = resources.getQuantityString(
                        R.plurals.torrent_list_torrents_selected,
                        itemCount,
                        itemCount
                    )
                }
            }
        }
        binding.recyclerTorrentList.adapter = adapter
        binding.recyclerTorrentList.addItemDecoration(object : RecyclerView.ItemDecoration() {
            override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
                val verticalPx = 8.toPx(requireContext())
                val horizontalPx = 4.toPx(requireContext())
                if (parent.getChildAdapterPosition(view) == 0) {
                    outRect.top = verticalPx
                }
                outRect.bottom = verticalPx
                outRect.left = horizontalPx
                outRect.right = horizontalPx
            }
        })

        categoryTagAdapter = CategoryTagAdapter(
            onSelected = { isCategory, name ->
                if (isCategory) {
                    viewModel.setSelectedCategory(name)
                } else {
                    viewModel.setSelectedTag(name)
                }

                activityBinding.layoutDrawer.close()
            },
            onLongClick = { isCategory, name ->
                activityBinding.layoutDrawer.close()
                showDeleteCategoryTagDialog(isCategory, name)
            },
            onCreateClick = { isCategory ->
                activityBinding.layoutDrawer.close()
                if (isCategory) {
                    showCreateCategoryDialog()
                } else {
                    showCreateTagDialog()
                }
            }
        )
        (requireActivity() as MainActivity).submitCategoryTagAdapter(categoryTagAdapter)

        drawerListener = object : DrawerListener {
            override fun onDrawerStateChanged(newState: Int) {
                actionMode?.finish()
            }

            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {}

            override fun onDrawerOpened(drawerView: View) {}

            override fun onDrawerClosed(drawerView: View) {}
        }
        activityBinding.layoutDrawer.addDrawerListener(drawerListener)

        combine(
            viewModel.sortedTorrentList,
            viewModel.searchQuery,
            viewModel.selectedCategory,
            viewModel.selectedTag
        ) { torrentList, searchQuery, selectedCategory, selectedTag ->
            if (torrentList != null) {
                Quadruple(torrentList, searchQuery, selectedCategory, selectedTag)
            } else {
                null
            }
        }.filterNotNull()
            .launchAndCollectLatestIn(viewLifecycleOwner) { (torrentList, query, selectedCategory, selectedTag) ->
                val list = torrentList
                    .filter { torrent ->
                        if (query.isNotEmpty()) {
                            if (!torrent.name.contains(query, ignoreCase = true)) {
                                return@filter false
                            }
                        }
                        if (selectedCategory != null) {
                            if (torrent.category != selectedCategory) {
                                return@filter false
                            }
                        }
                        if (selectedTag != null) {
                            if (selectedTag !in torrent.tags) {
                                return@filter false
                            }
                        }
                        true
                    }

                adapter.submitList(list)
            }

        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refreshTorrentListCategoryTags(serverConfig)
        }

        if (!viewModel.isInitialLoadStarted) {
            viewModel.isInitialLoadStarted = true
            viewModel.loadTorrentList(serverConfig)
            viewModel.updateCategoryAndTags(serverConfig)
        }

        viewModel.isLoading.launchAndCollectLatestIn(viewLifecycleOwner) { isLoading ->
            binding.progressIndicator.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.isRefreshing.launchAndCollectLatestIn(viewLifecycleOwner) { isRefreshing ->
            binding.swipeRefresh.isRefreshing = isRefreshing
        }

        combine(viewModel.categoryList, viewModel.tagList) { categoryList, tagList ->
            if (categoryList != null && tagList != null) {
                categoryList to tagList
            } else {
                null
            }
        }.filterNotNull().launchAndCollectLatestIn(viewLifecycleOwner) { (categoryList, tagList) ->
            categoryTagAdapter.submitLists(categoryList, tagList)
        }

        viewModel.eventFlow.launchAndCollectIn(viewLifecycleOwner) { event ->
            when (event) {
                is TorrentListViewModel.Event.Error -> {
                    showSnackbar(getErrorMessage(requireContext(), event.error))
                }
                is TorrentListViewModel.Event.QueueingNotEnabled -> {
                    showSnackbar(R.string.torrent_queueing_is_not_enabled)
                }
                is TorrentListViewModel.Event.TorrentsDeleted -> {
                    showSnackbar(
                        resources.getQuantityString(
                            R.plurals.torrent_list_torrents_deleted_success,
                            event.count,
                            event.count
                        )
                    )

                    viewModel.loadTorrentList(serverConfig)
                }
                is TorrentListViewModel.Event.TorrentsPaused -> {
                    showSnackbar(
                        resources.getQuantityString(
                            R.plurals.torrent_list_torrents_paused_success,
                            event.count,
                            event.count
                        )
                    )

                    viewLifecycleOwner.lifecycleScope.launch {
                        delay(1000) // wait until qBittorrent pauses the torrent
                        viewModel.loadTorrentList(serverConfig)
                    }
                }
                is TorrentListViewModel.Event.TorrentsResumed -> {
                    showSnackbar(
                        resources.getQuantityString(
                            R.plurals.torrent_list_torrents_resumed_success,
                            event.count,
                            event.count
                        )
                    )

                    viewLifecycleOwner.lifecycleScope.launch {
                        delay(1000) // wait until qBittorrent resumes the torrent
                        viewModel.loadTorrentList(serverConfig)
                    }
                }
                is TorrentListViewModel.Event.CategoryDeleted -> {
                    showSnackbar(
                        getString(R.string.torrent_list_delete_category_success, event.name)
                    )
                    viewModel.loadTorrentList(serverConfig)
                    viewModel.updateCategoryAndTags(serverConfig)
                }
                is TorrentListViewModel.Event.TagDeleted -> {
                    showSnackbar(
                        getString(R.string.torrent_list_delete_tag_success, event.name)
                    )
                    viewModel.loadTorrentList(serverConfig)
                    viewModel.updateCategoryAndTags(serverConfig)
                }
                TorrentListViewModel.Event.TorrentsPriorityDecreased -> {
                    showSnackbar(R.string.torrent_list_priority_decreased_success)
                    viewLifecycleOwner.lifecycleScope.launch {
                        delay(1000) // wait until qBittorrent changes the priority
                        viewModel.loadTorrentList(serverConfig)
                    }
                }
                TorrentListViewModel.Event.TorrentsPriorityIncreased -> {
                    showSnackbar(R.string.torrent_list_priority_increased_success)
                    viewLifecycleOwner.lifecycleScope.launch {
                        delay(1000) // wait until qBittorrent changes the priority
                        viewModel.loadTorrentList(serverConfig)
                    }
                }
                TorrentListViewModel.Event.TorrentsPriorityMaximized -> {
                    showSnackbar(R.string.torrent_list_priority_maximized_success)
                    viewLifecycleOwner.lifecycleScope.launch {
                        delay(1000) // wait until qBittorrent changes the priority
                        viewModel.loadTorrentList(serverConfig)
                    }
                }
                TorrentListViewModel.Event.TorrentsPriorityMinimized -> {
                    showSnackbar(R.string.torrent_list_priority_minimized_success)
                    viewLifecycleOwner.lifecycleScope.launch {
                        delay(1000) // wait until qBittorrent changes the priority
                        viewModel.loadTorrentList(serverConfig)
                    }
                }
                TorrentListViewModel.Event.CategoryCreated -> {
                    showSnackbar(R.string.torrent_list_create_category_success)
                    viewModel.updateCategoryAndTags(serverConfig)
                }
                TorrentListViewModel.Event.TagCreated -> {
                    showSnackbar(R.string.torrent_list_create_tag_success)
                    viewModel.updateCategoryAndTags(serverConfig)
                }
            }
        }
    }

    private fun showDeleteTorrentsDialog(adapter: TorrentListAdapter, actionMode: ActionMode?) {
        showDialog(DialogTorrentDeleteBinding::inflate) { binding ->
            setTitle(
                resources.getQuantityString(
                    R.plurals.torrent_list_delete_torrents,
                    adapter.selectedItemCount,
                    adapter.selectedItemCount
                )
            )
            setPositiveButton { _, _ ->
                viewModel.deleteTorrents(serverConfig, adapter.selectedItems.toList(), binding.checkDeleteFiles.isChecked)
                adapter.finishSelection()
                actionMode?.finish()
            }
            setNegativeButton()
        }
    }

    private fun showDeleteCategoryTagDialog(isCategory: Boolean, name: String) {
        showDialog {
            setTitle(
                if (isCategory) {
                    R.string.torrent_list_delete_category_title
                } else {
                    R.string.torrent_list_delete_tag_title
                }
            )
            setMessage(
                getString(
                    if (isCategory) {
                        R.string.torrent_list_delete_category_desc
                    } else {
                        R.string.torrent_list_delete_tag_desc
                    },
                    name
                )
            )
            setPositiveButton { _, _ ->
                if (isCategory) {
                    viewModel.deleteCategory(serverConfig, name)
                } else {
                    viewModel.deleteTag(serverConfig, name)
                }
            }
            setNegativeButton()
        }
    }

    private fun showCreateCategoryDialog() {
        showDialog(DialogCreateCategoryBinding::inflate) { binding ->
            setTitle(R.string.torrent_list_create_category_title)
            setPositiveButton { _, _ ->
                viewModel.createCategory(
                    serverConfig,
                    binding.editName.text.toString(),
                    binding.editSavePath.text.toString()
                )
            }
            setNegativeButton()
        }
    }

    private fun showCreateTagDialog() {
        showDialog(DialogCreateTagBinding::inflate) { binding ->
            setTitle(R.string.torrent_list_create_tag_title)
            setPositiveButton { _, _ ->
                viewModel.createTags(
                    serverConfig,
                    binding.editName.text.toString().split("\n")
                )
            }
            setNegativeButton()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        activityBinding.layoutDrawer.removeDrawerListener(drawerListener)
        (requireActivity() as MainActivity).removeCategoryTagAdapter(categoryTagAdapter)
    }
}
