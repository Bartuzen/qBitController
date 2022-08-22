package dev.bartuzen.qbitcontroller.ui.torrent.tabs.files

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import com.hannesdorfmann.fragmentargs.annotation.Arg
import com.hannesdorfmann.fragmentargs.annotation.FragmentWithArgs
import dagger.hilt.android.AndroidEntryPoint
import dev.bartuzen.qbitcontroller.R
import dev.bartuzen.qbitcontroller.databinding.FragmentTorrentFilesBinding
import dev.bartuzen.qbitcontroller.model.ServerConfig
import dev.bartuzen.qbitcontroller.model.TorrentFile
import dev.bartuzen.qbitcontroller.ui.base.ArgsFragment
import dev.bartuzen.qbitcontroller.utils.*
import kotlinx.coroutines.flow.filterNotNull

@FragmentWithArgs
@AndroidEntryPoint
class TorrentFilesFragment : ArgsFragment(R.layout.fragment_torrent_files) {
    private var _binding: FragmentTorrentFilesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TorrentFilesViewModel by viewModels()

    @Arg
    lateinit var serverConfig: ServerConfig

    @Arg
    lateinit var torrentHash: String

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentTorrentFilesBinding.bind(view)

        val adapter =
            TorrentFilesAdapter(object : TorrentFilesAdapter.OnItemClickListener {
                override fun onClick(file: TorrentFile) {

                }
            })
        binding.recyclerFiles.adapter = adapter
        binding.recyclerFiles.setItemMargin(16, 16)

        binding.swipeRefresh.setOnRefreshListener {
            viewModel.updateFiles(serverConfig, torrentHash).invokeOnCompletion {
                binding.swipeRefresh.isRefreshing = false
            }
        }

        if (savedInstanceState == null) {
            viewModel.updateFiles(serverConfig, torrentHash).invokeOnCompletion {
                viewModel.isLoading.value = false
            }
        }

        viewModel.isLoading.launchAndCollectLatestIn(viewLifecycleOwner) { isLoading ->
            binding.progressIndicator.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.torrentFiles.filterNotNull()
            .launchAndCollectLatestIn(viewLifecycleOwner) { files ->
                adapter.submitList(files)
            }

        viewModel.eventFlow.launchAndCollectIn(viewLifecycleOwner) { event ->
            when (event) {
                is TorrentFilesViewModel.Event.OnError -> {
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