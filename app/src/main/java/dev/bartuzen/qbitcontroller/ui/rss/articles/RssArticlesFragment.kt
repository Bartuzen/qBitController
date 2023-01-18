package dev.bartuzen.qbitcontroller.ui.rss.articles

import android.graphics.Rect
import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.RecyclerView
import by.kirich1409.viewbindingdelegate.viewBinding
import dagger.hilt.android.AndroidEntryPoint
import dev.bartuzen.qbitcontroller.R
import dev.bartuzen.qbitcontroller.databinding.FragmentRssArticlesBinding
import dev.bartuzen.qbitcontroller.model.ServerConfig
import dev.bartuzen.qbitcontroller.utils.getErrorMessage
import dev.bartuzen.qbitcontroller.utils.getParcelableCompat
import dev.bartuzen.qbitcontroller.utils.launchAndCollectIn
import dev.bartuzen.qbitcontroller.utils.launchAndCollectLatestIn
import dev.bartuzen.qbitcontroller.utils.showSnackbar
import dev.bartuzen.qbitcontroller.utils.toPx
import kotlinx.coroutines.flow.filterNotNull

@AndroidEntryPoint
class RssArticlesFragment() : Fragment(R.layout.fragment_rss_articles) {
    private val binding by viewBinding(FragmentRssArticlesBinding::bind)

    private val viewModel: RssArticlesViewModel by viewModels()

    private val serverConfig get() = arguments?.getParcelableCompat<ServerConfig>("serverConfig")!!
    private val feedPath get() = arguments?.getStringArrayList("feedPath")!!

    constructor(serverConfig: ServerConfig, feedPath: List<String>) : this() {
        arguments = bundleOf(
            "serverConfig" to serverConfig,
            "feedPath" to ArrayList(feedPath)
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (!viewModel.isInitialLoadStarted) {
            viewModel.isInitialLoadStarted = true
            viewModel.loadRssFeed(serverConfig, feedPath)
        }

        val adapter = RssArticlesAdapter()
        binding.recyclerArticles.adapter = adapter
        binding.recyclerArticles.addItemDecoration(object : RecyclerView.ItemDecoration() {
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
            viewModel.refreshRssFeed(serverConfig, feedPath)
        }

        viewModel.isLoading.launchAndCollectLatestIn(viewLifecycleOwner) { isLoading ->
            binding.progressIndicator.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.isRefreshing.launchAndCollectLatestIn(viewLifecycleOwner) { isRefreshing ->
            binding.swipeRefresh.isRefreshing = isRefreshing
        }

        viewModel.rssFeed.filterNotNull().launchAndCollectLatestIn(viewLifecycleOwner) { feed ->
            adapter.submitList(feed.articles)
        }

        viewModel.eventFlow.launchAndCollectIn(viewLifecycleOwner) { event ->
            when (event) {
                is RssArticlesViewModel.Event.Error -> {
                    showSnackbar(getErrorMessage(requireContext(), event.error))
                }
            }
        }
    }
}
