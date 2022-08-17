package dev.bartuzen.qbitcontroller.ui.torrent.tabs.files

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import dev.bartuzen.qbitcontroller.R
import dev.bartuzen.qbitcontroller.databinding.FragmentTorrentFilesBinding
import dev.bartuzen.qbitcontroller.model.TorrentFile
import dev.bartuzen.qbitcontroller.ui.torrent.TorrentViewModel
import dev.bartuzen.qbitcontroller.utils.getErrorMessage
import dev.bartuzen.qbitcontroller.utils.setItemMargin
import dev.bartuzen.qbitcontroller.utils.showSnackbar

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

        if (viewModel.torrentFiles.isSet) {
            binding.progressIndicator.visibility = View.GONE
        } else {
            viewModel.updateFiles()
        }

        viewModel.torrentFiles.observe(viewLifecycleOwner) { files ->
            files?.let {
                binding.progressIndicator.visibility = View.GONE
                adapter.submitList(it)
            }
        }

        lifecycleScope.launchWhenStarted {
            viewModel.torrentFilesEvent.collect { event ->
                when (event) {
                    is TorrentViewModel.TorrentFilesEvent.OnError -> {
                        context?.getErrorMessage(event.error)?.let {
                            showSnackbar(it)
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