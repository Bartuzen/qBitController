package dev.bartuzen.qbitcontroller.ui.rss.feeds

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AlertDialog
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
import dev.bartuzen.qbitcontroller.databinding.DialogRssMoveFeedFolderBinding
import dev.bartuzen.qbitcontroller.databinding.DialogRssRenameFeedFolderBinding
import dev.bartuzen.qbitcontroller.databinding.FragmentRssFeedsBinding
import dev.bartuzen.qbitcontroller.model.RssFeedNode
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
import dev.bartuzen.qbitcontroller.utils.setTextWithoutAnimation
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
            },
            onLongClick = { feedNode ->
                showLongClickDialog(feedNode)
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
                RssFeedsViewModel.Event.FeedAddError -> {
                    showSnackbar(R.string.rss_error_feed_add)
                }
                RssFeedsViewModel.Event.FeedRenameError -> {
                    showSnackbar(R.string.rss_error_feed_rename)
                }
                RssFeedsViewModel.Event.FeedMoveError -> {
                    showSnackbar(R.string.rss_error_feed_move)
                }
                RssFeedsViewModel.Event.FeedDeleteError -> {
                    showSnackbar(R.string.rss_error_feed_delete)
                }
                RssFeedsViewModel.Event.FolderAddError -> {
                    showSnackbar(R.string.rss_error_folder_add)
                }
                RssFeedsViewModel.Event.FolderRenameError -> {
                    showSnackbar(R.string.rss_error_folder_rename)
                }
                RssFeedsViewModel.Event.FolderMoveError -> {
                    showSnackbar(R.string.rss_error_folder_move)
                }
                RssFeedsViewModel.Event.FolderDeleteError -> {
                    showSnackbar(R.string.rss_error_folder_delete)
                }
                RssFeedsViewModel.Event.FeedAdded -> {
                    showSnackbar(R.string.rss_success_feed_add)
                    viewModel.loadRssFeeds(serverConfig)
                }
                RssFeedsViewModel.Event.FeedRenamed -> {
                    showSnackbar(R.string.rss_success_feed_rename)
                    viewModel.loadRssFeeds(serverConfig)
                }
                RssFeedsViewModel.Event.FeedMoved -> {
                    showSnackbar(R.string.rss_success_feed_move)
                    viewModel.loadRssFeeds(serverConfig)
                }
                RssFeedsViewModel.Event.FeedDeleted -> {
                    showSnackbar(R.string.rss_success_feed_delete)
                    viewModel.loadRssFeeds(serverConfig)
                }
                RssFeedsViewModel.Event.FolderAdded -> {
                    showSnackbar(R.string.rss_success_folder_add)
                    viewModel.loadRssFeeds(serverConfig)
                }
                RssFeedsViewModel.Event.FolderRenamed -> {
                    showSnackbar(R.string.rss_success_folder_rename)
                    viewModel.loadRssFeeds(serverConfig)
                }
                RssFeedsViewModel.Event.FolderMoved -> {
                    showSnackbar(R.string.rss_success_folder_move)
                    viewModel.loadRssFeeds(serverConfig)
                }
                RssFeedsViewModel.Event.FolderDeleted -> {
                    showSnackbar(R.string.rss_success_folder_delete)
                    viewModel.loadRssFeeds(serverConfig)
                }
            }
        }
    }

    private fun showAddFeedDialog() {
        lateinit var dialogBinding: DialogRssAddFeedBinding

        val dialog = showDialog(DialogRssAddFeedBinding::inflate) { binding ->
            dialogBinding = binding

            setTitle(R.string.rss_menu_add_feed)
            setPositiveButton()
            setNegativeButton()
        }

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val feedUrl = dialogBinding.editFeedUrl.text.toString()
            if (feedUrl.isNotBlank()) {
                val name = dialogBinding.editName.text.toString().ifBlank { null }
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
                dialog.dismiss()
            } else {
                dialogBinding.inputLayoutFeedUrl.error = getString(R.string.rss_field_required)
            }
        }
    }

    private fun showAddFolderDialog() {
        lateinit var dialogBinding: DialogRssAddFolderBinding

        val dialog = showDialog(DialogRssAddFolderBinding::inflate) { binding ->
            dialogBinding = binding

            setTitle(R.string.rss_menu_add_folder)
            setPositiveButton()
            setNegativeButton()
        }

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val path = dialogBinding.editName.text.toString()
            if (path.isNotBlank()) {
                val currentDirectory = viewModel.currentDirectory.value.reversed().joinToString("\\").ifEmpty { null }

                val fullPath = if (currentDirectory != null) {
                    "$currentDirectory\\$path"
                } else {
                    path
                }

                viewModel.addRssFolder(serverConfig, fullPath)
                dialog.dismiss()
            } else {
                dialogBinding.inputLayoutName.error = getString(R.string.rss_field_required)
            }
        }
    }

    private fun showLongClickDialog(feedNode: RssFeedNode) {
        showDialog {
            setItems(
                if (feedNode.isFeed) {
                    arrayOf(
                        getString(R.string.rss_menu_rename_feed),
                        getString(R.string.rss_menu_move_feed),
                        getString(R.string.rss_menu_delete_feed)
                    )
                } else {
                    arrayOf(
                        getString(R.string.rss_menu_rename_folder),
                        getString(R.string.rss_menu_move_folder),
                        getString(R.string.rss_menu_delete_folder)
                    )
                }
            ) { _, which ->
                when (which) {
                    0 -> {
                        showRenameFeedFolderDialog(
                            name = feedNode.name,
                            isFeed = feedNode.isFeed,
                            onRename = { from, to ->
                                viewModel.renameItem(serverConfig, from, to, feedNode.isFeed)
                            }
                        )
                    }
                    1 -> {
                        showMoveFeedFolderDialog(
                            name = feedNode.name,
                            isFeed = feedNode.isFeed,
                            onMove = { from, to ->
                                viewModel.moveItem(serverConfig, from, to, feedNode.isFeed)
                            }
                        )
                    }
                    2 -> {
                        val feedPath = (viewModel.currentDirectory.value.reversed() + feedNode.name).joinToString("\\")
                        showDeleteFeedFolderDialog(
                            name = feedNode.name,
                            isFeed = feedNode.isFeed,
                            onDelete = {
                                viewModel.deleteItem(serverConfig, feedPath, feedNode.isFeed)
                            }
                        )
                    }
                }
            }
        }
    }

    private fun showMoveFeedFolderDialog(name: String, isFeed: Boolean, onMove: (from: String, to: String) -> Unit) {
        showDialog(DialogRssMoveFeedFolderBinding::inflate) { binding ->
            val currentDirectory = viewModel.currentDirectory.value.reversed()
            binding.inputLayoutName.setTextWithoutAnimation(currentDirectory.joinToString("\\"))

            if (isFeed) {
                setTitle(R.string.rss_menu_move_feed)
            } else {
                setTitle(R.string.rss_menu_move_folder)
            }

            setPositiveButton { _, _ ->
                val from = (currentDirectory.toList() + name).joinToString("\\")
                val to = binding.editName.text.toString().let { to ->
                    if (to.isBlank()) name else "$to\\$name"
                }

                onMove(from, to)
            }
            setNegativeButton()
        }
    }

    private fun showRenameFeedFolderDialog(name: String, isFeed: Boolean, onRename: (from: String, to: String) -> Unit) {
        lateinit var dialogBinding: DialogRssRenameFeedFolderBinding

        val dialog = showDialog(DialogRssRenameFeedFolderBinding::inflate) { binding ->
            dialogBinding = binding

            binding.inputLayoutName.setTextWithoutAnimation(name)

            if (isFeed) {
                setTitle(R.string.rss_menu_rename_feed)
                binding.inputLayoutName.setHint(R.string.rss_hint_feed_name)
            } else {
                setTitle(R.string.rss_menu_rename_folder)
                binding.inputLayoutName.setHint(R.string.rss_hint_folder_name)
            }

            setPositiveButton()
            setNegativeButton()
        }

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val newName = dialogBinding.editName.text.toString()
            if (newName.isNotBlank()) {
                val currentDirectory = viewModel.currentDirectory.value.reversed()
                val from = (currentDirectory.toList() + name).joinToString("\\")
                val to = (currentDirectory.toList() + newName).joinToString("\\")

                onRename(from, to)
                dialog.dismiss()
            } else {
                dialogBinding.inputLayoutName.error = getString(R.string.rss_field_required)
            }
        }
    }

    private fun showDeleteFeedFolderDialog(name: String, isFeed: Boolean, onDelete: () -> Unit) {
        showDialog {
            if (isFeed) {
                setTitle(R.string.rss_menu_delete_feed)
                setMessage(getString(R.string.rss_confirm_delete_feed, name))
            } else {
                setTitle(R.string.rss_menu_delete_folder)
                setMessage(getString(R.string.rss_confirm_delete_folder, name))
            }
            setPositiveButton { _, _ ->
                onDelete()
            }
            setNegativeButton()
        }
    }

    override fun onResume() {
        super.onResume()
        requireAppCompatActivity().supportActionBar?.setTitle(R.string.rss_title)
    }
}
