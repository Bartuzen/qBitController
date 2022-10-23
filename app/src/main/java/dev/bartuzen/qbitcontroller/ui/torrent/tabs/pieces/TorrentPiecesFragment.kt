package dev.bartuzen.qbitcontroller.ui.torrent.tabs.pieces

import android.graphics.Rect
import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hannesdorfmann.fragmentargs.annotation.Arg
import com.hannesdorfmann.fragmentargs.annotation.FragmentWithArgs
import dagger.hilt.android.AndroidEntryPoint
import dev.bartuzen.qbitcontroller.R
import dev.bartuzen.qbitcontroller.databinding.FragmentTorrentPiecesBinding
import dev.bartuzen.qbitcontroller.model.ServerConfig
import dev.bartuzen.qbitcontroller.ui.base.ArgsFragment
import dev.bartuzen.qbitcontroller.utils.*
import kotlinx.coroutines.flow.filterNotNull

@FragmentWithArgs
@AndroidEntryPoint
class TorrentPiecesFragment : ArgsFragment(R.layout.fragment_torrent_pieces) {
    private var _binding: FragmentTorrentPiecesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TorrentPiecesViewModel by viewModels()

    @Arg
    lateinit var serverConfig: ServerConfig

    @Arg
    lateinit var torrentHash: String

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentTorrentPiecesBinding.bind(view)

        val adapter = TorrentPiecesAdapter()
        binding.recyclerPieces.adapter = adapter

        val displayMetrics = requireContext().resources.displayMetrics
        val screenWidthDp = displayMetrics.widthPixels.toDp(requireContext())
        val columns = (screenWidthDp - 32) / 20
        binding.recyclerPieces.layoutManager = GridLayoutManager(context, columns).apply {
            spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(position: Int) = if (position == 0) columns else 1
            }
        }

        binding.recyclerPieces.addItemDecoration(object : RecyclerView.ItemDecoration() {
            override fun getItemOffsets(
                outRect: Rect,
                view: View,
                parent: RecyclerView,
                state: RecyclerView.State
            ) {
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
            viewModel.updatePieces(serverConfig, torrentHash).invokeOnCompletion {
                binding.swipeRefresh.isRefreshing = false
            }
        }

        if (!viewModel.isInitialLoadStarted) {
            viewModel.isInitialLoadStarted = true
            viewModel.updatePieces(serverConfig, torrentHash).invokeOnCompletion {
                viewModel.isLoading.value = false
            }
        }

        viewModel.isLoading.launchAndCollectLatestIn(viewLifecycleOwner) { isLoading ->
            binding.progressIndicator.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.torrentProperties.filterNotNull()
            .launchAndCollectLatestIn(viewLifecycleOwner) { properties ->
                adapter.submitHeaderData(properties.piecesCount, properties.pieceSize)
            }

        viewModel.torrentPieces.filterNotNull()
            .launchAndCollectLatestIn(viewLifecycleOwner) { pieces ->
                adapter.submitList(pieces)
            }

        viewModel.eventFlow.launchAndCollectIn(viewLifecycleOwner) { event ->
            when (event) {
                is TorrentPiecesViewModel.Event.Error -> {
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