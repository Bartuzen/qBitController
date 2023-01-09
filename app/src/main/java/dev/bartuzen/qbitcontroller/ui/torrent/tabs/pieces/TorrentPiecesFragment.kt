package dev.bartuzen.qbitcontroller.ui.torrent.tabs.pieces

import android.graphics.Rect
import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import by.kirich1409.viewbindingdelegate.viewBinding
import dagger.hilt.android.AndroidEntryPoint
import dev.bartuzen.qbitcontroller.R
import dev.bartuzen.qbitcontroller.databinding.FragmentTorrentPiecesBinding
import dev.bartuzen.qbitcontroller.model.ServerConfig
import dev.bartuzen.qbitcontroller.utils.getErrorMessage
import dev.bartuzen.qbitcontroller.utils.getParcelableCompat
import dev.bartuzen.qbitcontroller.utils.launchAndCollectIn
import dev.bartuzen.qbitcontroller.utils.launchAndCollectLatestIn
import dev.bartuzen.qbitcontroller.utils.showSnackbar
import dev.bartuzen.qbitcontroller.utils.toDp
import dev.bartuzen.qbitcontroller.utils.toPx
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.isActive

@AndroidEntryPoint
class TorrentPiecesFragment() : Fragment(R.layout.fragment_torrent_pieces) {
    private val binding by viewBinding(FragmentTorrentPiecesBinding::bind)

    private val viewModel: TorrentPiecesViewModel by viewModels()

    private val serverConfig get() = arguments?.getParcelableCompat<ServerConfig>("serverConfig")!!
    private val torrentHash get() = arguments?.getString("torrentHash")!!

    constructor(serverConfig: ServerConfig, torrentHash: String) : this() {
        arguments = bundleOf(
            "serverConfig" to serverConfig,
            "torrentHash" to torrentHash
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val adapter = TorrentPiecesAdapter()
        val headerAdapter = TorrentPiecesHeaderAdapter()
        binding.recyclerPieces.adapter = ConcatAdapter(headerAdapter, adapter)

        val displayMetrics = requireContext().resources.displayMetrics
        val screenWidthDp = displayMetrics.widthPixels.toDp(requireContext())
        val columns = (screenWidthDp - 32) / 20
        binding.recyclerPieces.layoutManager = GridLayoutManager(context, columns).apply {
            spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(position: Int) = if (position == 0) columns else 1
            }
        }

        binding.recyclerPieces.addItemDecoration(object : RecyclerView.ItemDecoration() {
            override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
                parent.adapter?.itemCount?.let { itemCount ->
                    val position = parent.getChildAdapterPosition(view) - 1
                    if (position != -1) {
                        if (position < columns) {
                            outRect.top = 8.toPx(requireContext())
                        }
                        if (itemCount % columns != 0) {
                            if (itemCount / columns * columns < position + 1) {
                                outRect.bottom = 8.toPx(requireContext())
                            }
                        } else if (itemCount / columns * columns < position + columns + 1) {
                            outRect.bottom = 8.toPx(requireContext())
                        }
                        if (outRect.bottom == 0) {
                            outRect.bottom = (screenWidthDp / columns - 18).toPx(requireContext())
                        }
                    } else {
                        outRect.top = 16.toPx(requireContext())
                    }
                }
            }
        })

        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refreshPieces(serverConfig, torrentHash)
        }

        if (!viewModel.isInitialLoadStarted) {
            viewModel.isInitialLoadStarted = true
            viewModel.loadPieces(serverConfig, torrentHash)
        }

        viewModel.isLoading.launchAndCollectLatestIn(viewLifecycleOwner) { isLoading ->
            binding.progressIndicator.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.isRefreshing.launchAndCollectLatestIn(viewLifecycleOwner) { isRefreshing ->
            binding.swipeRefresh.isRefreshing = isRefreshing
        }

        viewModel.torrentProperties.filterNotNull().launchAndCollectLatestIn(viewLifecycleOwner) { properties ->
            headerAdapter.submitHeaderData(properties.piecesCount, properties.pieceSize)
        }

        viewModel.torrentPieces.filterNotNull().launchAndCollectLatestIn(viewLifecycleOwner) { pieces ->
            adapter.submitPieces(pieces)
        }

        viewModel.autoRefreshInterval.launchAndCollectLatestIn(viewLifecycleOwner, Lifecycle.State.RESUMED) { interval ->
            if (interval != 0) {
                while (isActive) {
                    delay(interval * 1000L)
                    if (isActive) {
                        viewModel.loadPieces(serverConfig, torrentHash)
                    }
                }
            }
        }

        viewModel.eventFlow.launchAndCollectIn(viewLifecycleOwner) { event ->
            when (event) {
                is TorrentPiecesViewModel.Event.Error -> {
                    showSnackbar(getErrorMessage(requireContext(), event.error))
                }

                TorrentPiecesViewModel.Event.TorrentNotFound -> {
                    showSnackbar(R.string.torrent_error_not_found)
                }
            }
        }
    }
}
