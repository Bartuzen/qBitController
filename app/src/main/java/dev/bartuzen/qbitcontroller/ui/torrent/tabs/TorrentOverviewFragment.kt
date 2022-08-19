package dev.bartuzen.qbitcontroller.ui.torrent.tabs

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import dev.bartuzen.qbitcontroller.R
import dev.bartuzen.qbitcontroller.databinding.FragmentTorrentOverviewBinding
import dev.bartuzen.qbitcontroller.ui.torrent.TorrentViewModel
import dev.bartuzen.qbitcontroller.utils.*
import kotlinx.coroutines.flow.filterNotNull

@AndroidEntryPoint
class TorrentOverviewFragment : Fragment(R.layout.fragment_torrent_overview) {
    private var _binding: FragmentTorrentOverviewBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TorrentViewModel by viewModels(
        ownerProducer = { requireActivity() }
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentTorrentOverviewBinding.bind(view)

        binding.swipeRefresh.setOnRefreshListener {
            viewModel.updateTorrent().invokeOnCompletion {
                binding.swipeRefresh.isRefreshing = false
            }
        }

        if (savedInstanceState == null) {
            if (viewModel.torrent.value == null) {
                viewModel.updateTorrent().invokeOnCompletion {
                    viewModel.isTorrentLoading.value = false
                }
            } else {
                viewModel.isTorrentLoading.value = false
            }
        }

        viewModel.isTorrentLoading.launchAndCollectLatestIn(viewLifecycleOwner) {isLoading ->
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

        viewModel.torrentOverviewEvent.launchAndCollectIn(viewLifecycleOwner) { event ->
            when (event) {
                is TorrentViewModel.TorrentOverviewEvent.OnError -> {
                    showSnackbar(requireContext().getErrorMessage(event.error))
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}