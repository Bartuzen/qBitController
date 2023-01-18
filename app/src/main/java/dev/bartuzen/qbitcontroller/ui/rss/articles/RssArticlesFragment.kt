package dev.bartuzen.qbitcontroller.ui.rss.articles

import android.app.Activity
import android.content.Intent
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.os.bundleOf
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import by.kirich1409.viewbindingdelegate.viewBinding
import dagger.hilt.android.AndroidEntryPoint
import dev.bartuzen.qbitcontroller.R
import dev.bartuzen.qbitcontroller.databinding.FragmentRssArticlesBinding
import dev.bartuzen.qbitcontroller.model.Article
import dev.bartuzen.qbitcontroller.model.ServerConfig
import dev.bartuzen.qbitcontroller.ui.addtorrent.AddTorrentActivity
import dev.bartuzen.qbitcontroller.utils.getErrorMessage
import dev.bartuzen.qbitcontroller.utils.getParcelableCompat
import dev.bartuzen.qbitcontroller.utils.launchAndCollectIn
import dev.bartuzen.qbitcontroller.utils.launchAndCollectLatestIn
import dev.bartuzen.qbitcontroller.utils.requireAppCompatActivity
import dev.bartuzen.qbitcontroller.utils.showDialog
import dev.bartuzen.qbitcontroller.utils.showSnackbar
import dev.bartuzen.qbitcontroller.utils.toPx
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

@AndroidEntryPoint
class RssArticlesFragment() : Fragment(R.layout.fragment_rss_articles) {
    private val binding by viewBinding(FragmentRssArticlesBinding::bind)

    private val viewModel: RssArticlesViewModel by viewModels()

    private val serverConfig get() = arguments?.getParcelableCompat<ServerConfig>("serverConfig")!!
    private val feedPath get() = arguments?.getStringArrayList("feedPath")!!

    private val startAddTorrentActivity =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val isAdded = result.data?.getBooleanExtra(
                    AddTorrentActivity.Extras.IS_ADDED,
                    false
                ) ?: false
                if (isAdded) {
                    showSnackbar(R.string.torrent_add_success)
                }
            }
        }

    constructor(serverConfig: ServerConfig, feedPath: List<String>) : this() {
        arguments = bundleOf(
            "serverConfig" to serverConfig,
            "feedPath" to ArrayList(feedPath)
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        requireAppCompatActivity().supportActionBar?.title = feedPath.last()

        requireActivity().addMenuProvider(
            object : MenuProvider {
                override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                    menuInflater.inflate(R.menu.torrent_rss_articles_menu, menu)
                }

                override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                    when (menuItem.itemId) {
                        R.id.menu_refresh -> {
                            viewModel.refreshFeed(serverConfig, feedPath)
                        }
                        else -> return false
                    }
                    return true
                }
            },
            viewLifecycleOwner,
            Lifecycle.State.RESUMED
        )

        if (!viewModel.isInitialLoadStarted) {
            viewModel.isInitialLoadStarted = true
            viewModel.loadRssFeed(serverConfig, feedPath)
        }

        val adapter = RssArticlesAdapter(
            onClick = { article ->
                showArticleDialog(
                    article = article,
                    onDownload = {
                        val intent = Intent(requireActivity(), AddTorrentActivity::class.java).apply {
                            putExtra(AddTorrentActivity.Extras.SERVER_CONFIG, serverConfig)
                            putExtra(AddTorrentActivity.Extras.TORRENT_URL, article.torrentUrl)
                        }
                        startAddTorrentActivity.launch(intent)
                    }
                )
            }
        )
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
                RssArticlesViewModel.Event.RssFeedNotFound -> {
                    showSnackbar(R.string.rss_error_feed_not_found)
                }
                RssArticlesViewModel.Event.FeedRefreshed -> {
                    showSnackbar(R.string.rss_success_feed_refresh)

                    viewLifecycleOwner.lifecycleScope.launch {
                        delay(1000)
                        viewModel.loadRssFeed(serverConfig, feedPath)
                    }
                }
            }
        }
    }

    private fun showArticleDialog(article: Article, onDownload: () -> Unit) {
        showDialog {
            setTitle(article.title)

            val description = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Html.fromHtml(article.description, Html.FROM_HTML_MODE_COMPACT)
            } else {
                @Suppress("DEPRECATION")
                Html.fromHtml(article.description)
            }
            setMessage(description)

            setPositiveButton(R.string.rss_download) { _, _ ->
                onDownload()
            }
        }
    }
}
