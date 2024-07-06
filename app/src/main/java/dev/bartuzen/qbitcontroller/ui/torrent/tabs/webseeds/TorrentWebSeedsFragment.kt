package dev.bartuzen.qbitcontroller.ui.torrent.tabs.webseeds

import android.graphics.Rect
import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.RecyclerView
import by.kirich1409.viewbindingdelegate.viewBinding
import dagger.hilt.android.AndroidEntryPoint
import dev.bartuzen.qbitcontroller.R
import dev.bartuzen.qbitcontroller.databinding.FragmentTorrentWebSeedsBinding
import dev.bartuzen.qbitcontroller.utils.applyNavigationBarInsets
import dev.bartuzen.qbitcontroller.utils.getErrorMessage
import dev.bartuzen.qbitcontroller.utils.launchAndCollectIn
import dev.bartuzen.qbitcontroller.utils.launchAndCollectLatestIn
import dev.bartuzen.qbitcontroller.utils.showSnackbar
import dev.bartuzen.qbitcontroller.utils.toPx
import dev.bartuzen.qbitcontroller.utils.view
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@AndroidEntryPoint
class TorrentWebSeedsFragment() : Fragment(R.layout.fragment_torrent_web_seeds) {
    private val binding by viewBinding(FragmentTorrentWebSeedsBinding::bind)

    private val viewModel: TorrentWebSeedsViewModel by viewModels()

    private val serverId get() = arguments?.getInt("serverId", -1).takeIf { it != -1 }!!
    private val torrentHash get() = arguments?.getString("torrentHash")!!

    constructor(serverId: Int, torrentHash: String) : this() {
        arguments = bundleOf(
            "serverId" to serverId,
            "torrentHash" to torrentHash,
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.recyclerWebSeeds.applyNavigationBarInsets()

        val adapter = TorrentWebSeedsAdapter()
        binding.recyclerWebSeeds.adapter = adapter
        binding.recyclerWebSeeds.addItemDecoration(object : RecyclerView.ItemDecoration() {
            override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
                val verticalPx = 8.toPx(requireContext())
                val horizontalPx = 8.toPx(requireContext())
                if (parent.getChildAdapterPosition(view) == 0) {
                    outRect.top = verticalPx
                }
                outRect.bottom = verticalPx
                outRect.left = horizontalPx
                outRect.right = horizontalPx
            }
        })

        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refreshWebSeeds(serverId, torrentHash)
        }

        if (!viewModel.isInitialLoadStarted) {
            viewModel.isInitialLoadStarted = true
            viewModel.loadWebSeeds(serverId, torrentHash)
        }

        binding.progressIndicator.setVisibilityAfterHide(View.GONE)
        viewModel.isNaturalLoading.launchAndCollectLatestIn(viewLifecycleOwner) { isNaturalLoading ->
            if (isNaturalLoading == true) {
                binding.progressIndicator.show()
            } else {
                binding.progressIndicator.hide()
            }
        }

        viewModel.isRefreshing.launchAndCollectLatestIn(viewLifecycleOwner) { isRefreshing ->
            binding.swipeRefresh.isRefreshing = isRefreshing
        }

        viewModel.webSeeds.launchAndCollectLatestIn(viewLifecycleOwner) { webSeeds ->
            adapter.submitList(webSeeds)
        }

        viewModel.autoRefreshInterval.launchAndCollectLatestIn(viewLifecycleOwner, Lifecycle.State.RESUMED) { interval ->
            if (interval != 0) {
                while (isActive) {
                    delay(interval * 1000L)
                    if (isActive) {
                        viewModel.loadWebSeeds(serverId, torrentHash, autoRefresh = true)
                    }
                }
            }
        }

        viewModel.eventFlow.launchAndCollectIn(viewLifecycleOwner) { event ->
            when (event) {
                is TorrentWebSeedsViewModel.Event.Error -> {
                    showSnackbar(getErrorMessage(requireContext(), event.error), view = requireActivity().view)
                }
                TorrentWebSeedsViewModel.Event.TorrentNotFound -> {
                    showSnackbar(R.string.torrent_error_not_found, view = requireActivity().view)
                }
            }
        }
    }
}
