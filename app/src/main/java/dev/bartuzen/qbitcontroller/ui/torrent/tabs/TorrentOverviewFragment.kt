package dev.bartuzen.qbitcontroller.ui.torrent.tabs

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import dev.bartuzen.qbitcontroller.R
import dev.bartuzen.qbitcontroller.databinding.FragmentTorrentOverviewBinding
import dev.bartuzen.qbitcontroller.network.RequestResult
import dev.bartuzen.qbitcontroller.ui.torrent.TorrentViewModel
import dev.bartuzen.qbitcontroller.utils.*
import kotlinx.coroutines.flow.collect

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

        if (viewModel.torrent.isSet) {
            binding.progressIndicator.visibility = View.GONE
        } else {
            viewModel.updateTorrent()
        }

        viewModel.torrent.observe(viewLifecycleOwner) { torrent ->
            val context = requireContext()

            binding.textName.text = torrent.name

            val progress = torrent.progress * 100
            binding.progressTorrent.progress = progress.toInt()

            val progressText = if (torrent.progress < 1) {
                progress.floorToDecimal(1)
            } else {
                "100"
            }
            binding.textProgress.text =
                context.getString(
                    R.string.torrent_item_progress,
                    context.formatByte(torrent.downloaded),
                    context.formatByte(torrent.size),
                    progressText
                )

            val eta = context.formatTime(torrent.eta)
            if (eta != "inf") {
                binding.textEta.text = eta
            }
            binding.textState.text = context.formatState(torrent.state)


            val speedList = mutableListOf<String>()
            if (torrent.uploadSpeed > 0) {
                speedList.add("↑ ${context.formatBytePerSecond(torrent.uploadSpeed)}")
            }
            if (torrent.downloadSpeed > 0) {
                speedList.add("↓ ${context.formatBytePerSecond(torrent.downloadSpeed)}")
            }
            binding.textSpeed.text = speedList.joinToString(" ")
        }

        lifecycleScope.launchWhenStarted {
            viewModel.torrentOverviewEvent.collect { event ->
                when (event) {
                    is TorrentViewModel.TorrentOverviewEvent.OnRequestComplete -> {
                        binding.progressIndicator.visibility = View.GONE
                        if (event.result != RequestResult.SUCCESS) {
                            context?.getErrorMessage(event.result)?.let {
                                showSnackbar(it)
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}