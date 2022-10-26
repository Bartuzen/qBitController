package dev.bartuzen.qbitcontroller.ui.torrentlist

import android.content.Intent
import android.os.Bundle
import android.view.ActionMode
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
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
import dev.bartuzen.qbitcontroller.databinding.FragmentTorrentDeleteDialogBinding
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

                viewModel.isLoading.value = true
                viewModel.updateTorrentList(serverConfig, sort).invokeOnCompletion {
                    viewModel.isLoading.value = false
                }

                return true
            }

        }, viewLifecycleOwner)

        var actionMode: ActionMode? = null
        lateinit var adapter: TorrentListAdapter
        adapter = TorrentListAdapter(
            onClick = { torrent ->
                val intent = Intent(context, TorrentActivity::class.java).apply {
                    putExtra(TorrentActivity.Extras.TORRENT_HASH, torrent.hash)
                    putExtra(TorrentActivity.Extras.SERVER_CONFIG, serverConfig)
                }
                startActivity(intent)
            }, onSelectionModeStart = {
                actionMode = requireActivity().startActionMode(object : ActionMode.Callback {
                    override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
                        mode.menuInflater.inflate(R.menu.torrent_list_selection_menu, menu)
                        return true
                    }

                    override fun onPrepareActionMode(mode: ActionMode, menu: Menu) = false

                    override fun onActionItemClicked(mode: ActionMode, item: MenuItem) =
                        when (item.itemId) {
                            R.id.menu_delete -> {
                                showDeleteTorrentsDialog(adapter, actionMode)
                                true
                            }
                            R.id.menu_pause -> {
                                viewModel.pauseTorrents(
                                    serverConfig, adapter.selectedTorrentHashes.toList()
                                )
                                adapter.finishSelection()
                                actionMode?.finish()
                                true
                            }
                            R.id.menu_resume -> {
                                viewModel.resumeTorrents(
                                    serverConfig, adapter.selectedTorrentHashes.toList()
                                )
                                adapter.finishSelection()
                                actionMode?.finish()
                                true
                            }
                            R.id.menu_select_all -> {
                                adapter.selectAll()
                                true
                            }
                            R.id.menu_select_inverse -> {
                                adapter.selectInverse()
                                true
                            }
                            else -> false
                        }

                    override fun onDestroyActionMode(mode: ActionMode) {
                        adapter.finishSelection()
                    }
                })
            }, onSelectionModeEnd = {
                actionMode?.finish()
            }, onUpdateSelection = { count ->
                if (count != 0) {
                    actionMode?.title = resources.getQuantityString(
                        R.plurals.torrent_list_torrents_selected,
                        count,
                        count
                    )
                }
            }
        )
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

        viewModel.torrentList.filterNotNull()
            .launchAndCollectLatestIn(viewLifecycleOwner) { torrentList ->
                adapter.submitList(torrentList)
            }

        binding.swipeRefresh.setOnRefreshListener {
            viewModel.isRefreshing.value = true
            viewModel.updateTorrentList(serverConfig).invokeOnCompletion {
                viewModel.isRefreshing.value = false
            }
        }

        if (!viewModel.isInitialLoadStarted) {
            viewModel.isInitialLoadStarted = true
            viewModel.updateTorrentList(serverConfig).invokeOnCompletion {
                viewModel.isLoading.value = false
            }
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

                    viewModel.isLoading.value = true
                    viewModel.updateTorrentList(serverConfig).invokeOnCompletion {
                        viewModel.isLoading.value = false
                    }
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
                        viewModel.isLoading.value = true
                        viewModel.updateTorrentList(serverConfig).invokeOnCompletion {
                            viewModel.isLoading.value = false
                        }
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
                        viewModel.isLoading.value = true
                        viewModel.updateTorrentList(serverConfig).invokeOnCompletion {
                            viewModel.isLoading.value = false
                        }
                    }
                }
            }
        }
    }

    private fun showDeleteTorrentsDialog(adapter: TorrentListAdapter, actionMode: ActionMode?) {
        val dialogBinding = FragmentTorrentDeleteDialogBinding.inflate(layoutInflater)

        MaterialAlertDialogBuilder(requireActivity())
            .setTitle(
                resources.getQuantityString(
                    R.plurals.torrent_list_delete_torrents,
                    adapter.selectedTorrentHashes.size,
                    adapter.selectedTorrentHashes.size
                )
            )
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.dialog_ok) { _, _ ->
                viewModel.deleteTorrents(
                    serverConfig,
                    adapter.selectedTorrentHashes,
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