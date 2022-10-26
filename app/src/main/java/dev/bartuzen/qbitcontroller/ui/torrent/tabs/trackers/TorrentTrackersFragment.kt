package dev.bartuzen.qbitcontroller.ui.torrent.tabs.trackers

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.core.view.MenuProvider
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import com.hannesdorfmann.fragmentargs.annotation.Arg
import com.hannesdorfmann.fragmentargs.annotation.FragmentWithArgs
import dagger.hilt.android.AndroidEntryPoint
import dev.bartuzen.qbitcontroller.R
import dev.bartuzen.qbitcontroller.databinding.FragmentTorrentTrackersBinding
import dev.bartuzen.qbitcontroller.model.ServerConfig
import dev.bartuzen.qbitcontroller.ui.base.ArgsFragment
import dev.bartuzen.qbitcontroller.utils.getErrorMessage
import dev.bartuzen.qbitcontroller.utils.launchAndCollectIn
import dev.bartuzen.qbitcontroller.utils.launchAndCollectLatestIn
import dev.bartuzen.qbitcontroller.utils.setItemMargin
import dev.bartuzen.qbitcontroller.utils.showSnackbar

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

        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.torrent_trackers_menu, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                when (menuItem.itemId) {
                    R.id.menu_add -> {
                        TorrentTrackersAddDialogFragmentBuilder(serverConfig, torrentHash)
                            .build()
                            .show(childFragmentManager, null)
                    }
                    else -> return false
                }

                return true
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)

        val adapter = TorrentTrackersAdapter()
        binding.recyclerTrackers.adapter = adapter
        binding.recyclerTrackers.setItemMargin(8, 8)

        binding.swipeRefresh.setOnRefreshListener {
            viewModel.isRefreshing.value = true
            viewModel.updateTrackers(serverConfig, torrentHash).invokeOnCompletion {
                viewModel.isRefreshing.value = false
            }
        }

        if (!viewModel.isInitialLoadStarted) {
            viewModel.isInitialLoadStarted = true
            viewModel.updateTrackers(serverConfig, torrentHash).invokeOnCompletion {
                viewModel.isLoading.value = false
            }
        }

        viewModel.isLoading.launchAndCollectLatestIn(viewLifecycleOwner) { isLoading ->
            binding.progressIndicator.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.isRefreshing.launchAndCollectLatestIn(viewLifecycleOwner) { isRefreshing ->
            binding.swipeRefresh.isRefreshing = isRefreshing
        }

        viewModel.torrentTrackers.launchAndCollectLatestIn(viewLifecycleOwner) { trackers ->
            adapter.submitList(trackers)
        }

        viewModel.eventFlow.launchAndCollectIn(viewLifecycleOwner) { event ->
            when (event) {
                is TorrentTrackersViewModel.Event.Error -> {
                    showSnackbar(getErrorMessage(requireContext(), event.error))
                }
                TorrentTrackersViewModel.Event.TrackersAdded -> {
                    showSnackbar(getString(R.string.torrent_trackers_added))
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}