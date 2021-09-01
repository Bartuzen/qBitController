package dev.bartuzen.qbitcontroller.ui.torrent.tabs.pieces

import android.graphics.Rect
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import dev.bartuzen.qbitcontroller.R
import dev.bartuzen.qbitcontroller.databinding.FragmentTorrentPiecesBinding
import dev.bartuzen.qbitcontroller.network.RequestResult
import dev.bartuzen.qbitcontroller.ui.torrent.TorrentViewModel
import dev.bartuzen.qbitcontroller.utils.getErrorMessage
import dev.bartuzen.qbitcontroller.utils.showSnackbar
import dev.bartuzen.qbitcontroller.utils.toDp
import dev.bartuzen.qbitcontroller.utils.toPx
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

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
        binding.recyclerFileList.adapter = adapter

        val displayMetrics = requireContext().resources.displayMetrics
        val screenWidthDp = displayMetrics.widthPixels.toDp(requireContext())
        val columns = (screenWidthDp - 32) / 20
        binding.recyclerFileList.layoutManager = GridLayoutManager(context, columns).apply {
            spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(position: Int) = if (position == 0) columns else 1
            }
        }

        binding.recyclerFileList.addItemDecoration(object : RecyclerView.ItemDecoration() {
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
            lifecycleScope.launch {
                viewModel.updatePieces().join()
                viewModel.updateProperties().join()
                binding.swipeRefresh.isRefreshing = false
            }
        }

        if (!viewModel.torrentProperties.isSet) {
            viewModel.updateProperties()
        }

        if (viewModel.torrentPieces.isSet) {
            binding.progressIndicator.visibility = View.GONE
        } else {
            viewModel.updatePieces()
        }

        viewModel.torrentProperties.observe(viewLifecycleOwner) { properties ->
            properties?.apply {
                adapter.submitHeaderData(piecesCount, pieceSize)
            }
        }

        viewModel.torrentPieces.observe(viewLifecycleOwner) { pieces ->
            pieces?.let {
                adapter.submitList(it)
            }
        }

        lifecycleScope.launchWhenStarted {
            viewModel.torrentPiecesEvent.collect { event ->
                when (event) {
                    is TorrentViewModel.TorrentPiecesEvent.OnRequestComplete -> {
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