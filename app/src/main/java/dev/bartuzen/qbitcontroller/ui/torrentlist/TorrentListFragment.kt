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
import dev.bartuzen.qbitcontroller.databinding.DialogCreateCategoryBinding
import dev.bartuzen.qbitcontroller.databinding.DialogCreateTagBinding
import dev.bartuzen.qbitcontroller.databinding.DialogTorrentDeleteBinding
import dev.bartuzen.qbitcontroller.databinding.DialogTorrentLocationBinding
import dev.bartuzen.qbitcontroller.databinding.FragmentTorrentListBinding
import dev.bartuzen.qbitcontroller.ui.addtorrent.AddTorrentActivity
import dev.bartuzen.qbitcontroller.ui.main.MainActivity
import dev.bartuzen.qbitcontroller.ui.rss.RssActivity
import dev.bartuzen.qbitcontroller.ui.torrent.TorrentActivity
import dev.bartuzen.qbitcontroller.utils.getErrorMessage
import dev.bartuzen.qbitcontroller.utils.launchAndCollectIn
import dev.bartuzen.qbitcontroller.utils.launchAndCollectLatestIn
import dev.bartuzen.qbitcontroller.utils.setNegativeButton
import dev.bartuzen.qbitcontroller.utils.setPositiveButton
import dev.bartuzen.qbitcontroller.utils.setTextWithoutAnimation
import dev.bartuzen.qbitcontroller.utils.showDialog
import dev.bartuzen.qbitcontroller.utils.showSnackbar
import dev.bartuzen.qbitcontroller.utils.toPx
import dev.bartuzen.qbitcontroller.utils.view
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
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
                    viewModel.loadTorrentList(serverId)
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
                    viewModel.loadTorrentList(serverId)
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
                        R.id.menu_priority_maximize -> {
                            viewModel.maximizeTorrentPriority(
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
                                viewModel.torrentList.value
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
                    showCreateCategoryDialog()
                } else {
                    showCreateTagDialog()
                }
            }
        )
        parentAdapter = ConcatAdapter(torrentFilterAdapter, categoryTagAdapter)

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

        viewModel.filteredTorrentList.filterNotNull().launchAndCollectLatestIn(viewLifecycleOwner) { torrentList ->
            adapter.submitList(torrentList)
        }

        viewModel.torrentList.filterNotNull().launchAndCollectLatestIn(viewLifecycleOwner) { torrentList ->
            val countMap = mutableMapOf<TorrentFilter, Int>()

            torrentList.forEach { torrent ->
                TorrentFilter.values().forEach { filter ->
                    if (filter.states == null || torrent.state in filter.states) {
                        countMap[filter] = (countMap[filter] ?: 0) + 1
                    }
                }
            }

            torrentFilterAdapter.submitCountMap(countMap)
        }

        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refreshTorrentListCategoryTags(serverId)
        }

        if (!viewModel.isInitialLoadStarted) {
            viewModel.isInitialLoadStarted = true
            viewModel.loadTorrentList(serverId)
            viewModel.updateCategoryAndTags(serverId)
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

        combine(viewModel.categoryList, viewModel.tagList) { categoryList, tagList ->
            if (categoryList != null && tagList != null) {
                categoryList to tagList
            } else {
                null
            }
        }.filterNotNull().launchAndCollectLatestIn(viewLifecycleOwner) { (categoryList, tagList) ->
            categoryTagAdapter.submitLists(categoryList.map { it.name }, tagList)
        }

        viewModel.autoRefreshInterval.launchAndCollectLatestIn(viewLifecycleOwner) { interval ->
            if (interval != 0) {
                while (isActive) {
                    delay(interval * 1000L)
                    if (isActive && actionMode == null) {
                        viewModel.loadTorrentList(serverId, autoRefresh = true)
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

                    viewModel.loadTorrentList(serverId)
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
                        viewModel.loadTorrentList(serverId)
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
                        viewModel.loadTorrentList(serverId)
                    }
                }
                is TorrentListViewModel.Event.CategoryDeleted -> {
                    showSnackbar(
                        getString(R.string.torrent_list_delete_category_success, event.name)
                    )
                    viewModel.loadTorrentList(serverId)
                    viewModel.updateCategoryAndTags(serverId)
                }
                is TorrentListViewModel.Event.TagDeleted -> {
                    showSnackbar(
                        getString(R.string.torrent_list_delete_tag_success, event.name)
                    )
                    viewModel.loadTorrentList(serverId)
                    viewModel.updateCategoryAndTags(serverId)
                }
                TorrentListViewModel.Event.TorrentsPriorityDecreased -> {
                    showSnackbar(R.string.torrent_list_priority_decreased_success)
                    viewLifecycleOwner.lifecycleScope.launch {
                        delay(1000) // wait until qBittorrent changes the priority
                        viewModel.loadTorrentList(serverId)
                    }
                }
                TorrentListViewModel.Event.TorrentsPriorityIncreased -> {
                    showSnackbar(R.string.torrent_list_priority_increased_success)
                    viewLifecycleOwner.lifecycleScope.launch {
                        delay(1000) // wait until qBittorrent changes the priority
                        viewModel.loadTorrentList(serverId)
                    }
                }
                TorrentListViewModel.Event.TorrentsPriorityMaximized -> {
                    showSnackbar(R.string.torrent_list_priority_maximized_success)
                    viewLifecycleOwner.lifecycleScope.launch {
                        delay(1000) // wait until qBittorrent changes the priority
                        viewModel.loadTorrentList(serverId)
                    }
                }
                TorrentListViewModel.Event.TorrentsPriorityMinimized -> {
                    showSnackbar(R.string.torrent_list_priority_minimized_success)
                    viewLifecycleOwner.lifecycleScope.launch {
                        delay(1000) // wait until qBittorrent changes the priority
                        viewModel.loadTorrentList(serverId)
                    }
                }
                TorrentListViewModel.Event.LocationUpdated -> {
                    showSnackbar(R.string.torrent_location_update_success)
                    viewModel.loadTorrentList(serverId)
                }
                TorrentListViewModel.Event.CategoryCreated -> {
                    showSnackbar(R.string.torrent_list_create_category_success)
                    viewModel.updateCategoryAndTags(serverId)
                }
                TorrentListViewModel.Event.CategoryEdited -> {
                    showSnackbar(R.string.torrent_list_edit_category_success)
                    viewModel.updateCategoryAndTags(serverId)
                }
                TorrentListViewModel.Event.TagCreated -> {
                    showSnackbar(R.string.torrent_list_create_tag_success)
                    viewModel.updateCategoryAndTags(serverId)
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
                        showRenameCategoryDialog(name)
                    }
                    1 -> {
                        showDeleteCategoryTagDialog(true, name)
                    }
                }
            }
            setNegativeButton()
        }
    }

    private fun showRenameCategoryDialog(name: String) {
        showDialog(DialogCreateCategoryBinding::inflate) { binding ->
            val savePath = viewModel.categoryList.value?.find { it.name == name }?.savePath ?: return@showDialog

            binding.editName.isEnabled = false
            binding.editName.inputType = InputType.TYPE_NULL

            binding.inputLayoutName.setTextWithoutAnimation(name)
            binding.inputLayoutSavePath.setTextWithoutAnimation(savePath)

            setTitle(R.string.torrent_list_edit_category_title)
            setPositiveButton { _, _ ->
                viewModel.editCategory(
                    serverId,
                    name,
                    binding.editSavePath.text.toString()
                )
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
                    viewModel.deleteCategory(serverId, name)
                } else {
                    viewModel.deleteTag(serverId, name)
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
                    serverId,
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
                    serverId,
                    binding.editName.text.toString().split("\n")
                )
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
