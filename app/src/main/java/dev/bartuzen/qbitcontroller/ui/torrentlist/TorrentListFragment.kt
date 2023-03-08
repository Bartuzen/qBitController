package dev.bartuzen.qbitcontroller.ui.torrentlist

import android.app.Activity
import android.content.Intent
import android.graphics.Rect
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
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.SearchView.OnQueryTextListener
import androidx.core.os.bundleOf
import androidx.core.view.MenuProvider
import androidx.core.view.iterator
import androidx.drawerlayout.widget.DrawerLayout.DrawerListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.RecyclerView
import by.kirich1409.viewbindingdelegate.viewBinding
import dagger.hilt.android.AndroidEntryPoint
import dev.bartuzen.qbitcontroller.R
import dev.bartuzen.qbitcontroller.data.TorrentSort
import dev.bartuzen.qbitcontroller.databinding.ActivityMainBinding
import dev.bartuzen.qbitcontroller.databinding.DialogCreateEditCategoryBinding
import dev.bartuzen.qbitcontroller.databinding.DialogCreateTagBinding
import dev.bartuzen.qbitcontroller.databinding.DialogServerStatsBinding
import dev.bartuzen.qbitcontroller.databinding.DialogSpeedLimitBinding
import dev.bartuzen.qbitcontroller.databinding.DialogTorrentDeleteBinding
import dev.bartuzen.qbitcontroller.databinding.DialogTorrentLocationBinding
import dev.bartuzen.qbitcontroller.databinding.FragmentTorrentListBinding
import dev.bartuzen.qbitcontroller.model.Category
import dev.bartuzen.qbitcontroller.ui.addtorrent.AddTorrentActivity
import dev.bartuzen.qbitcontroller.ui.log.LogActivity
import dev.bartuzen.qbitcontroller.ui.main.MainActivity
import dev.bartuzen.qbitcontroller.ui.rss.RssActivity
import dev.bartuzen.qbitcontroller.ui.torrent.TorrentActivity
import dev.bartuzen.qbitcontroller.utils.formatBytes
import dev.bartuzen.qbitcontroller.utils.formatBytesPerSecond
import dev.bartuzen.qbitcontroller.utils.getErrorMessage
import dev.bartuzen.qbitcontroller.utils.launchAndCollectIn
import dev.bartuzen.qbitcontroller.utils.launchAndCollectLatestIn
import dev.bartuzen.qbitcontroller.utils.requireAppCompatActivity
import dev.bartuzen.qbitcontroller.utils.setNegativeButton
import dev.bartuzen.qbitcontroller.utils.setPositiveButton
import dev.bartuzen.qbitcontroller.utils.setTextWithoutAnimation
import dev.bartuzen.qbitcontroller.utils.showDialog
import dev.bartuzen.qbitcontroller.utils.showSnackbar
import dev.bartuzen.qbitcontroller.utils.text
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
        requireActivity().addMenuProvider(
            object : MenuProvider {
                override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                    menuInflater.inflate(R.menu.torrent_list, menu)

                    viewModel.torrentSort.launchAndCollectLatestIn(viewLifecycleOwner) { sort ->
                        val selectedSort = when (sort) {
                            TorrentSort.HASH -> R.id.menu_sort_hash
                            TorrentSort.NAME -> R.id.menu_sort_name
                            TorrentSort.DOWNLOAD_SPEED -> R.id.menu_sort_dlspeed
                            TorrentSort.UPLOAD_SPEED -> R.id.menu_sort_upspeed
                            TorrentSort.PRIORITY -> R.id.menu_sort_priority
                            TorrentSort.ETA -> R.id.menu_sort_eta
                            TorrentSort.SIZE -> R.id.menu_sort_size
                            TorrentSort.PROGRESS -> R.id.menu_sort_progress
                            TorrentSort.CONNECTED_SEEDS -> R.id.menu_sort_connected_seeds
                            TorrentSort.TOTAL_SEEDS -> R.id.menu_sort_total_seeds
                            TorrentSort.CONNECTED_LEECHES -> R.id.menu_sort_connected_leeches
                            TorrentSort.TOTAL_LEECHES -> R.id.menu_sort_total_leeches
                            TorrentSort.ADDITION_DATE -> R.id.menu_sort_addition_date
                            TorrentSort.COMPLETION_DATE -> R.id.menu_sort_completion_date
                        }
                        menu.findItem(selectedSort).isChecked = true
                    }

                    viewModel.isReverseSorting.launchAndCollectLatestIn(viewLifecycleOwner) { isReverseSorting ->
                        menu.findItem(R.id.menu_sort_reverse).isChecked = isReverseSorting
                    }

                    viewModel.mainData.launchAndCollectLatestIn(viewLifecycleOwner) { mainData ->
                        val speedLimitMode = menu.findItem(R.id.menu_speed_limit_mode)
                        val stats = menu.findItem(R.id.menu_stats)

                        speedLimitMode.isEnabled = mainData != null
                        speedLimitMode.isChecked = mainData?.serverState?.useAlternativeSpeedLimits == true

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
                        R.id.menu_log -> {
                            val intent = Intent(requireActivity(), LogActivity::class.java).apply {
                                putExtra(LogActivity.Extras.SERVER_ID, serverId)
                            }
                            startActivity(intent)
                        }
                        R.id.menu_sort_name -> {
                            viewModel.setTorrentSort(TorrentSort.NAME)
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
                        R.id.menu_sort_priority -> {
                            viewModel.setTorrentSort(TorrentSort.PRIORITY)
                        }
                        R.id.menu_sort_reverse -> {
                            viewModel.changeReverseSorting()
                        }
                        R.id.menu_speed_limit_mode -> {
                            val isCurrentLimitAlternative =
                                viewModel.mainData.value?.serverState?.useAlternativeSpeedLimits == true
                            viewModel.toggleSpeedLimitsMode(serverId, isCurrentLimitAlternative)
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

                    override fun onActionItemClicked(mode: ActionMode, item: MenuItem) = when (item.itemId) {
                        R.id.menu_delete -> {
                            showDeleteTorrentsDialog(this@apply, actionMode)
                            true
                        }
                        R.id.menu_pause -> {
                            viewModel.pauseTorrents(serverId, selectedItems.toList())
                            finishSelection()
                            actionMode?.finish()
                            true
                        }
                        R.id.menu_resume -> {
                            viewModel.resumeTorrents(serverId, selectedItems.toList())
                            finishSelection()
                            actionMode?.finish()
                            true
                        }
                        R.id.menu_priority_maximize -> {
                            viewModel.maximizeTorrentPriority(
                                serverId,
                                selectedItems.toList()
                            )
                            actionMode?.finish()
                            true
                        }
                        R.id.menu_priority_increase -> {
                            viewModel.increaseTorrentPriority(
                                serverId,
                                selectedItems.toList()
                            )
                            actionMode?.finish()
                            true
                        }
                        R.id.menu_priority_decrease -> {
                            viewModel.decreaseTorrentPriority(
                                serverId,
                                selectedItems.toList()
                            )
                            actionMode?.finish()
                            true
                        }
                        R.id.menu_priority_minimize -> {
                            viewModel.minimizeTorrentPriority(
                                serverId,
                                selectedItems.toList()
                            )
                            actionMode?.finish()
                            true
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
                                }
                            )
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

        val torrentFilterAdapter = TorrentFilterAdapter(
            onClick = { filter ->
                viewModel.setSelectedFilter(filter)

                activityBinding.layoutDrawer.close()
            }
        )

        val categoryTagAdapter = CategoryTagAdapter(
            onSelected = { categoryTag ->
                when (categoryTag) {
                    is CategoryTag.ICategory -> {
                        viewModel.setSelectedCategory(categoryTag)
                    }
                    is CategoryTag.ITag -> {
                        viewModel.setSelectedTag(categoryTag)
                    }
                }

                activityBinding.layoutDrawer.close()
            },
            onLongClick = { isCategory, name ->
                activityBinding.layoutDrawer.close()

                if (isCategory) {
                    showCategoryLongClickDialog(name)
                } else {
                    showDeleteCategoryTagDialog(false, name)
                }
            },
            onCreateClick = { isCategory ->
                activityBinding.layoutDrawer.close()
                if (isCategory) {
                    showCreateEditCategoryDialog(null)
                } else {
                    showCreateTagDialog()
                }
            }
        )

        val trackerAdapter = TrackerAdapter(
            onSelected = { tracker ->
                viewModel.setSelectedTracker(tracker)
                activityBinding.layoutDrawer.close()
            }
        )

        parentAdapter = ConcatAdapter(torrentFilterAdapter, categoryTagAdapter, trackerAdapter)

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
                showSpeedLimitsDialog(serverState.downloadSpeedLimit, serverState.uploadSpeedLimit)
            }
        }

        viewModel.filteredTorrentList.filterNotNull().launchAndCollectLatestIn(viewLifecycleOwner) { torrentList ->
            adapter.submitList(torrentList)
        }

        viewModel.mainData.filterNotNull().launchAndCollectLatestIn(viewLifecycleOwner) { mainData ->
            categoryTagAdapter.submitLists(mainData.categories.map { it.name }, mainData.tags)
            trackerAdapter.submitTrackers(
                trackers = mainData.trackers,
                allCount = mainData.torrents.size,
                trackerlessCount = mainData.torrents.count { it.trackerCount == 0 }
            )

            binding.textSpeed.text = getString(
                R.string.torrent_list_speed_format,
                formatBytesPerSecond(requireContext(), mainData.serverState.uploadSpeed),
                formatBytes(requireContext(), mainData.serverState.uploadSession),
                formatBytesPerSecond(requireContext(), mainData.serverState.downloadSpeed),
                formatBytes(requireContext(), mainData.serverState.downloadSession)
            )

            val countMap = mutableMapOf<TorrentFilter, Int>()

            mainData.torrents.forEach { torrent ->
                TorrentFilter.values().forEach { filter ->
                    when (filter) {
                        TorrentFilter.ACTIVE -> {
                            if (torrent.downloadSpeed != 0L || torrent.uploadSpeed != 0L) {
                                countMap[filter] = (countMap[filter] ?: 0) + 1
                            }
                        }
                        TorrentFilter.INACTIVE -> {
                            if (torrent.downloadSpeed == 0L && torrent.uploadSpeed == 0L) {
                                countMap[filter] = (countMap[filter] ?: 0) + 1
                            }
                        }
                        else -> {
                            if (filter.states == null || torrent.state in filter.states) {
                                countMap[filter] = (countMap[filter] ?: 0) + 1
                            }
                        }
                    }
                }
            }

            torrentFilterAdapter.submitCountMap(countMap)

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

        viewModel.isNaturalLoading.launchAndCollectLatestIn(viewLifecycleOwner) { isNaturalLoading ->
            val autoRefreshLoadingBar = viewModel.autoRefreshHideLoadingBar.value
            binding.progressIndicator.visibility =
                if (isNaturalLoading == true || isNaturalLoading == false && !autoRefreshLoadingBar) {
                    View.VISIBLE
                } else {
                    View.GONE
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
                    showSnackbar(R.string.torrent_list_edit_category_fail)
                }
                is TorrentListViewModel.Event.TorrentsDeleted -> {
                    showSnackbar(
                        resources.getQuantityString(
                            R.plurals.torrent_list_torrents_deleted_success,
                            event.count,
                            event.count
                        )
                    )
                    viewModel.loadMainData(serverId)
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
                        viewModel.loadMainData(serverId)
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
                    showSnackbar(R.string.torrent_list_priority_decreased_success)
                    viewLifecycleOwner.lifecycleScope.launch {
                        delay(1000) // wait until qBittorrent changes the priority
                        viewModel.loadMainData(serverId)
                    }
                }
                TorrentListViewModel.Event.TorrentsPriorityIncreased -> {
                    showSnackbar(R.string.torrent_list_priority_increased_success)
                    viewLifecycleOwner.lifecycleScope.launch {
                        delay(1000) // wait until qBittorrent changes the priority
                        viewModel.loadMainData(serverId)
                    }
                }
                TorrentListViewModel.Event.TorrentsPriorityMaximized -> {
                    showSnackbar(R.string.torrent_list_priority_maximized_success)
                    viewLifecycleOwner.lifecycleScope.launch {
                        delay(1000) // wait until qBittorrent changes the priority
                        viewModel.loadMainData(serverId)
                    }
                }
                TorrentListViewModel.Event.TorrentsPriorityMinimized -> {
                    showSnackbar(R.string.torrent_list_priority_minimized_success)
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
                viewModel.deleteTorrents(serverId, adapter.selectedItems.toList(), binding.checkDeleteFiles.isChecked)
                adapter.finishSelection()
                actionMode?.finish()
            }
            setNegativeButton()
        }
    }

    private fun showLocationDialog(currentLocation: String?, onSuccess: (newLocation: String) -> Unit) {
        lateinit var dialogBinding: DialogTorrentLocationBinding

        val dialog = showDialog(DialogTorrentLocationBinding::inflate) { binding ->
            dialogBinding = binding

            binding.inputLayoutLocation.setTextWithoutAnimation(currentLocation)

            setTitle(R.string.torrent_location_dialog_title)
            setPositiveButton()
            setNegativeButton()
        }

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val newLocation = dialogBinding.editLocation.text.toString()
            if (newLocation.isNotBlank()) {
                onSuccess(newLocation)
                dialog.dismiss()
            } else {
                dialogBinding.inputLayoutLocation.error = getString(R.string.torrent_location_cannot_be_blank)
            }
        }
    }

    private fun showSpeedLimitsDialog(downloadSpeedLimit: Int, uploadSpeedLimit: Int) {
        lateinit var dialogBinding: DialogSpeedLimitBinding

        val dialog = showDialog(DialogSpeedLimitBinding::inflate) { binding ->
            dialogBinding = binding

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

    private fun showCategoryLongClickDialog(name: String) {
        showDialog {
            setTitle(name)
            setItems(
                arrayOf(
                    getString(R.string.torrent_list_edit_category_title),
                    getString(R.string.torrent_list_delete_category_title)
                )
            ) { _, which ->
                when (which) {
                    0 -> {
                        showCreateEditCategoryDialog(name)
                    }
                    1 -> {
                        showDeleteCategoryTagDialog(true, name)
                    }
                }
            }
            setNegativeButton()
        }
    }

    private fun showCreateEditCategoryDialog(name: String?) {
        val category = if (name != null) {
            viewModel.mainData.value?.categories?.find { it.name == name } ?: return
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
                    binding.editDownloadPath.isEnabled = true
                    binding.editDownloadPath.inputType = InputType.TYPE_CLASS_TEXT
                } else {
                    binding.editDownloadPath.isEnabled = false
                    binding.editDownloadPath.inputType = InputType.TYPE_NULL
                }
            }

            if (category == null) {
                setTitle(R.string.torrent_list_create_category_title)
            } else {
                binding.editName.isEnabled = false
                binding.editName.inputType = InputType.TYPE_NULL

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

                setTitle(R.string.torrent_list_edit_category_title)
            }

            setPositiveButton()
            setNegativeButton()
        }

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            if (category == null) {
                if (dialogBinding.editName.text.toString().isEmpty()) {
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
                viewModel.createCategory(
                    serverId = serverId,
                    name = dialogBinding.editName.text.toString(),
                    savePath = dialogBinding.editSavePath.text.toString(),
                    downloadPathEnabled = downloadPathEnabled,
                    downloadPath = dialogBinding.editDownloadPath.text.toString()
                )
            } else {
                viewModel.editCategory(
                    serverId = serverId,
                    name = category.name,
                    savePath = dialogBinding.editSavePath.text.toString(),
                    downloadPathEnabled = downloadPathEnabled,
                    downloadPath = dialogBinding.editDownloadPath.text.toString()
                )
            }

            dialog.dismiss()
        }
    }

    private fun showDeleteCategoryTagDialog(isCategory: Boolean, name: String) {
        showDialog {
            if (isCategory) {
                setTitle(R.string.torrent_list_delete_category_title)
                setMessage(getString(R.string.torrent_list_delete_category_desc, name))
                setPositiveButton { _, _ ->
                    viewModel.deleteCategory(serverId, name)
                }
            } else {
                setTitle(R.string.torrent_list_delete_tag_title)
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

            setTitle(R.string.torrent_list_create_tag_title)
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
            setTitle(R.string.stats_dialog_title)
            setPositiveButton()

            val mainDataJob = viewModel.mainData.filterNotNull().launchAndCollectLatestIn(viewLifecycleOwner) { mainData ->
                val state = mainData.serverState

                binding.textAllTimeUpload.text = formatBytes(requireContext(), state.allTimeUpload)
                binding.textAllTimeDownload.text = formatBytes(requireContext(), state.allTimeDownload)
                binding.textAllTimeShareRatio.text = state.globalRatio
                binding.textSessionWaste.text = formatBytes(requireContext(), state.sessionWaste)
                binding.textConnectedPeers.text = state.connectedPeers.toString()
                binding.textTotalBufferSize.text = formatBytes(requireContext(), state.bufferSize)
                binding.textWriteCacheOverload.text = getString(R.string.stats_percentage_format, state.writeCacheOverload)
                binding.textReadCacheOverload.text = getString(R.string.stats_percentage_format, state.readCacheOverload)
                binding.textQueuedIoJobs.text = state.queuedIOJobs
                binding.textAverageTimeInQueue.text = getString(R.string.stats_ms_format, state.averageTimeInQueue)
                binding.textTotalQueuedSize.text = formatBytes(requireContext(), state.queuedSize)
            }

            setOnDismissListener {
                mainDataJob.cancel()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        activityBinding.layoutDrawer.removeDrawerListener(drawerListener)
        parentActivity.removeAdapter(parentAdapter)
    }
}
