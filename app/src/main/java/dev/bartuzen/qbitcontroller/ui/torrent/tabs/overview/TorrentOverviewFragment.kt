package dev.bartuzen.qbitcontroller.ui.torrent.tabs.overview

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.core.view.MenuProvider
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.hannesdorfmann.fragmentargs.annotation.Arg
import com.hannesdorfmann.fragmentargs.annotation.FragmentWithArgs
import dagger.hilt.android.AndroidEntryPoint
import dev.bartuzen.qbitcontroller.R
import dev.bartuzen.qbitcontroller.databinding.DialogTorrentDeleteBinding
import dev.bartuzen.qbitcontroller.databinding.FragmentTorrentOverviewBinding
import dev.bartuzen.qbitcontroller.model.ServerConfig
import dev.bartuzen.qbitcontroller.model.TorrentState
import dev.bartuzen.qbitcontroller.ui.base.ArgsFragment
import dev.bartuzen.qbitcontroller.ui.torrent.TorrentActivity
import dev.bartuzen.qbitcontroller.utils.floorToDecimal
import dev.bartuzen.qbitcontroller.utils.formatBytes
import dev.bartuzen.qbitcontroller.utils.formatBytesPerSecond
import dev.bartuzen.qbitcontroller.utils.formatSeconds
import dev.bartuzen.qbitcontroller.utils.formatTorrentState
import dev.bartuzen.qbitcontroller.utils.getErrorMessage
import dev.bartuzen.qbitcontroller.utils.launchAndCollectIn
import dev.bartuzen.qbitcontroller.utils.launchAndCollectLatestIn
import dev.bartuzen.qbitcontroller.utils.showSnackbar
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

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
                        val isPaused = when (torrent.state) {
                            TorrentState.PAUSED_DL, TorrentState.PAUSED_UP,
                            TorrentState.MISSING_FILES, TorrentState.ERROR,
                            -> true

                            else -> false
                        }
                        menu.findItem(R.id.menu_resume).isVisible = isPaused
                        menu.findItem(R.id.menu_pause).isVisible = !isPaused
                    }
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                when (menuItem.itemId) {
                    R.id.menu_pause -> {
                        viewModel.pauseTorrent(serverConfig, torrentHash)
                    }
                    R.id.menu_resume -> {
                        viewModel.resumeTorrent(serverConfig, torrentHash)
                    }
                    R.id.menu_delete -> {
                        showDeleteTorrentDialog()
                    }
                    else -> return false
                }
                return true
            }
        }, viewLifecycleOwner)

        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refreshTorrent(serverConfig, torrentHash)
        }

        if (!viewModel.isInitialLoadStarted) {
            viewModel.isInitialLoadStarted = true
            viewModel.loadTorrent(serverConfig, torrentHash)
        }

        viewModel.isLoading.launchAndCollectLatestIn(viewLifecycleOwner) { isLoading ->
            binding.progressIndicator.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.isRefreshing.launchAndCollectLatestIn(viewLifecycleOwner) { isRefreshing ->
            binding.swipeRefresh.isRefreshing = isRefreshing
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
                formatBytes(requireContext(), torrent.completed),
                formatBytes(requireContext(), torrent.size),
                progressText
            )

            val eta = formatSeconds(requireContext(), torrent.eta)
            binding.textEta.text = if (eta != "inf") eta else null

            binding.textState.text = formatTorrentState(requireContext(), torrent.state)

            val speedList = mutableListOf<String>()
            if (torrent.uploadSpeed > 0) {
                speedList.add("↑ ${formatBytesPerSecond(requireContext(), torrent.uploadSpeed)}")
            }
            if (torrent.downloadSpeed > 0) {
                speedList.add("↓ ${formatBytesPerSecond(requireContext(), torrent.downloadSpeed)}")
            }
            binding.textSpeed.text = speedList.joinToString(" ")
        }

        viewModel.eventFlow.launchAndCollectIn(viewLifecycleOwner) { event ->
            when (event) {
                is TorrentOverviewViewModel.Event.Error -> {
                    showSnackbar(getErrorMessage(requireContext(), event.error))
                }
                TorrentOverviewViewModel.Event.TorrentDeleted -> {
                    val intent = Intent().apply {
                        putExtra(TorrentActivity.Extras.TORRENT_DELETED, true)
                    }
                    requireActivity().setResult(Activity.RESULT_OK, intent)
                    requireActivity().finish()
                }
                TorrentOverviewViewModel.Event.TorrentPaused -> {
                    showSnackbar(getString(R.string.torrent_paused_success))

                    viewLifecycleOwner.lifecycleScope.launch {
                        delay(1000) // wait until qBittorrent pauses the torrent
                        viewModel.loadTorrent(serverConfig, torrentHash)
                    }
                }
                TorrentOverviewViewModel.Event.TorrentResumed -> {
                    showSnackbar(getString(R.string.torrent_resumed_success))

                    viewLifecycleOwner.lifecycleScope.launch {
                        delay(1000) // wait until qBittorrent resumes the torrent
                        viewModel.loadTorrent(serverConfig, torrentHash)
                    }
                }
            }
        }
    }

    private fun showDeleteTorrentDialog() {
        val dialogBinding = DialogTorrentDeleteBinding.inflate(layoutInflater)

        MaterialAlertDialogBuilder(requireActivity())
            .setTitle(R.string.torrent_delete)
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.dialog_ok) { _, _ ->
                viewModel.deleteTorrent(
                    serverConfig,
                    torrentHash,
                    dialogBinding.checkBoxDeleteFiles.isChecked
                )
            }
            .setNegativeButton(R.string.dialog_cancel, null)
            .create()
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
