package dev.bartuzen.qbitcontroller.ui.torrent.tabs.overview

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.core.view.MenuProvider
import androidx.fragment.app.viewModels
import com.hannesdorfmann.fragmentargs.annotation.Arg
import com.hannesdorfmann.fragmentargs.annotation.FragmentWithArgs
import dagger.hilt.android.AndroidEntryPoint
import dev.bartuzen.qbitcontroller.R
import dev.bartuzen.qbitcontroller.databinding.FragmentTorrentOverviewBinding
import dev.bartuzen.qbitcontroller.model.ServerConfig
import dev.bartuzen.qbitcontroller.model.TorrentState
import dev.bartuzen.qbitcontroller.ui.base.ArgsFragment
import dev.bartuzen.qbitcontroller.utils.*
import kotlinx.coroutines.flow.filterNotNull

@FragmentWithArgs
@AndroidEntryPoint
class TorrentOverviewFragment : ArgsFragment(R.layout.fragment_torrent_overview) {
    private var _binding: FragmentTorrentOverviewBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TorrentOverviewViewModel by viewModels()

    @Arg
    lateinit var serverConfig: ServerConfig

    @Arg
    lateinit var torrentHash: String

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentTorrentOverviewBinding.bind(view)

        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.torrent_menu, menu)

                viewModel.torrent.filterNotNull()
                    .launchAndCollectLatestIn(this@TorrentOverviewFragment) { torrent ->
                        if (torrent.state == TorrentState.PAUSED_DL ||
                            torrent.state == TorrentState.PAUSED_UP
                        ) {
                            menu.findItem(R.id.menu_resume).isVisible = true
                            menu.findItem(R.id.menu_pause).isVisible = false
                        } else {
                            menu.findItem(R.id.menu_pause).isVisible = true
                            menu.findItem(R.id.menu_resume).isVisible = false
                        }
                    }
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                when (menuItem.itemId) {
                    R.id.menu_pause -> viewModel.pauseTorrent(serverConfig, torrentHash)
                    R.id.menu_resume -> viewModel.resumeTorrent(serverConfig, torrentHash)
                    else -> return false
                }
                return true
            }
        }, viewLifecycleOwner)

        binding.swipeRefresh.setOnRefreshListener {
            viewModel.updateTorrent(serverConfig, torrentHash).invokeOnCompletion {
                binding.swipeRefresh.isRefreshing = false
            }
        }

        if (savedInstanceState == null) {
            viewModel.updateTorrent(serverConfig, torrentHash).invokeOnCompletion {
                viewModel.isLoading.value = false
            }
        }

        viewModel.isLoading.launchAndCollectLatestIn(viewLifecycleOwner) { isLoading ->
            binding.progressIndicator.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.torrent.filterNotNull().launchAndCollectLatestIn(viewLifecycleOwner) { torrent ->
            binding.textName.text = torrent.name

            val progress = torrent.progress * 100
            binding.progressTorrent.progress = progress.toInt()

            val progressText = if (torrent.progress < 1) {
                progress.floorToDecimal(1)
            } else {
                "100"
            }
            binding.textProgress.text = requireContext().getString(
                R.string.torrent_item_progress,
                requireContext().formatByte(torrent.completed),
                requireContext().formatByte(torrent.size),
                progressText
            )

            val eta = requireContext().formatTime(torrent.eta)
            if (eta != "inf") {
                binding.textEta.text = eta
            }
            binding.textState.text = requireContext().formatState(torrent.state)


            val speedList = mutableListOf<String>()
            if (torrent.uploadSpeed > 0) {
                speedList.add("↑ ${requireContext().formatBytePerSecond(torrent.uploadSpeed)}")
            }
            if (torrent.downloadSpeed > 0) {
                speedList.add("↓ ${requireContext().formatBytePerSecond(torrent.downloadSpeed)}")
            }
            binding.textSpeed.text = speedList.joinToString(" ")
        }

        viewModel.eventFlow.launchAndCollectIn(viewLifecycleOwner) { event ->
            when (event) {
                is TorrentOverviewViewModel.Event.Error -> {
                    showSnackbar(requireContext().getErrorMessage(event.error))
                }
                TorrentOverviewViewModel.Event.OnTorrentPause -> {
                    showSnackbar(getString(R.string.torrent_paused_success))
                }
                TorrentOverviewViewModel.Event.OnTorrentResume -> {
                    showSnackbar(getString(R.string.torrent_resumed_success))
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}