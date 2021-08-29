package dev.bartuzen.qbitcontroller.ui.torrent.tabs.filelist

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import dev.bartuzen.qbitcontroller.R
import dev.bartuzen.qbitcontroller.databinding.FragmentTorrentFileListBinding
import dev.bartuzen.qbitcontroller.model.TorrentFile
import dev.bartuzen.qbitcontroller.network.RequestResult
import dev.bartuzen.qbitcontroller.ui.torrent.TorrentViewModel
import dev.bartuzen.qbitcontroller.utils.getErrorMessage
import dev.bartuzen.qbitcontroller.utils.setItemMargin
import dev.bartuzen.qbitcontroller.utils.showSnackbar
import kotlinx.coroutines.flow.collect

@AndroidEntryPoint
class TorrentFileListFragment : Fragment(R.layout.fragment_torrent_file_list) {
    private var _binding: FragmentTorrentFileListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TorrentViewModel by viewModels(
        ownerProducer = { requireActivity() }
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentTorrentFileListBinding.bind(view)

        val adapter =
            TorrentFileListAdapter(object : TorrentFileListAdapter.OnItemClickListener {
                override fun onClick(file: TorrentFile) {

                }
            })
        binding.recyclerFileList.adapter = adapter
        binding.recyclerFileList.setItemMargin(16, 16)

        binding.swipeRefresh.setOnRefreshListener {
            viewModel.updateFileList().invokeOnCompletion {
                binding.swipeRefresh.isRefreshing = false
            }
        }

        if (viewModel.fileList.isSet) {
            binding.progressIndicator.visibility = View.GONE
        } else {
            viewModel.updateFileList()
        }

        viewModel.fileList.observe(viewLifecycleOwner) { fileList ->
            adapter.submitList(fileList)
        }

        lifecycleScope.launchWhenStarted {
            viewModel.torrentFileListEvent.collect { event ->
                when (event) {
                    is TorrentViewModel.TorrentFileListEvent.OnRequestComplete -> {
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