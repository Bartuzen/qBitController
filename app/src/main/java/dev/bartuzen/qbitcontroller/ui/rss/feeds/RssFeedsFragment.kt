package dev.bartuzen.qbitcontroller.ui.rss.feeds

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.core.os.bundleOf
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.ConcatAdapter
import by.kirich1409.viewbindingdelegate.viewBinding
import dagger.hilt.android.AndroidEntryPoint
import dev.bartuzen.qbitcontroller.R
import dev.bartuzen.qbitcontroller.databinding.DialogRssAddFeedBinding
import dev.bartuzen.qbitcontroller.databinding.DialogRssAddFolderBinding
import dev.bartuzen.qbitcontroller.databinding.FragmentRssFeedsBinding
import dev.bartuzen.qbitcontroller.model.ServerConfig
import dev.bartuzen.qbitcontroller.ui.rss.articles.RssArticlesFragment
import dev.bartuzen.qbitcontroller.utils.getErrorMessage
import dev.bartuzen.qbitcontroller.utils.getParcelableCompat
import dev.bartuzen.qbitcontroller.utils.launchAndCollectIn
import dev.bartuzen.qbitcontroller.utils.launchAndCollectLatestIn
import dev.bartuzen.qbitcontroller.utils.requireAppCompatActivity
import dev.bartuzen.qbitcontroller.utils.setDefaultAnimations
import dev.bartuzen.qbitcontroller.utils.setNegativeButton
import dev.bartuzen.qbitcontroller.utils.setPositiveButton
import dev.bartuzen.qbitcontroller.utils.showDialog
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
        requireActivity().addMenuProvider(
            object : MenuProvider {
                override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                    menuInflater.inflate(R.menu.rss_feeds_menu, menu)
                }

                override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                    when (menuItem.itemId) {
                        R.id.menu_add -> {
                            showDialog {
                                setItems(
                                    arrayOf(
                                        getString(R.string.rss_menu_add_feed),
                                        getString(R.string.rss_menu_add_folder)
                                    )
                                ) { _, which ->
                                    when (which) {
                                        0 -> {
                                            showAddFeedDialog()
                                        }
                                        1 -> {
                                            showAddFolderDialog()
                                        }
                                    }
                                }
                            }
                        }
                        else -> return false
                    }
                    return true
                }
            },
            viewLifecycleOwner
        )

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
            val currentNode = feedNode.findFolder(currentDirectory)?.children?.sortedBy { it.name.lowercase() }
            if (currentNode != null) {
                adapter.submitList(currentNode) {
                    if (currentDirectory.isNotEmpty()) {
                        backButtonAdapter.currentDirectory = currentDirectory.reversed().joinToString("\\")
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
                RssFeedsViewModel.Event.FeedAdded -> {
                    showSnackbar(R.string.rss_success_feed_add)
                    viewModel.loadRssFeeds(serverConfig)
                }
                RssFeedsViewModel.Event.FolderAdded -> {
                    showSnackbar(R.string.rss_success_folder_add)
                    viewModel.loadRssFeeds(serverConfig)
                }
            }
        }
    }

    private fun showAddFeedDialog() {
        showDialog(DialogRssAddFeedBinding::inflate) { binding ->
            setTitle(R.string.rss_menu_add_feed)
            setPositiveButton { _, _ ->
                val feedUrl = binding.editFeedUrl.text.toString()
                val name = binding.editName.text.toString().ifBlank { null }
                val currentDirectory = viewModel.currentDirectory.value.reversed().joinToString("\\").ifEmpty { null }

                val fullPath = when {
                    currentDirectory != null && name != null -> {
                        "$currentDirectory\\$name"
                    }
                    currentDirectory != null && name == null -> {
                        "$currentDirectory\\$feedUrl"
                    }
                    currentDirectory == null && name != null -> {
                        name
                    }
                    else -> {
                        feedUrl
                    }
                }

                viewModel.addRssFeed(serverConfig, feedUrl, fullPath)
            }
            setNegativeButton()
        }
    }

    private fun showAddFolderDialog() {
        showDialog(DialogRssAddFolderBinding::inflate) { binding ->
            setTitle(R.string.rss_menu_add_folder)
            setPositiveButton { _, _ ->
                val path = binding.editName.text.toString()
                val currentDirectory = viewModel.currentDirectory.value.reversed().joinToString("\\").ifEmpty { null }

                val fullPath = if (currentDirectory != null) {
                    "$currentDirectory\\$path"
                } else {
                    path
                }

                viewModel.addRssFolder(serverConfig, fullPath)
            }
            setNegativeButton()
        }
    }

    override fun onResume() {
        super.onResume()
        requireAppCompatActivity().supportActionBar?.setTitle(R.string.rss_title)
    }
}
