package dev.bartuzen.qbitcontroller.ui.torrentlist

import android.app.Activity
import android.content.Intent
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
import androidx.drawerlayout.widget.DrawerLayout.DrawerListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.hannesdorfmann.fragmentargs.annotation.Arg
import com.hannesdorfmann.fragmentargs.annotation.FragmentWithArgs
import dagger.hilt.android.AndroidEntryPoint
import dev.bartuzen.qbitcontroller.R
import dev.bartuzen.qbitcontroller.data.TorrentSort
import dev.bartuzen.qbitcontroller.databinding.ActivityMainBinding
import dev.bartuzen.qbitcontroller.databinding.DialogTorrentDeleteBinding
import dev.bartuzen.qbitcontroller.databinding.FragmentTorrentListBinding
import dev.bartuzen.qbitcontroller.model.ServerConfig
import dev.bartuzen.qbitcontroller.ui.base.ArgsFragment
import dev.bartuzen.qbitcontroller.ui.torrent.TorrentActivity
import dev.bartuzen.qbitcontroller.utils.getErrorMessage
import dev.bartuzen.qbitcontroller.utils.launchAndCollectIn
import dev.bartuzen.qbitcontroller.utils.launchAndCollectLatestIn
import dev.bartuzen.qbitcontroller.utils.setItemMargin
import dev.bartuzen.qbitcontroller.utils.showSnackbar
import dev.bartuzen.qbitcontroller.utils.view
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

@FragmentWithArgs
@AndroidEntryPoint
class TorrentListFragment : ArgsFragment(R.layout.fragment_torrent_list) {
    private var _binding: FragmentTorrentListBinding? = null
    private val binding get() = _binding!!

    private var _activityBinding: ActivityMainBinding? = null
    private val activityBinding get() = _activityBinding!!

    private val viewModel: TorrentListViewModel by viewModels()

    @Arg
    lateinit var serverConfig: ServerConfig

    private lateinit var drawerListener: DrawerListener

    private val startTorrentActivity =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val isTorrentDeleted = result.data?.getBooleanExtra(
                    TorrentActivity.Extras.TORRENT_DELETED, false
                ) ?: false
                if (isTorrentDeleted) {
                    viewModel.loadTorrentList(serverConfig)
                    showSnackbar(getString(R.string.torrent_deleted_success))
                }
            }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentTorrentListBinding.bind(view)
        _activityBinding = ActivityMainBinding.bind(requireActivity().view)

        requireActivity().addMenuProvider(object : MenuProvider {
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

                val settingsItem = menu.findItem(R.id.menu_settings)
                val sortItem = menu.findItem(R.id.menu_sort)
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
                        settingsItem.isVisible = false
                        sortItem.isVisible = false

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
                val sort = when (menuItem.itemId) {
                    R.id.menu_sort_name -> TorrentSort.NAME
                    R.id.menu_sort_hash -> TorrentSort.HASH
                    R.id.menu_sort_dlspeed -> TorrentSort.DOWNLOAD_SPEED
                    R.id.menu_sort_upspeed -> TorrentSort.UPLOAD_SPEED
                    R.id.menu_sort_priority -> TorrentSort.PRIORITY
                    else -> return false
                }
                viewModel.setTorrentSort(sort)

                viewModel.loadTorrentList(serverConfig, sort)

                return true
            }

        }, viewLifecycleOwner)

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

                    override fun onActionItemClicked(mode: ActionMode, item: MenuItem) =
                        when (item.itemId) {
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
        binding.recyclerTorrentList.setItemMargin(8, 8)

        drawerListener = object : DrawerListener {
            override fun onDrawerStateChanged(newState: Int) {
                actionMode?.finish()
            }

            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {}

            override fun onDrawerOpened(drawerView: View) {}

            override fun onDrawerClosed(drawerView: View) {}
        }
        activityBinding.layoutDrawer.addDrawerListener(drawerListener)

        combine(viewModel.torrentList, viewModel.searchQuery) { torrentList, searchQuery ->
            if (torrentList != null) {
                torrentList to searchQuery
            } else {
                null
            }
        }.filterNotNull().launchAndCollectLatestIn(viewLifecycleOwner) { (torrentList, query) ->
            val list = if (query.isEmpty()) {
                torrentList
            } else {
                torrentList.filter { torrent -> torrent.name.contains(query, ignoreCase = true) }
            }
            adapter.submitList(list)
        }

        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refreshTorrentList(serverConfig)
        }

        if (!viewModel.isInitialLoadStarted) {
            viewModel.isInitialLoadStarted = true
            viewModel.loadTorrentList(serverConfig)
        }

        viewModel.isLoading.launchAndCollectLatestIn(viewLifecycleOwner) { isLoading ->
            binding.progressIndicator.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.isRefreshing.launchAndCollectLatestIn(viewLifecycleOwner) { isRefreshing ->
            binding.swipeRefresh.isRefreshing = isRefreshing
        }

        viewModel.eventFlow.launchAndCollectIn(viewLifecycleOwner) { event ->
            when (event) {
                is TorrentListViewModel.Event.Error -> {
                    showSnackbar(getErrorMessage(requireContext(), event.result))
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
            }
        }
    }

    private fun showDeleteTorrentsDialog(adapter: TorrentListAdapter, actionMode: ActionMode?) {
        val dialogBinding = DialogTorrentDeleteBinding.inflate(layoutInflater)

        MaterialAlertDialogBuilder(requireActivity())
            .setTitle(
                resources.getQuantityString(
                    R.plurals.torrent_list_delete_torrents,
                    adapter.selectedItemCount,
                    adapter.selectedItemCount
                )
            )
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.dialog_ok) { _, _ ->
                viewModel.deleteTorrents(
                    serverConfig,
                    adapter.selectedItems.toList(),
                    dialogBinding.checkBoxDeleteFiles.isChecked
                )
                adapter.finishSelection()
                actionMode?.finish()
            }
            .setNegativeButton(R.string.dialog_cancel, null)
            .create()
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null

        activityBinding.layoutDrawer.removeDrawerListener(drawerListener)
        _activityBinding = null
    }
}
