package dev.bartuzen.qbitcontroller.ui.torrent.tabs.files

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.ConcatAdapter
import com.hannesdorfmann.fragmentargs.annotation.Arg
import com.hannesdorfmann.fragmentargs.annotation.FragmentWithArgs
import dagger.hilt.android.AndroidEntryPoint
import dev.bartuzen.qbitcontroller.R
import dev.bartuzen.qbitcontroller.databinding.FragmentTorrentFilesBinding
import dev.bartuzen.qbitcontroller.model.ServerConfig
import dev.bartuzen.qbitcontroller.model.TorrentFileNode
import dev.bartuzen.qbitcontroller.ui.base.ArgsFragment
import dev.bartuzen.qbitcontroller.utils.getErrorMessage
import dev.bartuzen.qbitcontroller.utils.launchAndCollectIn
import dev.bartuzen.qbitcontroller.utils.launchAndCollectLatestIn
import dev.bartuzen.qbitcontroller.utils.showSnackbar
import kotlinx.coroutines.flow.combine

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

        val adapter = TorrentFilesAdapter(object : TorrentFilesAdapter.OnItemClickListener {
            override fun onClick(file: TorrentFileNode) {
                if (file.isFolder) {
                    viewModel.goToFolder(file.name)
                }
            }
        })
        val backButtonAdapter = TorrentFilesBackButtonAdapter(
            object : TorrentFilesBackButtonAdapter.OnItemClickListener {
                override fun onClick() {
                    viewModel.goBack()
                }
            }
        )
        binding.recyclerFiles.adapter = ConcatAdapter(backButtonAdapter, adapter)

        binding.swipeRefresh.setOnRefreshListener {
            viewModel.isRefreshing.value = true
            viewModel.updateFiles(serverConfig, torrentHash).invokeOnCompletion {
                viewModel.isRefreshing.value = false
            }
        }

        if (!viewModel.isInitialLoadStarted) {
            viewModel.isInitialLoadStarted = true
            viewModel.updateFiles(serverConfig, torrentHash).invokeOnCompletion {
                viewModel.isLoading.value = false
            }
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
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}