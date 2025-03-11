package dev.bartuzen.qbitcontroller.ui.torrent.tabs.trackers

import android.graphics.Rect
import android.os.Bundle
import android.view.ActionMode
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AlertDialog
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
import dev.bartuzen.qbitcontroller.databinding.DialogEditTrackerBinding
import dev.bartuzen.qbitcontroller.databinding.DialogTorrentTrackersAddBinding
import dev.bartuzen.qbitcontroller.databinding.FragmentTorrentTrackersBinding
import dev.bartuzen.qbitcontroller.utils.applySafeDrawingInsets
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
import kotlinx.coroutines.isActive

@AndroidEntryPoint
class TorrentTrackersFragment() : Fragment(R.layout.fragment_torrent_trackers) {
    private val binding by viewBinding(FragmentTorrentTrackersBinding::bind)
    private val activityBinding by viewBinding(ActivityTorrentBinding::bind, viewProvider = { requireActivity().view })

    private val viewModel: TorrentTrackersViewModel by viewModels()

    private val serverId get() = arguments?.getInt("serverId", -1).takeIf { it != -1 }!!
    private val torrentHash get() = arguments?.getString("torrentHash")!!

    constructor(serverId: Int, torrentHash: String) : this() {
        arguments = bundleOf(
            "serverId" to serverId,
            "torrentHash" to torrentHash,
        )
    }

    private lateinit var onPageChange: ViewPager2.OnPageChangeCallback

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.progressIndicator.applySafeDrawingInsets(top = false, bottom = false)
        binding.recyclerTrackers.applySafeDrawingInsets(top = false)

        requireActivity().addMenuProvider(
            object : MenuProvider {
                override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                    menuInflater.inflate(R.menu.torrent_trackers, menu)
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
            Lifecycle.State.RESUMED,
        )

        var actionMode: ActionMode? = null
        val adapter = TorrentTrackersAdapter().apply {
            onSelectionModeStart {
                actionMode = requireActivity().startActionMode(object : ActionMode.Callback {
                    override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
                        mode.menuInflater.inflate(R.menu.torrent_trackers_selection, menu)
                        return true
                    }

                    override fun onPrepareActionMode(mode: ActionMode, menu: Menu) = false

                    override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
                        when (item.itemId) {
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
                                        },
                                    )
                                } else {
                                    showSnackbar(
                                        R.string.torrent_trackers_cannot_delete_default,
                                        view = requireActivity().view,
                                    )
                                    finishSelection()
                                    actionMode?.finish()
                                }
                            }
                            R.id.menu_edit -> {
                                val selectedItem = selectedItems.firstOrNull() ?: return true
                                if (selectedItem.first() == '1') {
                                    val tracker = selectedItem.substring(1)
                                    showEditTrackerDialog(
                                        tracker = tracker,
                                        onSuccess = { newUrl ->
                                            viewModel.editTracker(serverId, torrentHash, tracker, newUrl)
                                        },
                                    )
                                } else {
                                    showSnackbar(
                                        R.string.torrent_trackers_cannot_edit_default,
                                        view = requireActivity().view,
                                    )
                                    finishSelection()
                                    actionMode?.finish()
                                }
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
                        R.plurals.torrent_trackers_selected,
                        itemCount,
                        itemCount,
                    )
                }
                actionMode?.menu?.findItem(R.id.menu_edit)?.isEnabled = itemCount == 1
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
            viewModel.refreshTrackers(serverId, torrentHash)
        }

        if (!viewModel.isInitialLoadStarted) {
            viewModel.isInitialLoadStarted = true
            viewModel.loadTrackers(serverId, torrentHash)
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

        viewModel.torrentTrackers.launchAndCollectLatestIn(viewLifecycleOwner) { trackers ->
            adapter.submitList(trackers)
        }

        viewModel.autoRefreshInterval.launchAndCollectLatestIn(viewLifecycleOwner, Lifecycle.State.RESUMED) { interval ->
            if (interval != 0) {
                while (isActive) {
                    delay(interval * 1000L)
                    if (isActive && actionMode == null) {
                        viewModel.loadTrackers(serverId, torrentHash, autoRefresh = true)
                    }
                }
            }
        }

        viewModel.eventFlow.launchAndCollectIn(viewLifecycleOwner) { event ->
            when (event) {
                is TorrentTrackersViewModel.Event.Error -> {
                    showSnackbar(getErrorMessage(requireContext(), event.error), view = requireActivity().view)
                }
                TorrentTrackersViewModel.Event.TorrentNotFound -> {
                    showSnackbar(R.string.torrent_error_not_found, view = requireActivity().view)
                }
                TorrentTrackersViewModel.Event.TrackersAdded -> {
                    showSnackbar(R.string.torrent_trackers_added, view = requireActivity().view)
                    viewModel.loadTrackers(serverId, torrentHash)
                }
                TorrentTrackersViewModel.Event.TrackersDeleted -> {
                    showSnackbar(R.string.torrent_trackers_deleted, view = requireActivity().view)
                    viewModel.loadTrackers(serverId, torrentHash)
                }
                TorrentTrackersViewModel.Event.TrackerEdited -> {
                    showSnackbar(R.string.torrent_trackers_edited, view = requireActivity().view)
                    viewModel.loadTrackers(serverId, torrentHash)
                }
            }
        }
    }

    private fun showAddTrackersDialog() {
        showDialog(DialogTorrentTrackersAddBinding::inflate) { binding ->
            setTitle(R.string.torrent_trackers_action_add)
            setPositiveButton { _, _ ->
                viewModel.addTrackers(
                    serverId,
                    torrentHash,
                    binding.editTrackers.text.toString().split("\n"),
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
                    trackerKeys.size,
                ),
            )
            setMessage(
                resources.getQuantityString(
                    R.plurals.torrent_trackers_delete_desc,
                    trackerKeys.size,
                    trackerKeys.size,
                ),
            )
            setPositiveButton { _, _ ->
                viewModel.deleteTrackers(serverId, torrentHash, trackerKeys)
                onDelete()
            }
            setNegativeButton()
        }
    }

    private fun showEditTrackerDialog(tracker: String, onSuccess: (newUrl: String) -> Unit) {
        lateinit var dialogBinding: DialogEditTrackerBinding

        val dialog = showDialog(DialogEditTrackerBinding::inflate) { binding ->
            dialogBinding = binding

            binding.inputLayoutTrackerUrl.setTextWithoutAnimation(tracker)

            setTitle(R.string.torrent_trackers_action_edit)
            setPositiveButton()
            setNegativeButton()
        }

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val newUrl = dialogBinding.editTracker.text.toString()
            if (newUrl.isNotBlank()) {
                onSuccess(newUrl)
                dialog.dismiss()
            } else {
                dialogBinding.inputLayoutTrackerUrl.error = getString(R.string.torrent_trackers_url_cannot_be_empty)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        activityBinding.viewPager.unregisterOnPageChangeCallback(onPageChange)
    }
}
