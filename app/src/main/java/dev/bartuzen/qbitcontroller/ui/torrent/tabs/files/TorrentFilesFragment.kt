package dev.bartuzen.qbitcontroller.ui.torrent.tabs.files

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import dev.bartuzen.qbitcontroller.R
import dev.bartuzen.qbitcontroller.databinding.FragmentTorrentFilesBinding
import dev.bartuzen.qbitcontroller.model.TorrentFile
import dev.bartuzen.qbitcontroller.ui.torrent.TorrentViewModel
import dev.bartuzen.qbitcontroller.utils.*
import kotlinx.coroutines.flow.filterNotNull

@AndroidEntryPoint
class TorrentFilesFragment : Fragment(R.layout.fragment_torrent_files) {
    private var _binding: FragmentTorrentFilesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TorrentViewModel by viewModels(
        ownerProducer = { requireActivity() }
    )

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
            viewModel.updateFiles().invokeOnCompletion {
                binding.swipeRefresh.isRefreshing = false
            }
        }

        if (savedInstanceState == null) {
            viewModel.updateFiles().invokeOnCompletion {
                viewModel.isTorrentFilesLoading.value = false
            }
        }

        viewModel.isTorrentFilesLoading.launchAndCollectLatestIn(viewLifecycleOwner) { isLoading ->
            binding.progressIndicator.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.torrentFiles.filterNotNull()
            .launchAndCollectLatestIn(viewLifecycleOwner) { files ->
                adapter.submitList(files)
            }

        viewModel.torrentFilesEvent.launchAndCollectIn(viewLifecycleOwner) { event ->
            when (event) {
                is TorrentViewModel.TorrentFilesEvent.OnError -> {
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