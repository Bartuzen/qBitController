package dev.bartuzen.qbitcontroller.ui.torrent.tabs.files

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.ConcatAdapter
import by.kirich1409.viewbindingdelegate.viewBinding
import dagger.hilt.android.AndroidEntryPoint
import dev.bartuzen.qbitcontroller.R
import dev.bartuzen.qbitcontroller.databinding.FragmentTorrentFilesBinding
import dev.bartuzen.qbitcontroller.model.ServerConfig
import dev.bartuzen.qbitcontroller.utils.getErrorMessage
import dev.bartuzen.qbitcontroller.utils.getParcelableCompat
import dev.bartuzen.qbitcontroller.utils.launchAndCollectIn
import dev.bartuzen.qbitcontroller.utils.launchAndCollectLatestIn
import dev.bartuzen.qbitcontroller.utils.showSnackbar
import kotlinx.coroutines.flow.combine

@AndroidEntryPoint
class TorrentFilesFragment() : Fragment(R.layout.fragment_torrent_files) {
    private val binding by viewBinding(FragmentTorrentFilesBinding::bind)

    private val viewModel: TorrentFilesViewModel by viewModels()

    private val serverConfig get() = arguments?.getParcelableCompat<ServerConfig>("serverConfig")!!
    private val torrentHash get() = arguments?.getString("torrentHash")!!

    constructor(serverConfig: ServerConfig, torrentHash: String) : this() {
        arguments = bundleOf(
            "serverConfig" to serverConfig,
            "torrentHash" to torrentHash
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val adapter = TorrentFilesAdapter(
            onClick = { file ->
                if (file.isFolder) {
                    viewModel.goToFolder(file.name)
                }
            }
        )
        val backButtonAdapter = TorrentFilesBackButtonAdapter(
            onClick = {
                viewModel.goBack()
            }
        )
        binding.recyclerFiles.adapter = ConcatAdapter(backButtonAdapter, adapter)

        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refreshFiles(serverConfig, torrentHash)
        }

        if (!viewModel.isInitialLoadStarted) {
            viewModel.isInitialLoadStarted = true
            viewModel.loadFiles(serverConfig, torrentHash)
        }

        viewModel.isLoading.launchAndCollectLatestIn(viewLifecycleOwner) { isLoading ->
            binding.progressIndicator.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.isRefreshing.launchAndCollectLatestIn(viewLifecycleOwner) { isRefreshing ->
            binding.swipeRefresh.isRefreshing = isRefreshing
        }

        combine(viewModel.nodeStack, viewModel.torrentFiles) { nodeStack, torrentFiles ->
            nodeStack to torrentFiles
        }.launchAndCollectLatestIn(viewLifecycleOwner) { (nodeStack, torrentFiles) ->
            if (torrentFiles != null) {
                val fileList = torrentFiles.findChildNode(nodeStack)?.children?.sortedWith(
                    compareBy({ it.isFile }, { it.name.lowercase() })
                )
                if (fileList != null) {
                    adapter.submitList(fileList) {
                        backButtonAdapter.isVisible = nodeStack.isNotEmpty()
                    }
                }
            }
        }

        viewModel.eventFlow.launchAndCollectIn(viewLifecycleOwner) { event ->
            when (event) {
                is TorrentFilesViewModel.Event.Error -> {
                    showSnackbar(getErrorMessage(requireContext(), event.error))
                }
                TorrentFilesViewModel.Event.TorrentNotFound -> {
                    showSnackbar(R.string.torrent_error_not_found)
                }
            }
        }
    }
}
