package dev.bartuzen.qbitcontroller.ui.torrent.tabs.peers

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
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import by.kirich1409.viewbindingdelegate.viewBinding
import dagger.hilt.android.AndroidEntryPoint
import dev.bartuzen.qbitcontroller.R
import dev.bartuzen.qbitcontroller.databinding.ActivityTorrentBinding
import dev.bartuzen.qbitcontroller.databinding.DialogTorrentPeersAddBinding
import dev.bartuzen.qbitcontroller.databinding.FragmentTorrentPeersBinding
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
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

@AndroidEntryPoint
class TorrentPeersFragment() : Fragment(R.layout.fragment_torrent_peers) {
    private val binding by viewBinding(FragmentTorrentPeersBinding::bind)
    private val activityBinding by viewBinding(ActivityTorrentBinding::bind, viewProvider = { requireActivity().view })

    private val viewModel: TorrentPeersViewModel by viewModels()

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
                    menuInflater.inflate(R.menu.torrent_peers_menu, menu)
                }

                override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                    when (menuItem.itemId) {
                        R.id.menu_add -> {
                            showAddPeersDialog()
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
        val adapter = TorrentPeersAdapter().apply {
            onSelectionModeStart {
                actionMode = requireActivity().startActionMode(object : ActionMode.Callback {
                    override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
                        mode.menuInflater.inflate(R.menu.torrent_peers_selection_menu, menu)
                        return true
                    }

                    override fun onPrepareActionMode(mode: ActionMode, menu: Menu) = false

                    override fun onActionItemClicked(mode: ActionMode, item: MenuItem) = when (item.itemId) {
                        R.id.menu_ban_peers -> {
                            showBanPeersDialog(
                                peers = selectedItems,
                                onBan = {
                                    finishSelection()
                                    actionMode?.finish()
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
                        R.plurals.torrent_peers_selected,
                        itemCount,
                        itemCount
                    )
                }
            }
        }
        binding.recyclerPeers.adapter = adapter
        binding.recyclerPeers.addItemDecoration(object : RecyclerView.ItemDecoration() {
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
            viewModel.refreshPeers(serverConfig, torrentHash)
        }

        if (!viewModel.isInitialLoadStarted) {
            viewModel.isInitialLoadStarted = true
            viewModel.loadPeers(serverConfig, torrentHash)
        }

        viewModel.isLoading.launchAndCollectLatestIn(viewLifecycleOwner) { isLoading ->
            binding.progressIndicator.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.isRefreshing.launchAndCollectLatestIn(viewLifecycleOwner) { isRefreshing ->
            binding.swipeRefresh.isRefreshing = isRefreshing
        }

        viewModel.torrentPeers.filterNotNull().launchAndCollectLatestIn(viewLifecycleOwner) { peers ->
            adapter.submitList(peers)
        }

        viewModel.eventFlow.launchAndCollectIn(viewLifecycleOwner) { event ->
            when (event) {
                is TorrentPeersViewModel.Event.Error -> {
                    showSnackbar(getErrorMessage(requireContext(), event.error))
                }
                TorrentPeersViewModel.Event.PeersInvalid -> {
                    showSnackbar(R.string.torrent_peers_invalid)
                }
                TorrentPeersViewModel.Event.PeersBanned -> {
                    showSnackbar(R.string.torrent_peers_banned)
                    viewModel.loadPeers(serverConfig, torrentHash)
                }
                TorrentPeersViewModel.Event.PeersAdded -> {
                    showSnackbar(R.string.torrent_peers_added)

                    viewLifecycleOwner.lifecycleScope.launch {
                        delay(1000) // wait until qBittorrent adds the peers
                        viewModel.loadPeers(serverConfig, torrentHash)
                    }
                }
            }
        }
    }

    private fun showAddPeersDialog() {
        showDialog(DialogTorrentPeersAddBinding::inflate) { binding ->
            setTitle(R.string.torrent_peers_add_dialog_title)
            setPositiveButton { _, _ ->
                viewModel.addPeers(
                    serverConfig,
                    torrentHash,
                    binding.editPeers.text.toString().split("\n")
                )
            }
            setNegativeButton()
        }
    }

    private fun showBanPeersDialog(peers: List<String>, onBan: () -> Unit) {
        showDialog {
            setTitle(
                resources.getQuantityString(
                    R.plurals.torrent_peers_ban_title,
                    peers.size,
                    peers.size
                )
            )
            setMessage(
                resources.getQuantityString(
                    R.plurals.torrent_peers_ban_desc,
                    peers.size,
                    peers.size
                )
            )
            setPositiveButton { _, _ ->
                viewModel.banPeers(serverConfig, peers)
                onBan()
            }
            setNegativeButton()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        activityBinding.viewPager.unregisterOnPageChangeCallback(onPageChange)
    }
}
