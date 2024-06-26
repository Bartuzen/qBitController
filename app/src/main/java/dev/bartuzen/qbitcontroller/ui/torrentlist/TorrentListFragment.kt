package dev.bartuzen.qbitcontroller.ui.torrentlist

import android.app.Activity
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.InputType
import android.view.ActionMode
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.MenuItem.OnActionExpandListener
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.PopupMenu
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.SearchView.OnQueryTextListener
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import androidx.core.os.bundleOf
import androidx.core.view.MenuProvider
import androidx.core.view.iterator
import androidx.drawerlayout.widget.DrawerLayout.DrawerListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import by.kirich1409.viewbindingdelegate.viewBinding
import com.google.android.material.chip.Chip
import dagger.hilt.android.AndroidEntryPoint
import dev.bartuzen.qbitcontroller.R
import dev.bartuzen.qbitcontroller.data.TorrentSort
import dev.bartuzen.qbitcontroller.databinding.ActivityMainBinding
import dev.bartuzen.qbitcontroller.databinding.DialogCreateEditCategoryBinding
import dev.bartuzen.qbitcontroller.databinding.DialogCreateTagBinding
import dev.bartuzen.qbitcontroller.databinding.DialogServerStatsBinding
import dev.bartuzen.qbitcontroller.databinding.DialogSpeedLimitBinding
import dev.bartuzen.qbitcontroller.databinding.DialogTorrentCategoryBinding
import dev.bartuzen.qbitcontroller.databinding.DialogTorrentDeleteBinding
import dev.bartuzen.qbitcontroller.databinding.DialogTorrentLocationBinding
import dev.bartuzen.qbitcontroller.databinding.FragmentTorrentListBinding
import dev.bartuzen.qbitcontroller.model.Category
import dev.bartuzen.qbitcontroller.model.Torrent
import dev.bartuzen.qbitcontroller.model.TorrentState
import dev.bartuzen.qbitcontroller.ui.addtorrent.AddTorrentActivity
import dev.bartuzen.qbitcontroller.ui.log.LogActivity
import dev.bartuzen.qbitcontroller.ui.main.MainActivity
import dev.bartuzen.qbitcontroller.ui.rss.RssActivity
import dev.bartuzen.qbitcontroller.ui.search.SearchActivity
import dev.bartuzen.qbitcontroller.ui.torrent.TorrentActivity
import dev.bartuzen.qbitcontroller.utils.applyNavigationBarInsets
import dev.bartuzen.qbitcontroller.utils.applySystemBarInsets
import dev.bartuzen.qbitcontroller.utils.formatBytes
import dev.bartuzen.qbitcontroller.utils.formatBytesPerSecond
import dev.bartuzen.qbitcontroller.utils.getDrawableCompat
import dev.bartuzen.qbitcontroller.utils.getErrorMessage
import dev.bartuzen.qbitcontroller.utils.getThemeColor
import dev.bartuzen.qbitcontroller.utils.launchAndCollectIn
import dev.bartuzen.qbitcontroller.utils.launchAndCollectLatestIn
import dev.bartuzen.qbitcontroller.utils.requireAppCompatActivity
import dev.bartuzen.qbitcontroller.utils.setNegativeButton
import dev.bartuzen.qbitcontroller.utils.setPositiveButton
import dev.bartuzen.qbitcontroller.utils.setTextWithoutAnimation
import dev.bartuzen.qbitcontroller.utils.showDialog
import dev.bartuzen.qbitcontroller.utils.showSnackbar
import dev.bartuzen.qbitcontroller.utils.text
import dev.bartuzen.qbitcontroller.utils.themeColors
import dev.bartuzen.qbitcontroller.utils.toPx
import dev.bartuzen.qbitcontroller.utils.view
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@AndroidEntryPoint
class TorrentListFragment() : Fragment(R.layout.fragment_torrent_list) {
    private val binding by viewBinding(FragmentTorrentListBinding::bind)
    private val activityBinding by viewBinding(ActivityMainBinding::bind, viewProvider = { requireActivity().view })

    private val viewModel: TorrentListViewModel by viewModels()

    private lateinit var parentAdapter: RecyclerView.Adapter<*>

    private val parentActivity get() = requireActivity() as MainActivity

    private val serverId get() = arguments?.getInt("serverId", -1).takeIf { it != -1 }!!

    constructor(serverId: Int) : this() {
        arguments = bundleOf("serverId" to serverId)
    }

    private lateinit var drawerListener: DrawerListener

    private val startTorrentActivity =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val isTorrentDeleted = result.data?.getBooleanExtra(
                    TorrentActivity.Extras.TORRENT_DELETED,
                    false
                ) ?: false
                if (isTorrentDeleted) {
                    viewModel.loadMainData(serverId)
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
                    viewModel.loadMainData(serverId)
                    showSnackbar(R.string.torrent_add_success)
                }
            }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.textSpeed.applySystemBarInsets(top = false, padding = 4)
        binding.recyclerTorrentList.applyNavigationBarInsets()

        requireActivity().addMenuProvider(
            object : MenuProvider {
                override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                    menuInflater.inflate(R.menu.torrent_list, menu)

                    viewModel.torrentSort.launchAndCollectLatestIn(viewLifecycleOwner) { sort ->
                        val selectedSort = when (sort) {
                            TorrentSort.HASH -> R.id.menu_sort_hash
                            TorrentSort.NAME -> R.id.menu_sort_name
                            TorrentSort.STATUS -> R.id.menu_sort_status
                            TorrentSort.DOWNLOAD_SPEED -> R.id.menu_sort_dlspeed
                            TorrentSort.UPLOAD_SPEED -> R.id.menu_sort_upspeed
                            TorrentSort.PRIORITY -> R.id.menu_sort_priority
                            TorrentSort.ETA -> R.id.menu_sort_eta
                            TorrentSort.SIZE -> R.id.menu_sort_size
                            TorrentSort.PROGRESS -> R.id.menu_sort_progress
                            TorrentSort.RATIO -> R.id.menu_sort_ratio
                            TorrentSort.CONNECTED_SEEDS -> R.id.menu_sort_connected_seeds
                            TorrentSort.TOTAL_SEEDS -> R.id.menu_sort_total_seeds
                            TorrentSort.CONNECTED_LEECHES -> R.id.menu_sort_connected_leeches
                            TorrentSort.TOTAL_LEECHES -> R.id.menu_sort_total_leeches
                            TorrentSort.ADDITION_DATE -> R.id.menu_sort_addition_date
                            TorrentSort.COMPLETION_DATE -> R.id.menu_sort_completion_date
                            TorrentSort.LAST_ACTIVITY -> R.id.menu_sort_last_activity
                        }
                        menu.findItem(selectedSort).isChecked = true
                    }

                    viewModel.isReverseSorting.launchAndCollectLatestIn(viewLifecycleOwner) { isReverseSorting ->
                        menu.findItem(R.id.menu_sort_reverse).isChecked = isReverseSorting
                    }

                    viewModel.mainData.launchAndCollectLatestIn(viewLifecycleOwner) { mainData ->
                        val stats = menu.findItem(R.id.menu_stats)
                        stats.isEnabled = mainData != null
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

                override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                    when (menuItem.itemId) {
                        R.id.menu_add -> {
                            val intent = Intent(requireActivity(), AddTorrentActivity::class.java).apply {
                                putExtra(AddTorrentActivity.Extras.SERVER_ID, serverId)
                            }
                            startAddTorrentActivity.launch(intent)
                        }
                        R.id.menu_rss -> {
                            val intent = Intent(requireActivity(), RssActivity::class.java).apply {
                                putExtra(RssActivity.Extras.SERVER_ID, serverId)
                            }
                            startActivity(intent)
                        }
                        R.id.menu_plugin_search -> {
                            val intent = Intent(requireActivity(), SearchActivity::class.java).apply {
                                putExtra(SearchActivity.Extras.SERVER_ID, serverId)
                            }
                            startActivity(intent)
                        }
                        R.id.menu_log -> {
                            val intent = Intent(requireActivity(), LogActivity::class.java).apply {
                                putExtra(LogActivity.Extras.SERVER_ID, serverId)
                            }
                            startActivity(intent)
                        }
                        R.id.menu_shutdown -> {
                            showShutdownDialog()
                        }
                        R.id.menu_sort_name -> {
                            viewModel.setTorrentSort(TorrentSort.NAME)
                        }
                        R.id.menu_sort_status -> {
                            viewModel.setTorrentSort(TorrentSort.STATUS)
                        }
                        R.id.menu_sort_hash -> {
                            viewModel.setTorrentSort(TorrentSort.HASH)
                        }
                        R.id.menu_sort_dlspeed -> {
                            viewModel.setTorrentSort(TorrentSort.DOWNLOAD_SPEED)
                        }
                        R.id.menu_sort_upspeed -> {
                            viewModel.setTorrentSort(TorrentSort.UPLOAD_SPEED)
                        }
                        R.id.menu_sort_eta -> {
                            viewModel.setTorrentSort(TorrentSort.ETA)
                        }
                        R.id.menu_sort_size -> {
                            viewModel.setTorrentSort(TorrentSort.SIZE)
                        }
                        R.id.menu_sort_progress -> {
                            viewModel.setTorrentSort(TorrentSort.PROGRESS)
                        }
                        R.id.menu_sort_ratio -> {
                            viewModel.setTorrentSort(TorrentSort.RATIO)
                        }
                        R.id.menu_sort_connected_seeds -> {
                            viewModel.setTorrentSort(TorrentSort.CONNECTED_SEEDS)
                        }
                        R.id.menu_sort_total_seeds -> {
                            viewModel.setTorrentSort(TorrentSort.TOTAL_SEEDS)
                        }
                        R.id.menu_sort_connected_leeches -> {
                            viewModel.setTorrentSort(TorrentSort.CONNECTED_LEECHES)
                        }
                        R.id.menu_sort_total_leeches -> {
                            viewModel.setTorrentSort(TorrentSort.TOTAL_LEECHES)
                        }
                        R.id.menu_sort_addition_date -> {
                            viewModel.setTorrentSort(TorrentSort.ADDITION_DATE)
                        }
                        R.id.menu_sort_completion_date -> {
                            viewModel.setTorrentSort(TorrentSort.COMPLETION_DATE)
                        }
                        R.id.menu_sort_last_activity -> {
                            viewModel.setTorrentSort(TorrentSort.LAST_ACTIVITY)
                        }
                        R.id.menu_sort_priority -> {
                            viewModel.setTorrentSort(TorrentSort.PRIORITY)
                        }
                        R.id.menu_sort_reverse -> {
                            viewModel.changeReverseSorting()
                        }
                        R.id.menu_stats -> {
                            showStatsDialog()
                        }
                        else -> return false
                    }
                    return true
                }
            },
            viewLifecycleOwner
        )

        var actionMode: ActionMode? = null
        val adapter = TorrentListAdapter().apply {
            onClick { torrent ->
                val intent = Intent(context, TorrentActivity::class.java).apply {
                    putExtra(TorrentActivity.Extras.TORRENT_HASH, torrent.hash)
                    putExtra(TorrentActivity.Extras.SERVER_ID, serverId)
                }
                startTorrentActivity.launch(intent)
            }
            onSelectionModeStart {
                actionMode = requireActivity().startActionMode(object : ActionMode.Callback {
                    override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
                        mode.menuInflater.inflate(R.menu.torrent_list_selection, menu)
                        return true
                    }

                    override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
                        val isQueueingEnabled = viewModel.mainData.value?.serverState?.isQueueingEnabled == true
                        menu.findItem(R.id.menu_priority).isEnabled = isQueueingEnabled

                        return true
                    }

                    override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
                        when (item.itemId) {
                            R.id.menu_delete -> {
                                showDeleteTorrentsDialog(
                                    hashes = selectedItems.toList(),
                                    onDelete = {
                                        finishSelection()
                                        actionMode?.finish()
                                    }
                                )
                            }
                            R.id.menu_pause -> {
                                viewModel.pauseTorrents(serverId, selectedItems.toList())
                                finishSelection()
                                actionMode?.finish()
                            }
                            R.id.menu_resume -> {
                                viewModel.resumeTorrents(serverId, selectedItems.toList())
                                finishSelection()
                                actionMode?.finish()
                            }
                            R.id.menu_priority_maximize -> {
                                viewModel.maximizeTorrentPriority(
                                    serverId,
                                    selectedItems.toList()
                                )
                                actionMode?.finish()
                            }
                            R.id.menu_priority_increase -> {
                                viewModel.increaseTorrentPriority(
                                    serverId,
                                    selectedItems.toList()
                                )
                                actionMode?.finish()
                            }
                            R.id.menu_priority_decrease -> {
                                viewModel.decreaseTorrentPriority(
                                    serverId,
                                    selectedItems.toList()
                                )
                                actionMode?.finish()
                            }
                            R.id.menu_priority_minimize -> {
                                viewModel.minimizeTorrentPriority(
                                    serverId,
                                    selectedItems.toList()
                                )
                                actionMode?.finish()
                            }
                            R.id.menu_location -> {
                                val selectedItems = selectedItems.toList()

                                val currentLocation =
                                    viewModel.mainData.value?.torrents
                                        ?.filter { torrent -> torrent.hash in selectedItems }
                                        ?.distinctBy { torrent -> torrent.savePath }
                                        ?.let { list ->
                                            if (list.size == 1) list.first().savePath else null
                                        }
                                showLocationDialog(
                                    currentLocation = currentLocation,
                                    onSuccess = { newLocation ->
                                        viewModel.setLocation(serverId, selectedItems, newLocation)
                                        actionMode?.finish()
                                    }
                                )
                            }
                            R.id.menu_set_category -> {
                                val selectedItems = selectedItems.toList()
                                val commonCategory = viewModel.mainData.value?.torrents
                                    ?.filter { torrent -> torrent.hash in selectedItems }
                                    ?.distinctBy { torrent -> torrent.category }
                                    ?.let { torrents -> if (torrents.size == 1) torrents.first().category else null }

                                showCategoryDialog(
                                    commonCategory = commonCategory,
                                    onSuccess = { category ->
                                        viewModel.setCategory(serverId, selectedItems, category)
                                        actionMode?.finish()
                                    }
                                )
                            }
                            R.id.menu_select_all -> {
                                selectAll()
                            }
                            R.id.menu_select_inverse -> {
                                selectInverse()
                            }
                            else -> return false
                        }
                        return true
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
        binding.recyclerTorrentList.itemAnimator?.changeDuration = 0
        binding.recyclerTorrentList.addItemDecoration(object : RecyclerView.ItemDecoration() {
            override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
                val verticalPx = 8.toPx(requireContext())
                val horizontalPx = 4.toPx(requireContext())
                outRect.bottom = verticalPx
                outRect.left = horizontalPx
                outRect.right = horizontalPx
            }
        })

        val itemTouchHelper = setupItemTouchHelper()
        viewModel.areTorrentSwipeActionsEnabled.launchAndCollectLatestIn(viewLifecycleOwner) { isEnabled ->
            if (isEnabled) {
                itemTouchHelper.attachToRecyclerView(binding.recyclerTorrentList)
            } else {
                itemTouchHelper.attachToRecyclerView(null)
            }
        }

        val torrentFilterAdapter = TorrentFilterAdapter(
            isCollapsed = viewModel.areStatesCollapsed.value,
            onClick = { filter ->
                viewModel.setSelectedFilter(filter)

                activityBinding.layoutDrawer.close()
            },
            onCollapse = { isCollapsed ->
                viewModel.areStatesCollapsed.value = isCollapsed
            }
        )

        val categoryAdapter = CategoryTagAdapter(
            isCategory = true,
            isCollapsed = viewModel.areCategoriesCollapsed.value,
            onSelected = { category ->
                viewModel.setSelectedCategory(category)
                activityBinding.layoutDrawer.close()
            },
            onLongClick = { name, rootView ->
                val areSubcategoriesEnabled = viewModel.mainData.value?.serverState?.areSubcategoriesEnabled == true

                val popupMenu = PopupMenu(requireContext(), rootView)
                popupMenu.inflate(R.menu.torrent_list_drawer_category)
                popupMenu.menu.findItem(R.id.menu_create_subcategory).isVisible = areSubcategoriesEnabled
                popupMenu.show()

                popupMenu.setOnMenuItemClickListener { item ->
                    when (item.itemId) {
                        R.id.menu_create_subcategory -> {
                            showCreateEditCategoryDialog(null, name)
                        }
                        R.id.menu_edit -> {
                            showCreateEditCategoryDialog(name.substringAfterLast("/"), name)
                        }
                        R.id.menu_delete -> {
                            showDeleteCategoryTagDialog(true, name)
                        }
                        else -> return@setOnMenuItemClickListener false
                    }

                    activityBinding.layoutDrawer.close()
                    true
                }
            },
            onCreateClick = {
                showCreateEditCategoryDialog(null, null)
                activityBinding.layoutDrawer.close()
            },
            onCollapse = { isCollapsed ->
                viewModel.areCategoriesCollapsed.value = isCollapsed
            }
        )

        val tagAdapter = CategoryTagAdapter(
            isCategory = false,
            isCollapsed = viewModel.areTagsCollapsed.value,
            onSelected = { tag ->
                viewModel.setSelectedTag(tag)
                activityBinding.layoutDrawer.close()
            },
            onLongClick = { name, rootView ->
                val popupMenu = PopupMenu(requireContext(), rootView)
                popupMenu.inflate(R.menu.torrent_list_drawer_tag)
                popupMenu.show()

                popupMenu.setOnMenuItemClickListener { item ->
                    when (item.itemId) {
                        R.id.menu_delete -> {
                            showDeleteCategoryTagDialog(false, name)
                        }
                        else -> return@setOnMenuItemClickListener false
                    }

                    activityBinding.layoutDrawer.close()
                    true
                }
            },
            onCreateClick = {
                showCreateTagDialog()
                activityBinding.layoutDrawer.close()
            },
            onCollapse = { isCollapsed ->
                viewModel.areTagsCollapsed.value = isCollapsed
            }
        )

        val trackerAdapter = TrackerAdapter(
            isCollapsed = viewModel.areTrackersCollapsed.value,
            onSelected = { tracker ->
                viewModel.setSelectedTracker(tracker)
                activityBinding.layoutDrawer.close()
            },
            onCollapse = { isCollapsed ->
                viewModel.areTrackersCollapsed.value = isCollapsed
            }
        )

        parentAdapter = ConcatAdapter(torrentFilterAdapter, categoryAdapter, tagAdapter, trackerAdapter)

        parentActivity.submitAdapter(parentAdapter)

        drawerListener = object : DrawerListener {
            override fun onDrawerStateChanged(newState: Int) {
                actionMode?.finish()
            }

            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {}

            override fun onDrawerOpened(drawerView: View) {}

            override fun onDrawerClosed(drawerView: View) {}
        }
        activityBinding.layoutDrawer.addDrawerListener(drawerListener)

        binding.textSpeed.setOnClickListener {
            viewModel.mainData.value?.serverState?.let { serverState ->
                showSpeedLimitsDialog(
                    useAlternativeSpeedLimits = serverState.useAlternativeSpeedLimits,
                    downloadSpeedLimit = serverState.downloadSpeedLimit,
                    uploadSpeedLimit = serverState.uploadSpeedLimit
                )
            }
        }

        viewModel.filteredTorrentList.filterNotNull().launchAndCollectLatestIn(viewLifecycleOwner) { torrentList ->
            val layoutManager = binding.recyclerTorrentList.layoutManager as LinearLayoutManager
            val position = layoutManager.findFirstVisibleItemPosition()
            val offset = layoutManager.findViewByPosition(position)?.top

            adapter.submitList(torrentList) {
                if (offset != null) {
                    layoutManager.scrollToPositionWithOffset(position, offset)
                }
            }
        }

        viewModel.mainData.filterNotNull().launchAndCollectLatestIn(viewLifecycleOwner) { mainData ->
            trackerAdapter.submitTrackers(
                trackers = mainData.trackers,
                allCount = mainData.torrents.size,
                trackerlessCount = mainData.torrents.count { it.trackerCount == 0 }
            )

            binding.textSpeed.text = getString(
                R.string.torrent_list_speed_format,
                formatBytesPerSecond(requireContext(), mainData.serverState.downloadSpeed),
                formatBytes(requireContext(), mainData.serverState.downloadSession),
                formatBytesPerSecond(requireContext(), mainData.serverState.uploadSpeed),
                formatBytes(requireContext(), mainData.serverState.uploadSession)
            )

            val stateCountMap = mutableMapOf<TorrentFilter, Int>()
            val categoryMap = mainData.categories.associateBy({ it.name }, { 0 }).toMutableMap()
            val tagMap = mainData.tags.associateBy({ it }, { 0 }).toMutableMap()

            var uncategorizedCount = 0
            var untaggedCount = 0

            mainData.torrents.forEach { torrent ->
                TorrentFilter.entries.forEach { filter ->
                    when (filter) {
                        TorrentFilter.ACTIVE -> {
                            if (torrent.downloadSpeed != 0L || torrent.uploadSpeed != 0L) {
                                stateCountMap[filter] = (stateCountMap[filter] ?: 0) + 1
                            }
                        }
                        TorrentFilter.INACTIVE -> {
                            if (torrent.downloadSpeed == 0L && torrent.uploadSpeed == 0L) {
                                stateCountMap[filter] = (stateCountMap[filter] ?: 0) + 1
                            }
                        }
                        else -> {
                            if (filter.states == null || torrent.state in filter.states) {
                                stateCountMap[filter] = (stateCountMap[filter] ?: 0) + 1
                            }
                        }
                    }
                }

                if (torrent.category != null) {
                    categoryMap[torrent.category] = (categoryMap[torrent.category] ?: 0) + 1
                } else {
                    uncategorizedCount++
                }

                if (torrent.tags.isNotEmpty()) {
                    torrent.tags.forEach { tag ->
                        tagMap[tag] = (tagMap[tag] ?: 0) + 1
                    }
                } else {
                    untaggedCount++
                }
            }

            categoryAdapter.areSubcategoriesEnabled = mainData.serverState.areSubcategoriesEnabled

            if (mainData.serverState.areSubcategoriesEnabled) {
                val categories = categoryMap.keys.toList()
                categories.forEachIndexed { index, category ->
                    for (i in index + 1 until categories.size) {
                        if (categories[i].startsWith("$category/")) {
                            categoryMap[category] = (categoryMap[category] ?: 0) + (categoryMap[categories[i]] ?: 0)
                        } else {
                            break
                        }
                    }
                }
            }

            categoryAdapter.submitList(
                items = categoryMap,
                allCount = mainData.torrents.size,
                uncategorizedCount = uncategorizedCount
            )
            tagAdapter.submitList(
                items = tagMap,
                allCount = mainData.torrents.size,
                uncategorizedCount = untaggedCount
            )

            torrentFilterAdapter.submitCountMap(stateCountMap)

            actionMode?.invalidate()

            binding.textSpeed.visibility = View.VISIBLE

            requireAppCompatActivity().supportActionBar?.subtitle = getString(
                R.string.torrent_list_free_space,
                formatBytes(requireContext(), mainData.serverState.freeSpace)
            )
        }

        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refreshMainData(serverId)
        }

        if (!viewModel.isInitialLoadStarted) {
            viewModel.isInitialLoadStarted = true
            viewModel.loadMainData(serverId)
        }

        binding.progressIndicator.setVisibilityAfterHide(View.GONE)
        viewModel.isNaturalLoading.launchAndCollectLatestIn(viewLifecycleOwner) { isNaturalLoading ->
            if (isNaturalLoading == true) {
                binding.progressIndicator.show()
            } else {
                binding.progressIndicator.hide()
            }
        }

        viewModel.isRefreshing.launchAndCollectLatestIn(viewLifecycleOwner) { isRefreshing ->
            binding.swipeRefresh.isRefreshing = isRefreshing
        }

        viewModel.autoRefreshInterval.launchAndCollectLatestIn(viewLifecycleOwner) { interval ->
            if (interval != 0) {
                while (isActive) {
                    delay(interval * 1000L)
                    if (isActive && actionMode == null) {
                        viewModel.loadMainData(serverId, autoRefresh = true)
                    }
                }
            }
        }

        viewModel.eventFlow.launchAndCollectIn(viewLifecycleOwner) { event ->
            when (event) {
                is TorrentListViewModel.Event.Error -> {
                    showSnackbar(getErrorMessage(requireContext(), event.error))
                }
                TorrentListViewModel.Event.QueueingNotEnabled -> {
                    showSnackbar(R.string.torrent_queueing_is_not_enabled)
                }
                TorrentListViewModel.Event.CategoryEditingFailed -> {
                    showSnackbar(R.string.torrent_list_edit_category_error)
                }
                is TorrentListViewModel.Event.TorrentsDeleted -> {
                    showSnackbar(
                        resources.getQuantityString(
                            R.plurals.torrent_list_torrents_delete_success,
                            event.count,
                            event.count
                        )
                    )
                    viewModel.loadMainData(serverId)
                }
                is TorrentListViewModel.Event.TorrentsPaused -> {
                    showSnackbar(
                        resources.getQuantityString(
                            R.plurals.torrent_list_torrents_pause_success,
                            event.count,
                            event.count
                        )
                    )

                    viewLifecycleOwner.lifecycleScope.launch {
                        delay(1000) // wait until qBittorrent pauses the torrent
                        viewModel.loadMainData(serverId)
                    }
                }
                is TorrentListViewModel.Event.TorrentsResumed -> {
                    showSnackbar(
                        resources.getQuantityString(
                            R.plurals.torrent_list_torrents_resume_success,
                            event.count,
                            event.count
                        )
                    )

                    viewLifecycleOwner.lifecycleScope.launch {
                        delay(1000) // wait until qBittorrent resumes the torrent
                        viewModel.loadMainData(serverId)
                    }
                }
                is TorrentListViewModel.Event.CategoryDeleted -> {
                    showSnackbar(
                        getString(R.string.torrent_list_delete_category_success, event.name)
                    )
                    viewModel.loadMainData(serverId)
                }
                is TorrentListViewModel.Event.TagDeleted -> {
                    showSnackbar(
                        getString(R.string.torrent_list_delete_tag_success, event.name)
                    )
                    viewModel.loadMainData(serverId)
                }
                TorrentListViewModel.Event.TorrentsPriorityDecreased -> {
                    showSnackbar(R.string.torrent_list_priority_decrease_success)
                    viewLifecycleOwner.lifecycleScope.launch {
                        delay(1000) // wait until qBittorrent changes the priority
                        viewModel.loadMainData(serverId)
                    }
                }
                TorrentListViewModel.Event.TorrentsPriorityIncreased -> {
                    showSnackbar(R.string.torrent_list_priority_increase_success)
                    viewLifecycleOwner.lifecycleScope.launch {
                        delay(1000) // wait until qBittorrent changes the priority
                        viewModel.loadMainData(serverId)
                    }
                }
                TorrentListViewModel.Event.TorrentsPriorityMaximized -> {
                    showSnackbar(R.string.torrent_list_priority_maximize_success)
                    viewLifecycleOwner.lifecycleScope.launch {
                        delay(1000) // wait until qBittorrent changes the priority
                        viewModel.loadMainData(serverId)
                    }
                }
                TorrentListViewModel.Event.TorrentsPriorityMinimized -> {
                    showSnackbar(R.string.torrent_list_priority_minimize_success)
                    viewLifecycleOwner.lifecycleScope.launch {
                        delay(1000) // wait until qBittorrent changes the priority
                        viewModel.loadMainData(serverId)
                    }
                }
                TorrentListViewModel.Event.LocationUpdated -> {
                    showSnackbar(R.string.torrent_location_update_success)
                    viewModel.loadMainData(serverId)
                }
                TorrentListViewModel.Event.CategoryCreated -> {
                    showSnackbar(R.string.torrent_list_create_category_success)
                    viewModel.loadMainData(serverId)
                }
                TorrentListViewModel.Event.CategoryEdited -> {
                    showSnackbar(R.string.torrent_list_edit_category_success)
                    viewModel.loadMainData(serverId)
                }
                TorrentListViewModel.Event.TagCreated -> {
                    showSnackbar(R.string.torrent_list_create_tag_success)
                    viewModel.loadMainData(serverId)
                }
                is TorrentListViewModel.Event.SpeedLimitsToggled -> {
                    if (event.switchedToAlternativeLimit) {
                        showSnackbar(R.string.torrent_list_switch_speed_limit_alternative_success)
                    } else {
                        showSnackbar(R.string.torrent_list_switch_speed_limit_regular_success)
                    }
                    viewModel.loadMainData(serverId)
                }
                TorrentListViewModel.Event.SpeedLimitsUpdated -> {
                    showSnackbar(R.string.torrent_speed_update_success)
                    viewModel.loadMainData(serverId)
                }
                TorrentListViewModel.Event.Shutdown -> {
                    showSnackbar(R.string.torrent_list_shutdown_success)
                }
                TorrentListViewModel.Event.TorrentCategoryUpdated -> {
                    showSnackbar(R.string.torrent_category_update_success)
                    viewModel.loadMainData(serverId)
                }
            }
        }
    }

    private fun showCategoryDialog(commonCategory: String?, onSuccess: (category: String?) -> Unit) {
        val categories = viewModel.mainData.value?.categories ?: return
        showDialog(DialogTorrentCategoryBinding::inflate) { dialogBinding ->
            dialogBinding.progressIndicator.visibility = View.GONE

            categories.forEach { category ->
                val chip = layoutInflater.inflate(R.layout.chip_category, dialogBinding.chipGroupCategory, false) as Chip
                chip.text = category.name
                chip.isClickable = true

                if (category.name == commonCategory) {
                    chip.isChecked = true
                }

                dialogBinding.chipGroupCategory.addView(chip)
            }

            setTitle(R.string.torrent_action_category)
            setPositiveButton { _, _ ->
                val selectedCategory = dialogBinding.chipGroupCategory.checkedChipId.let { id ->
                    if (id != View.NO_ID) {
                        dialogBinding.chipGroupCategory.findViewById<Chip>(id).text.toString()
                    } else {
                        null
                    }
                }

                onSuccess(selectedCategory)
            }
            setNegativeButton()
        }
    }

    private fun setupItemTouchHelper(): ItemTouchHelper {
        lateinit var itemTouchHelper: ItemTouchHelper

        @Suppress("ktlint:standard:property-naming")
        val callback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.START or ItemTouchHelper.END) {
            val pauseIcon = requireContext().getDrawableCompat(R.drawable.ic_pause)!!
            val resumeIcon = requireContext().getDrawableCompat(R.drawable.ic_resume)!!
            val deleteIcon = requireContext().getDrawableCompat(R.drawable.ic_delete)!!

            init {
                val color = requireContext().getThemeColor(themeColors.colorOnSurface)
                val colorFilter =
                    BlendModeColorFilterCompat.createBlendModeColorFilterCompat(color, BlendModeCompat.SRC_ATOP)

                pauseIcon.colorFilter = colorFilter
                resumeIcon.colorFilter = colorFilter
                deleteIcon.colorFilter = colorFilter
            }

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ) = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val torrent = getTorrent(viewHolder) ?: return

                if (direction == ItemTouchHelper.START) {
                    showDeleteTorrentsDialog(listOf(torrent.hash)) {}
                } else if (direction == ItemTouchHelper.END) {
                    if (isTorrentPaused(torrent)) {
                        viewModel.resumeTorrents(serverId, listOf(torrent.hash))
                    } else {
                        viewModel.pauseTorrents(serverId, listOf(torrent.hash))
                    }
                }

                viewHolder.itemView.animate().apply {
                    translationX(0f)
                    duration = 300L
                    withEndAction {
                        itemTouchHelper.attachToRecyclerView(null)
                        itemTouchHelper.attachToRecyclerView(binding.recyclerTorrentList)
                    }
                }
            }

            private fun getTorrent(viewHolder: RecyclerView.ViewHolder): Torrent? {
                val position = viewHolder.bindingAdapterPosition
                return viewModel.filteredTorrentList.value?.getOrNull(position)
            }

            override fun onChildDraw(
                c: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                val torrent = getTorrent(viewHolder) ?: return
                val leftIcon: Drawable
                val rightIcon: Drawable

                if (binding.root.layoutDirection == View.LAYOUT_DIRECTION_LTR) {
                    leftIcon = if (isTorrentPaused(torrent)) {
                        resumeIcon
                    } else {
                        pauseIcon
                    }
                    rightIcon = deleteIcon
                } else {
                    rightIcon = if (isTorrentPaused(torrent)) {
                        resumeIcon
                    } else {
                        pauseIcon
                    }
                    leftIcon = deleteIcon
                }

                drawLeftIcon(viewHolder.itemView, c, leftIcon)
                drawRightIcon(viewHolder.itemView, c, rightIcon)

                super.onChildDraw(c, recyclerView, viewHolder, dX / 2, dY, actionState, isCurrentlyActive)
            }

            override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
                binding.swipeRefresh.isEnabled = actionState != ItemTouchHelper.ACTION_STATE_SWIPE
            }

            private fun isTorrentPaused(torrent: Torrent) = torrent.state in listOf(
                TorrentState.PAUSED_DL,
                TorrentState.PAUSED_UP,
                TorrentState.MISSING_FILES,
                TorrentState.ERROR
            )

            private fun drawLeftIcon(itemView: View, canvas: Canvas, icon: Drawable) {
                val margin = 16.toPx(requireContext())
                val itemHeight = itemView.bottom - itemView.top
                val intrinsicWidth = icon.intrinsicWidth
                val intrinsicHeight = icon.intrinsicHeight

                val left = itemView.left + margin
                val right = itemView.left + margin + intrinsicWidth
                val top = itemView.top + (itemHeight - intrinsicHeight) / 2
                val bottom = top + intrinsicHeight
                icon.setBounds(left, top, right, bottom)

                icon.draw(canvas)
            }

            private fun drawRightIcon(itemView: View, canvas: Canvas, icon: Drawable) {
                val margin = 16.toPx(requireContext())
                val itemHeight = itemView.bottom - itemView.top
                val intrinsicWidth = icon.intrinsicWidth
                val intrinsicHeight = icon.intrinsicHeight

                val left = itemView.right - margin - intrinsicWidth
                val right = itemView.right - margin
                val top = itemView.top + (itemHeight - intrinsicHeight) / 2
                val bottom = top + intrinsicHeight
                icon.setBounds(left, top, right, bottom)

                icon.draw(canvas)
            }
        }

        itemTouchHelper = ItemTouchHelper(callback)
        return itemTouchHelper
    }

    private fun showDeleteTorrentsDialog(hashes: List<String>, onDelete: () -> Unit) {
        showDialog(DialogTorrentDeleteBinding::inflate) { binding ->
            setTitle(
                resources.getQuantityString(
                    R.plurals.torrent_list_delete_torrents,
                    hashes.size,
                    hashes.size
                )
            )
            setPositiveButton { _, _ ->
                viewModel.deleteTorrents(serverId, hashes, binding.checkDeleteFiles.isChecked)
                onDelete()
            }
            setNegativeButton()
        }
    }

    private fun showLocationDialog(currentLocation: String?, onSuccess: (newLocation: String) -> Unit) {
        lateinit var dialogBinding: DialogTorrentLocationBinding

        val dialog = showDialog(DialogTorrentLocationBinding::inflate) { binding ->
            dialogBinding = binding

            binding.inputLayoutLocation.setTextWithoutAnimation(currentLocation)

            setTitle(R.string.torrent_list_action_set_location)
            setPositiveButton()
            setNegativeButton()
        }

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val newLocation = dialogBinding.inputLayoutLocation.text
            if (newLocation.isNotBlank()) {
                onSuccess(newLocation)
                dialog.dismiss()
            } else {
                dialogBinding.inputLayoutLocation.error = getString(R.string.torrent_location_cannot_be_blank)
            }
        }
    }

    private fun showSpeedLimitsDialog(useAlternativeSpeedLimits: Boolean, downloadSpeedLimit: Int, uploadSpeedLimit: Int) {
        lateinit var dialogBinding: DialogSpeedLimitBinding

        val dialog = showDialog(DialogSpeedLimitBinding::inflate) { binding ->
            dialogBinding = binding

            binding.checkSpeedLimit.isChecked = useAlternativeSpeedLimits
            binding.checkSpeedLimit.setOnCheckedChangeListener { _, isChecked ->
                val isEnabled = useAlternativeSpeedLimits == isChecked
                binding.inputLayoutDownload.isEnabled = isEnabled
                binding.inputLayoutUpload.isEnabled = isEnabled
                binding.inputLayoutDlspeedUnit.isEnabled = isEnabled
                binding.inputLayoutUpspeedUnit.isEnabled = isEnabled
            }

            binding.dropdownDlspeedLimitUnit.setItems(
                R.string.speed_kibibytes_per_second,
                R.string.speed_mebibytes_per_second
            )
            binding.dropdownUpspeedLimitUnit.setItems(
                R.string.speed_kibibytes_per_second,
                R.string.speed_mebibytes_per_second
            )

            binding.inputLayoutDownload.setTextWithoutAnimation((downloadSpeedLimit / 1024).toString())
            binding.inputLayoutUpload.setTextWithoutAnimation((uploadSpeedLimit / 1024).toString())

            setTitle(R.string.torrent_speed_limits_title)
            setPositiveButton()
            setNegativeButton()
        }

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val newUseAlternativeSpeedLimits = dialogBinding.checkSpeedLimit.isChecked

            if (useAlternativeSpeedLimits != newUseAlternativeSpeedLimits) {
                viewModel.toggleSpeedLimitsMode(serverId, !newUseAlternativeSpeedLimits)
                dialog.dismiss()
                return@setOnClickListener
            }

            fun convertSpeedToBytes(speed: String, unit: Int): Int? {
                if (speed.isEmpty()) {
                    return 0
                }

                val limit = speed.toIntOrNull() ?: return null
                return when (unit) {
                    0 -> {
                        if (limit > 2_000_000) {
                            null
                        } else {
                            limit * 1024
                        }
                    }
                    1 -> {
                        if (limit > 2_000_000 / 1024) {
                            null
                        } else {
                            limit * 1024 * 1024
                        }
                    }
                    else -> null
                }
            }

            val download = convertSpeedToBytes(
                speed = dialogBinding.inputLayoutDownload.text,
                unit = dialogBinding.dropdownDlspeedLimitUnit.position
            )
            val upload = convertSpeedToBytes(
                speed = dialogBinding.inputLayoutUpload.text,
                unit = dialogBinding.dropdownUpspeedLimitUnit.position
            )

            if (download == null) {
                dialogBinding.inputLayoutDownload.error = getString(R.string.torrent_add_speed_limit_too_big)
            } else {
                dialogBinding.inputLayoutDownload.isErrorEnabled = false
            }

            if (upload == null) {
                dialogBinding.inputLayoutUpload.error = getString(R.string.torrent_add_speed_limit_too_big)
            } else {
                dialogBinding.inputLayoutUpload.isErrorEnabled = false
            }

            if (download != null && upload != null) {
                if (downloadSpeedLimit != download || uploadSpeedLimit != upload) {
                    viewModel.setSpeedLimits(
                        serverId = serverId,
                        download = if (downloadSpeedLimit != download) download else null,
                        upload = if (uploadSpeedLimit != upload) upload else null
                    )
                }

                dialog.dismiss()
            }
        }
    }

    private fun showCreateEditCategoryDialog(name: String?, parent: String?) {
        val category = if (name != null) {
            val fullName = parent ?: name
            viewModel.mainData.value?.categories?.find { it.name == fullName } ?: return
        } else {
            null
        }

        lateinit var dialogBinding: DialogCreateEditCategoryBinding

        val dialog = showDialog(DialogCreateEditCategoryBinding::inflate) { binding ->
            dialogBinding = binding

            binding.dropdownDownloadPath.setItems(
                R.string.torrent_list_create_category_download_path_default,
                R.string.torrent_list_create_category_download_path_yes,
                R.string.torrent_list_create_category_download_path_no
            )

            binding.dropdownDownloadPath.onItemChangeListener = { position ->
                if (position == 1) {
                    binding.inputLayoutDownloadPath.isEnabled = true
                    binding.inputLayoutDownloadPath.editText?.inputType = InputType.TYPE_CLASS_TEXT
                } else {
                    binding.inputLayoutDownloadPath.isEnabled = false
                    binding.inputLayoutDownloadPath.editText?.inputType = InputType.TYPE_NULL
                }
            }

            if (category == null) {
                if (parent == null) {
                    setTitle(R.string.torrent_list_create_category)
                } else {
                    setTitle(R.string.torrent_list_create_subcategory)
                }
            } else {
                binding.inputLayoutName.isEnabled = false
                binding.inputLayoutName.editText?.inputType = InputType.TYPE_NULL

                binding.inputLayoutName.setTextWithoutAnimation(name)
                binding.inputLayoutSavePath.setTextWithoutAnimation(category.savePath)

                when (category.downloadPath) {
                    Category.DownloadPath.Default -> {
                        binding.dropdownDownloadPath.setPosition(0)
                    }
                    is Category.DownloadPath.Yes -> {
                        binding.dropdownDownloadPath.setPosition(1)
                        binding.inputLayoutDownloadPath.setTextWithoutAnimation(category.downloadPath.path)
                    }
                    Category.DownloadPath.No -> {
                        binding.dropdownDownloadPath.setPosition(2)
                    }
                }

                setTitle(R.string.torrent_list_edit_category)
            }

            setPositiveButton()
            setNegativeButton()
        }

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            if (category == null) {
                if (dialogBinding.inputLayoutName.text.isEmpty()) {
                    dialogBinding.inputLayoutName.error =
                        getString(R.string.torrent_list_create_category_name_cannot_be_empty)
                    return@setOnClickListener
                } else {
                    dialogBinding.inputLayoutName.isErrorEnabled = false
                }
            }

            val downloadPathEnabled = when (dialogBinding.dropdownDownloadPath.position) {
                1 -> true
                2 -> false
                else -> null
            }

            if (category == null) {
                val categoryName = if (parent == null) {
                    dialogBinding.inputLayoutName.text
                } else {
                    "$parent/${dialogBinding.inputLayoutName.text}"
                }

                viewModel.createCategory(
                    serverId = serverId,
                    name = categoryName,
                    savePath = dialogBinding.inputLayoutSavePath.text,
                    downloadPathEnabled = downloadPathEnabled,
                    downloadPath = dialogBinding.inputLayoutDownloadPath.text
                )
            } else {
                viewModel.editCategory(
                    serverId = serverId,
                    name = category.name,
                    savePath = dialogBinding.inputLayoutSavePath.text,
                    downloadPathEnabled = downloadPathEnabled,
                    downloadPath = dialogBinding.inputLayoutDownloadPath.text
                )
            }

            dialog.dismiss()
        }
    }

    private fun showDeleteCategoryTagDialog(isCategory: Boolean, name: String) {
        showDialog {
            if (isCategory) {
                setTitle(R.string.torrent_list_delete_category)
                setMessage(getString(R.string.torrent_list_delete_category_confirm, name))
                setPositiveButton { _, _ ->
                    viewModel.deleteCategory(serverId, name)
                }
            } else {
                setTitle(R.string.torrent_list_delete_tag)
                setMessage(getString(R.string.torrent_list_delete_tag_desc, name))
                setPositiveButton { _, _ ->
                    viewModel.deleteTag(serverId, name)
                }
            }

            setNegativeButton()
        }
    }

    private fun showCreateTagDialog() {
        lateinit var dialogBinding: DialogCreateTagBinding

        val dialog = showDialog(DialogCreateTagBinding::inflate) { binding ->
            dialogBinding = binding

            setTitle(R.string.torrent_list_create_tag)
            setPositiveButton { _, _ ->
                viewModel.createTags(
                    serverId,
                    binding.editName.text.toString().split("\n")
                )
            }
            setNegativeButton()
        }

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val text = dialogBinding.editName.text.toString()
            if (text.isBlank()) {
                dialogBinding.inputLayoutName.error = getString(R.string.torrent_list_create_tag_name_cannot_be_empty)
                return@setOnClickListener
            } else {
                dialogBinding.inputLayoutName.isErrorEnabled = false
            }

            viewModel.createTags(
                serverId = serverId,
                names = text.split("\n")
            )
            dialog.dismiss()
        }
    }

    private fun showStatsDialog() {
        showDialog(DialogServerStatsBinding::inflate) { binding ->
            setTitle(R.string.torrent_list_action_statistics)
            setPositiveButton()

            val mainDataJob =
                viewModel.mainData.filterNotNull().launchAndCollectLatestIn(viewLifecycleOwner) { mainData ->
                    val state = mainData.serverState

                    binding.textAllTimeUpload.text = formatBytes(requireContext(), state.allTimeUpload)
                    binding.textAllTimeDownload.text = formatBytes(requireContext(), state.allTimeDownload)
                    binding.textAllTimeShareRatio.text = state.globalRatio
                    binding.textSessionWaste.text = formatBytes(requireContext(), state.sessionWaste)
                    binding.textConnectedPeers.text = state.connectedPeers.toString()
                    binding.textTotalBufferSize.text = formatBytes(requireContext(), state.bufferSize)
                    binding.textWriteCacheOverload.text =
                        getString(R.string.stats_percentage_format, state.writeCacheOverload)
                    binding.textReadCacheOverload.text =
                        getString(R.string.stats_percentage_format, state.readCacheOverload)
                    binding.textQueuedIoJobs.text = state.queuedIOJobs.toString()
                    binding.textAverageTimeInQueue.text = getString(R.string.stats_ms_format, state.averageTimeInQueue)
                    binding.textTotalQueuedSize.text = formatBytes(requireContext(), state.queuedSize)
                }

            setOnDismissListener {
                mainDataJob.cancel()
            }
        }
    }

    private fun showShutdownDialog() {
        showDialog {
            setTitle(R.string.torrent_list_action_shutdown)
            setMessage(R.string.torrent_list_shutdown_confirm)
            setPositiveButton { _, _ ->
                viewModel.shutdown(serverId)
            }
            setNegativeButton()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        activityBinding.layoutDrawer.removeDrawerListener(drawerListener)
        parentActivity.removeAdapter(parentAdapter)
    }
}
