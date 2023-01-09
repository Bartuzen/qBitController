package dev.bartuzen.qbitcontroller.ui.torrent.tabs.trackers

import android.graphics.Rect
import android.os.Bundle
import android.view.ActionMode
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.core.os.bundleOf
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import by.kirich1409.viewbindingdelegate.viewBinding
import dagger.hilt.android.AndroidEntryPoint
import dev.bartuzen.qbitcontroller.R
import dev.bartuzen.qbitcontroller.databinding.ActivityTorrentBinding
import dev.bartuzen.qbitcontroller.databinding.DialogTorrentTrackersAddBinding
import dev.bartuzen.qbitcontroller.databinding.FragmentTorrentTrackersBinding
import dev.bartuzen.qbitcontroller.model.ServerConfig
import dev.bartuzen.qbitcontroller.utils.getErrorMessage
import dev.bartuzen.qbitcontroller.utils.getParcelableCompat
import dev.bartuzen.qbitcontroller.utils.launchAndCollectIn
import dev.bartuzen.qbitcontroller.utils.launchAndCollectLatestIn
import dev.bartuzen.qbitcontroller.utils.setNegativeButton
import dev.bartuzen.qbitcontroller.utils.setPositiveButton
import dev.bartuzen.qbitcontroller.utils.showDialog
import dev.bartuzen.qbitcontroller.utils.showSnackbar
import dev.bartuzen.qbitcontroller.utils.toPx
import dev.bartuzen.qbitcontroller.utils.view
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@AndroidEntryPoint
class TorrentTrackersFragment() : Fragment(R.layout.fragment_torrent_trackers) {
    private val binding by viewBinding(FragmentTorrentTrackersBinding::bind)
    private val activityBinding by viewBinding(ActivityTorrentBinding::bind, viewProvider = { requireActivity().view })

    private val viewModel: TorrentTrackersViewModel by viewModels()

    private val serverConfig get() = arguments?.getParcelableCompat<ServerConfig>("serverConfig")!!
    private val torrentHash get() = arguments?.getString("torrentHash")!!

    constructor(serverConfig: ServerConfig, torrentHash: String) : this() {
        arguments = bundleOf(
            "serverConfig" to serverConfig,
            "torrentHash" to torrentHash
        )
    }

    private lateinit var onPageChange: ViewPager2.OnPageChangeCallback

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        requireActivity().addMenuProvider(
            object : MenuProvider {
                override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                    menuInflater.inflate(R.menu.torrent_trackers_menu, menu)
                }

                override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                    when (menuItem.itemId) {
                        R.id.menu_add -> {
                            showAddTrackersDialog()
                        }
                        else -> return false
                    }

                    return true
                }
            },
            viewLifecycleOwner,
            Lifecycle.State.RESUMED
        )

        var actionMode: ActionMode? = null
        val adapter = TorrentTrackersAdapter().apply {
            onSelectionModeStart {
                actionMode = requireActivity().startActionMode(object : ActionMode.Callback {
                    override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
                        mode.menuInflater.inflate(R.menu.torrent_trackers_selection_menu, menu)
                        return true
                    }

                    override fun onPrepareActionMode(mode: ActionMode, menu: Menu) = false

                    override fun onActionItemClicked(mode: ActionMode, item: MenuItem) = when (item.itemId) {
                        R.id.menu_delete -> {
                            val items = selectedItems
                                .filter { !it.startsWith("0") }
                                .map { it.substring(1) }
                            if (items.isNotEmpty()) {
                                showDeleteTrackersDialog(
                                    trackerKeys = items,
                                    onDelete = {
                                        finishSelection()
                                        actionMode?.finish()
                                    }
                                )
                            } else {
                                showSnackbar(R.string.torrent_trackers_cannot_delete_default)
                                finishSelection()
                                actionMode?.finish()
                            }
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
                        R.plurals.torrent_trackers_selected,
                        itemCount,
                        itemCount
                    )
                }
            }
        }
        binding.recyclerTrackers.adapter = adapter
        binding.recyclerTrackers.addItemDecoration(object : RecyclerView.ItemDecoration() {
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

        onPageChange = object : ViewPager2.OnPageChangeCallback() {
            override fun onPageScrollStateChanged(state: Int) {
                actionMode?.finish()
            }
        }
        activityBinding.viewPager.registerOnPageChangeCallback(onPageChange)

        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refreshTrackers(serverConfig, torrentHash)
        }

        if (!viewModel.isInitialLoadStarted) {
            viewModel.isInitialLoadStarted = true
            viewModel.loadTrackers(serverConfig, torrentHash)
        }

        viewModel.isLoading.launchAndCollectLatestIn(viewLifecycleOwner) { isLoading ->
            binding.progressIndicator.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.isRefreshing.launchAndCollectLatestIn(viewLifecycleOwner) { isRefreshing ->
            binding.swipeRefresh.isRefreshing = isRefreshing
        }

        viewModel.torrentTrackers.launchAndCollectLatestIn(viewLifecycleOwner) { trackers ->
            adapter.submitList(trackers)
        }

        viewModel.autoRefreshInterval.launchAndCollectLatestIn(viewLifecycleOwner, Lifecycle.State.RESUMED) { interval ->
            if (interval != 0) {
                while (isActive) {
                    delay(interval * 1000L)
                    if (isActive) {
                        viewModel.loadTrackers(serverConfig, torrentHash)
                    }
                }
            }
        }

        viewModel.eventFlow.launchAndCollectIn(viewLifecycleOwner) { event ->
            when (event) {
                is TorrentTrackersViewModel.Event.Error -> {
                    showSnackbar(getErrorMessage(requireContext(), event.error))
                }
                TorrentTrackersViewModel.Event.TorrentNotFound -> {
                    showSnackbar(R.string.torrent_error_not_found)
                }
                TorrentTrackersViewModel.Event.TrackersAdded -> {
                    showSnackbar(R.string.torrent_trackers_added)
                    viewModel.loadTrackers(serverConfig, torrentHash)
                }
                TorrentTrackersViewModel.Event.TrackersDeleted -> {
                    showSnackbar(R.string.torrent_trackers_deleted)
                    viewModel.loadTrackers(serverConfig, torrentHash)
                }
            }
        }
    }

    private fun showAddTrackersDialog() {
        showDialog(DialogTorrentTrackersAddBinding::inflate) { binding ->
            setTitle(R.string.torrent_trackers_add)
            setPositiveButton { _, _ ->
                viewModel.addTrackers(
                    serverConfig,
                    torrentHash,
                    binding.editTrackers.text.toString().split("\n")
                )
            }
            setNegativeButton()
        }
    }

    private fun showDeleteTrackersDialog(trackerKeys: List<String>, onDelete: () -> Unit) {
        showDialog {
            setTitle(
                resources.getQuantityString(
                    R.plurals.torrent_trackers_delete_title,
                    trackerKeys.size,
                    trackerKeys.size
                )
            )
            setMessage(
                resources.getQuantityString(
                    R.plurals.torrent_trackers_delete_desc,
                    trackerKeys.size,
                    trackerKeys.size
                )
            )
            setPositiveButton { _, _ ->
                viewModel.deleteTrackers(serverConfig, torrentHash, trackerKeys)
                onDelete()
            }
            setNegativeButton()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        activityBinding.viewPager.unregisterOnPageChangeCallback(onPageChange)
    }
}
