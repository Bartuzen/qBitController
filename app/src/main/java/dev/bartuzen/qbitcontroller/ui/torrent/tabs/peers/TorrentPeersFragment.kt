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
import coil.load
import dagger.hilt.android.AndroidEntryPoint
import dev.bartuzen.qbitcontroller.R
import dev.bartuzen.qbitcontroller.databinding.ActivityTorrentBinding
import dev.bartuzen.qbitcontroller.databinding.DialogTorrentPeerDetailsBinding
import dev.bartuzen.qbitcontroller.databinding.DialogTorrentPeersAddBinding
import dev.bartuzen.qbitcontroller.databinding.FragmentTorrentPeersBinding
import dev.bartuzen.qbitcontroller.model.PeerFlag
import dev.bartuzen.qbitcontroller.model.TorrentPeer
import dev.bartuzen.qbitcontroller.utils.floorToDecimal
import dev.bartuzen.qbitcontroller.utils.formatBytes
import dev.bartuzen.qbitcontroller.utils.formatBytesPerSecond
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
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale

@AndroidEntryPoint
class TorrentPeersFragment() : Fragment(R.layout.fragment_torrent_peers) {
    private val binding by viewBinding(FragmentTorrentPeersBinding::bind)
    private val activityBinding by viewBinding(ActivityTorrentBinding::bind, viewProvider = { requireActivity().view })

    private val viewModel: TorrentPeersViewModel by viewModels()

    private val serverId get() = arguments?.getInt("serverId", -1).takeIf { it != -1 }!!
    private val torrentHash get() = arguments?.getString("torrentHash")!!

    constructor(serverId: Int, torrentHash: String) : this() {
        arguments = bundleOf(
            "serverId" to serverId,
            "torrentHash" to torrentHash
        )
    }

    private lateinit var onPageChange: ViewPager2.OnPageChangeCallback

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        requireActivity().addMenuProvider(
            object : MenuProvider {
                override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                    menuInflater.inflate(R.menu.torrent_peers, menu)
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
        val adapter = TorrentPeersAdapter(
            loadImage = { imageView, countryCode ->
                imageView.load(
                    data = viewModel.getFlagUrl(serverId, countryCode),
                    imageLoader = viewModel.getImageLoader(serverId)
                )
            }
        ).apply {
            onClick { peer ->
                showPeerDetailsDialog(peer)
            }
            onSelectionModeStart {
                actionMode = requireActivity().startActionMode(object : ActionMode.Callback {
                    override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
                        mode.menuInflater.inflate(R.menu.torrent_peers_selection, menu)
                        return true
                    }

                    override fun onPrepareActionMode(mode: ActionMode, menu: Menu) = false

                    override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
                        when (item.itemId) {
                            R.id.menu_ban_peers -> {
                                showBanPeersDialog(
                                    peers = selectedItems,
                                    onBan = {
                                        finishSelection()
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
                }
                )
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
            viewModel.refreshPeers(serverId, torrentHash)
        }

        if (!viewModel.isInitialLoadStarted) {
            viewModel.isInitialLoadStarted = true
            viewModel.loadPeers(serverId, torrentHash)
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

        viewModel.torrentPeers.filterNotNull().launchAndCollectLatestIn(viewLifecycleOwner) { peers ->
            adapter.submitList(peers)
        }

        viewModel.autoRefreshInterval.launchAndCollectLatestIn(viewLifecycleOwner, Lifecycle.State.RESUMED) { interval ->
            if (interval != 0) {
                while (isActive) {
                    delay(interval * 1000L)
                    if (isActive && actionMode == null) {
                        viewModel.loadPeers(serverId, torrentHash, autoRefresh = true)
                    }
                }
            }
        }

        viewModel.eventFlow.launchAndCollectIn(viewLifecycleOwner) { event ->
            when (event) {
                is TorrentPeersViewModel.Event.Error -> {
                    showSnackbar(getErrorMessage(requireContext(), event.error), view = requireActivity().view)
                }
                TorrentPeersViewModel.Event.TorrentNotFound -> {
                    showSnackbar(R.string.torrent_error_not_found, view = requireActivity().view)
                }
                TorrentPeersViewModel.Event.PeersInvalid -> {
                    showSnackbar(R.string.torrent_peers_invalid, view = requireActivity().view)
                }
                TorrentPeersViewModel.Event.PeersBanned -> {
                    showSnackbar(R.string.torrent_peers_banned, view = requireActivity().view)
                    viewModel.loadPeers(serverId, torrentHash)
                }
                TorrentPeersViewModel.Event.PeersAdded -> {
                    showSnackbar(R.string.torrent_peers_added, view = requireActivity().view)

                    viewLifecycleOwner.lifecycleScope.launch {
                        delay(1000) // wait until qBittorrent adds the peers
                        viewModel.loadPeers(serverId, torrentHash)
                    }
                }
            }
        }
    }

    private fun showPeerDetailsDialog(peer: TorrentPeer) {
        showDialog(DialogTorrentPeerDetailsBinding::inflate) { binding ->
            setTitle(getString(R.string.torrent_peers_ip_format, peer.ip, peer.port))
            setPositiveButton()

            val progress = peer.progress.let { progress ->
                if (progress < 1) {
                    (progress * 100).floorToDecimal(1).toString()
                } else {
                    "100"
                }
            }

            val relevance = peer.relevance.let { relevance ->
                if (relevance < 1) {
                    (relevance * 100).floorToDecimal(1).toString()
                } else {
                    "100"
                }
            }

            val downloadSpeed = formatBytesPerSecond(requireContext(), peer.downloadSpeed.toLong())
            val uploadSpeed = formatBytesPerSecond(requireContext(), peer.uploadSpeed.toLong())
            val downloaded = formatBytes(requireContext(), peer.downloaded)
            val uploaded = formatBytes(requireContext(), peer.uploaded)

            val flagsText = if (peer.flags.isNotEmpty()) "" else "-"
            val filesText = if (peer.files.isNotEmpty()) "" else "-"

            if (peer.countryCode != null) {
                val countryName = Locale("", peer.countryCode).getDisplayCountry(
                    Locale(context.getString(R.string.language_code))
                )
                binding.textCountry.text = getString(R.string.torrent_peers_details_country, countryName)
                binding.textCountry.visibility = View.VISIBLE
            } else {
                binding.textCountry.visibility = View.GONE
            }

            binding.textConnection.text = getString(R.string.torrent_peers_details_connection, peer.connection)
            binding.textFlags.text = getString(R.string.torrent_peers_details_flags, flagsText)
            binding.textClient.text = getString(R.string.torrent_peers_details_client, peer.client ?: "-")
            binding.textPeerIdClient.text =
                getString(R.string.torrent_peers_details_peer_id_client, peer.peerIdClient ?: "-")
            binding.textProgress.text = getString(R.string.torrent_peers_details_progress, progress)
            binding.textDownloadSpeed.text = getString(R.string.torrent_peers_details_download_speed, downloadSpeed)
            binding.textUploadSpeed.text = getString(R.string.torrent_peers_details_upload_speed, uploadSpeed)
            binding.textDownloaded.text = getString(R.string.torrent_peers_details_downloaded, downloaded)
            binding.textUploaded.text = getString(R.string.torrent_peers_details_uploaded, uploaded)
            binding.textRelevance.text = getString(R.string.torrent_peers_details_relevance, relevance)
            binding.textFiles.text = getString(R.string.torrent_peers_details_files, filesText)

            if (peer.flags.isNotEmpty()) {
                binding.textFlagsDesc.visibility = View.VISIBLE
                binding.textFlagsDesc.text = peer.flags.joinToString("\n") { flag ->
                    val resId = when (flag) {
                        PeerFlag.INTERESTED_LOCAL_CHOKED_PEER -> R.string.torrent_peers_flag_interested_local_choked_peer
                        PeerFlag.INTERESTED_LOCAL_UNCHOKED_PEER -> R.string.torrent_peers_flag_interested_local_unchoked_peer
                        PeerFlag.INTERESTED_PEER_CHOKED_LOCAL -> R.string.torrent_peers_flag_interested_peer_choked_local
                        PeerFlag.INTERESTED_PEER_UNCHOKED_LOCAL -> R.string.torrent_peers_flag_interested_peer_unchoked_local
                        PeerFlag.NOT_INTERESTED_LOCAL_UNCHOKED_PEER ->
                            R.string.torrent_peers_flag_not_interested_local_unchoked_peer
                        PeerFlag.NOT_INTERESTED_PEER_UNCHOKED_LOCAL ->
                            R.string.torrent_peers_flag_not_interested_peer_unchoked_local
                        PeerFlag.OPTIMISTIC_UNCHOKE -> R.string.torrent_peers_flag_optimistic_unchoke
                        PeerFlag.PEER_SNUBBED -> R.string.torrent_peers_flag_peer_snubbed
                        PeerFlag.INCOMING_CONNECTION -> R.string.torrent_peers_flag_incoming_connection
                        PeerFlag.PEER_FROM_DHT -> R.string.torrent_peers_flag_peer_from_dht
                        PeerFlag.PEER_FROM_PEX -> R.string.torrent_peers_flag_peer_from_pex
                        PeerFlag.PEER_FROM_LSD -> R.string.torrent_peers_flag_peer_from_lsd
                        PeerFlag.ENCRYPTED_TRAFFIC -> R.string.torrent_peers_flag_encrypted_traffic
                        PeerFlag.ENCRYPTED_HANDSHAKE -> R.string.torrent_peers_flag_encrypted_handshake
                        PeerFlag.UTP -> R.string.torrent_peers_flag_utp
                    }
                    getString(R.string.torrent_peers_flag_format, flag.flag, getString(resId))
                }
            }

            if (peer.files.isNotEmpty()) {
                binding.textFilesDesc.visibility = View.VISIBLE
                binding.textFilesDesc.text = peer.files.joinToString("\n")
            }
        }
    }

    private fun showAddPeersDialog() {
        showDialog(DialogTorrentPeersAddBinding::inflate) { binding ->
            setTitle(R.string.torrent_peers_action_add)
            setPositiveButton { _, _ ->
                viewModel.addPeers(
                    serverId,
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
                viewModel.banPeers(serverId, peers)
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
