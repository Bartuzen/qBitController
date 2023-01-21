package dev.bartuzen.qbitcontroller.ui.rss.feeds

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.ConcatAdapter
import by.kirich1409.viewbindingdelegate.viewBinding
import dagger.hilt.android.AndroidEntryPoint
import dev.bartuzen.qbitcontroller.R
import dev.bartuzen.qbitcontroller.databinding.FragmentRssFeedsBinding
import dev.bartuzen.qbitcontroller.model.ServerConfig
import dev.bartuzen.qbitcontroller.ui.rss.articles.RssArticlesFragment
import dev.bartuzen.qbitcontroller.utils.getErrorMessage
import dev.bartuzen.qbitcontroller.utils.getParcelableCompat
import dev.bartuzen.qbitcontroller.utils.launchAndCollectIn
import dev.bartuzen.qbitcontroller.utils.launchAndCollectLatestIn
import dev.bartuzen.qbitcontroller.utils.requireAppCompatActivity
import dev.bartuzen.qbitcontroller.utils.setDefaultAnimations
import dev.bartuzen.qbitcontroller.utils.showSnackbar
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull

@AndroidEntryPoint
class RssFeedsFragment() : Fragment(R.layout.fragment_rss_feeds) {
    private val binding by viewBinding(FragmentRssFeedsBinding::bind)

    private val viewModel: RssFeedsViewModel by viewModels()

    private val serverConfig get() = arguments?.getParcelableCompat<ServerConfig>("serverConfig")!!

    constructor(serverConfig: ServerConfig) : this() {
        arguments = bundleOf("serverConfig" to serverConfig)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (!viewModel.isInitialLoadStarted) {
            viewModel.isInitialLoadStarted = true
            viewModel.loadRssFeeds(serverConfig)
        }

        val adapter = RssFeedsAdapter(
            onClick = { feedNode ->
                if (feedNode.isFeed) {
                    val feedPath = viewModel.currentDirectory.value.toList().reversed() + feedNode.name
                    parentFragmentManager.commit {
                        setReorderingAllowed(true)
                        setDefaultAnimations()
                        val fragment = RssArticlesFragment(serverConfig, feedPath)
                        replace(R.id.container, fragment)
                        addToBackStack(null)
                    }
                } else {
                    viewModel.goToFolder(feedNode.name)
                }
            }
        )
        val backButtonAdapter = RssFeedsBackButtonAdapter(
            onClick = {
                viewModel.goBack()
            }
        )
        binding.recyclerFeeds.adapter = ConcatAdapter(backButtonAdapter, adapter)

        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refreshRssFeeds(serverConfig)
        }

        viewModel.isLoading.launchAndCollectLatestIn(viewLifecycleOwner) { isLoading ->
            binding.progressIndicator.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.isRefreshing.launchAndCollectLatestIn(viewLifecycleOwner) { isRefreshing ->
            binding.swipeRefresh.isRefreshing = isRefreshing
        }

        combine(viewModel.rssFeeds, viewModel.currentDirectory) { feedNode, currentDirectory ->
            if (feedNode != null) {
                feedNode to currentDirectory
            } else {
                null
            }
        }.filterNotNull().launchAndCollectLatestIn(viewLifecycleOwner) { (feedNode, currentDirectory) ->
            val currentNode = feedNode.findFolder(currentDirectory)?.children?.sortedBy { it.name }
            if (currentNode != null) {
                adapter.submitList(currentNode) {
                    if (currentDirectory.isNotEmpty()) {
                        backButtonAdapter.currentDirectory = currentDirectory.reversed().joinToString("/")
                    } else {
                        backButtonAdapter.currentDirectory = null
                    }
                }
            } else {
                viewModel.goToRoot()
            }
        }

        viewModel.eventFlow.launchAndCollectIn(viewLifecycleOwner) { event ->
            when (event) {
                is RssFeedsViewModel.Event.Error -> {
                    showSnackbar(getErrorMessage(requireContext(), event.error))
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        requireAppCompatActivity().supportActionBar?.setTitle(R.string.rss_title)
    }
}
