package dev.bartuzen.qbitcontroller.ui.torrent.tabs.pieces

import android.graphics.Rect
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import dev.bartuzen.qbitcontroller.R
import dev.bartuzen.qbitcontroller.databinding.FragmentTorrentPiecesBinding
import dev.bartuzen.qbitcontroller.ui.torrent.TorrentViewModel
import dev.bartuzen.qbitcontroller.utils.*
import kotlinx.coroutines.flow.filterNotNull

@AndroidEntryPoint
class TorrentPiecesFragment : Fragment(R.layout.fragment_torrent_pieces) {
    private var _binding: FragmentTorrentPiecesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TorrentViewModel by viewModels(
        ownerProducer = { requireActivity() }
    )

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
            viewModel.updatePiecesAndProperties().invokeOnCompletion {
                binding.swipeRefresh.isRefreshing = false
            }
        }

        if (savedInstanceState == null) {
            viewModel.updatePiecesAndProperties().invokeOnCompletion {
                viewModel.isTorrentPiecesLoading.value = false
            }
        }

        viewModel.isTorrentPiecesLoading.launchAndCollectLatestIn(viewLifecycleOwner) { isLoading ->
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

        viewModel.torrentPiecesEvent.launchAndCollectIn(viewLifecycleOwner) { event ->
            when (event) {
                is TorrentViewModel.TorrentPiecesEvent.OnError -> {
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