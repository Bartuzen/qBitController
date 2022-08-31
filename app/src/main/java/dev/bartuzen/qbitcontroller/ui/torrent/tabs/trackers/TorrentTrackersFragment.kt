package dev.bartuzen.qbitcontroller.ui.torrent.tabs.trackers

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import com.hannesdorfmann.fragmentargs.annotation.Arg
import com.hannesdorfmann.fragmentargs.annotation.FragmentWithArgs
import dagger.hilt.android.AndroidEntryPoint
import dev.bartuzen.qbitcontroller.R
import dev.bartuzen.qbitcontroller.databinding.FragmentTorrentTrackersBinding
import dev.bartuzen.qbitcontroller.model.ServerConfig
import dev.bartuzen.qbitcontroller.ui.base.ArgsFragment
import dev.bartuzen.qbitcontroller.utils.*

@FragmentWithArgs
@AndroidEntryPoint
class TorrentTrackersFragment : ArgsFragment(R.layout.fragment_torrent_trackers) {
    private var _binding: FragmentTorrentTrackersBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TorrentTrackersViewModel by viewModels()

    @Arg
    lateinit var serverConfig: ServerConfig

    @Arg
    lateinit var torrentHash: String

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentTorrentTrackersBinding.bind(view)

        val adapter = TorrentTrackersAdapter()
        binding.recyclerTrackers.adapter = adapter
        binding.recyclerTrackers.setItemMargin(8, 8)

        binding.swipeRefresh.setOnRefreshListener {
            viewModel.updateTrackers(serverConfig, torrentHash).invokeOnCompletion {
                binding.swipeRefresh.isRefreshing = false
            }
        }

        if (savedInstanceState == null) {
            viewModel.updateTrackers(serverConfig, torrentHash).invokeOnCompletion {
                viewModel.isLoading.value = false
            }
        }

        viewModel.isLoading.launchAndCollectLatestIn(viewLifecycleOwner) { isLoading ->
            binding.progressIndicator.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.torrentTrackers.launchAndCollectLatestIn(viewLifecycleOwner) { trackers ->
            adapter.submitList(trackers)
        }

        viewModel.eventFlow.launchAndCollectIn(viewLifecycleOwner) { event ->
            when (event) {
                is TorrentTrackersViewModel.Event.OnError -> {
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