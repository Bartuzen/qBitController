package dev.bartuzen.qbitcontroller.ui.rss.feeds

import android.os.Bundle
import android.view.ActionMode
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.PopupMenu
import androidx.core.os.bundleOf
import androidx.core.view.MenuCompat
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import by.kirich1409.viewbindingdelegate.viewBinding
import dagger.hilt.android.AndroidEntryPoint
import dev.bartuzen.qbitcontroller.R
import dev.bartuzen.qbitcontroller.databinding.DialogRssAddFeedBinding
import dev.bartuzen.qbitcontroller.databinding.DialogRssAddFolderBinding
import dev.bartuzen.qbitcontroller.databinding.DialogRssRenameFeedFolderBinding
import dev.bartuzen.qbitcontroller.databinding.FragmentRssFeedsBinding
import dev.bartuzen.qbitcontroller.model.RssFeedNode
import dev.bartuzen.qbitcontroller.ui.rss.articles.RssArticlesFragment
import dev.bartuzen.qbitcontroller.ui.rss.rules.RssRulesFragment
import dev.bartuzen.qbitcontroller.utils.applySystemBarInsets
import dev.bartuzen.qbitcontroller.utils.getErrorMessage
import dev.bartuzen.qbitcontroller.utils.launchAndCollectIn
import dev.bartuzen.qbitcontroller.utils.launchAndCollectLatestIn
import dev.bartuzen.qbitcontroller.utils.requireAppCompatActivity
import dev.bartuzen.qbitcontroller.utils.setDefaultAnimations
import dev.bartuzen.qbitcontroller.utils.setNegativeButton
import dev.bartuzen.qbitcontroller.utils.setPositiveButton
import dev.bartuzen.qbitcontroller.utils.setTextWithoutAnimation
import dev.bartuzen.qbitcontroller.utils.showDialog
import dev.bartuzen.qbitcontroller.utils.showSnackbar
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

@AndroidEntryPoint
class RssFeedsFragment() : Fragment(R.layout.fragment_rss_feeds) {
    private val binding by viewBinding(FragmentRssFeedsBinding::bind)

    private val viewModel: RssFeedsViewModel by viewModels()

    private val serverId get() = arguments?.getInt("serverId", -1).takeIf { it != -1 }!!

    private var movingItem: RssFeedNode? = null

    private var actionMode: ActionMode? = null

    constructor(serverId: Int) : this() {
        arguments = bundleOf("serverId" to serverId)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.progressIndicator.applySystemBarInsets(top = false, bottom = false)
        binding.recyclerFeeds.applySystemBarInsets(top = false)

        requireActivity().addMenuProvider(
            object : MenuProvider {
                override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                    menuInflater.inflate(R.menu.rss_feeds, menu)
                }

                override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                    when (menuItem.itemId) {
                        R.id.menu_rules -> {
                            val fragment = RssRulesFragment(serverId)
                            parentFragmentManager.commit {
                                setReorderingAllowed(true)
                                setDefaultAnimations()
                                replace(R.id.container, fragment)
                                addToBackStack(null)
                            }
                        }
                        R.id.menu_refresh -> {
                            viewModel.refreshAllFeeds(serverId)
                        }
                        R.id.menu_add_feed -> {
                            showAddFeedDialog(null)
                        }
                        R.id.menu_add_folder -> {
                            showAddFolderDialog(null)
                        }
                        else -> return false
                    }
                    return true
                }
            },
            viewLifecycleOwner,
            Lifecycle.State.RESUMED,
        )

        if (!viewModel.isInitialLoadStarted) {
            viewModel.isInitialLoadStarted = true
            viewModel.loadRssFeeds(serverId)
        }

        val adapter = RssFeedsAdapter(
            collapsedNodes = viewModel.collapsedNodes,
            onClick = { feedNode ->
                val movingItem = movingItem
                if (movingItem == null) {
                    parentFragmentManager.commit {
                        setReorderingAllowed(true)
                        setDefaultAnimations()
                        val fragment = RssArticlesFragment(serverId, feedNode.path, feedNode.feed?.uid)
                        replace(R.id.container, fragment)
                        addToBackStack(null)
                    }
                } else {
                    if (feedNode.isFolder) {
                        val from = movingItem.path.joinToString("\\")
                        val to = (feedNode.path + movingItem.name).joinToString("\\")

                        viewModel.moveItem(serverId, from, to, feedNode.isFeed)

                        this@RssFeedsFragment.movingItem = null
                        actionMode?.finish()
                        actionMode = null
                    }
                }
            },
            onLongClick = { feedNode, rootView ->
                showLongClickMenu(feedNode, rootView)
            },
        )
        binding.recyclerFeeds.adapter = adapter

        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refreshRssFeeds(serverId)
        }

        binding.progressIndicator.setVisibilityAfterHide(View.GONE)
        viewModel.isLoading.launchAndCollectLatestIn(viewLifecycleOwner) { isLoading ->
            if (isLoading) {
                binding.progressIndicator.show()
            } else {
                binding.progressIndicator.hide()
            }
        }

        viewModel.isRefreshing.launchAndCollectLatestIn(viewLifecycleOwner) { isRefreshing ->
            binding.swipeRefresh.isRefreshing = isRefreshing
        }

        viewModel.rssFeeds.filterNotNull().launchAndCollectLatestIn(viewLifecycleOwner) { feedNode ->
            adapter.setNode(feedNode)
        }

        setFragmentResultListener("rssArticlesResult") { _, bundle ->
            val isUpdated = bundle.getBoolean("isUpdated", false)
            if (isUpdated) {
                viewModel.loadRssFeeds(serverId)
            }
        }

        viewModel.eventFlow.launchAndCollectIn(viewLifecycleOwner) { event ->
            when (event) {
                is RssFeedsViewModel.Event.Error -> {
                    showSnackbar(getErrorMessage(requireContext(), event.error))
                }
                RssFeedsViewModel.Event.AllFeedsRefreshed -> {
                    showSnackbar(R.string.rss_refresh_all_feeds_success)

                    viewLifecycleOwner.lifecycleScope.launch {
                        delay(1000)
                        viewModel.loadRssFeeds(serverId)
                    }
                }
                RssFeedsViewModel.Event.FeedAddError -> {
                    showSnackbar(R.string.rss_add_feed_error)
                }
                RssFeedsViewModel.Event.FeedRenameError -> {
                    showSnackbar(R.string.rss_rename_feed_error)
                }
                RssFeedsViewModel.Event.FeedMoveError -> {
                    showSnackbar(R.string.rss_move_feed_error)
                }
                RssFeedsViewModel.Event.FeedDeleteError -> {
                    showSnackbar(R.string.rss_delete_feed_error)
                }
                RssFeedsViewModel.Event.FolderAddError -> {
                    showSnackbar(R.string.rss_add_folder_error)
                }
                RssFeedsViewModel.Event.FolderRenameError -> {
                    showSnackbar(R.string.rss_rename_folder_error)
                }
                RssFeedsViewModel.Event.FolderMoveError -> {
                    showSnackbar(R.string.rss_move_folder_error)
                }
                RssFeedsViewModel.Event.FolderDeleteError -> {
                    showSnackbar(R.string.rss_delete_folder_error)
                }
                RssFeedsViewModel.Event.FeedAdded -> {
                    showSnackbar(R.string.rss_added_feed)
                    viewModel.loadRssFeeds(serverId)
                }
                RssFeedsViewModel.Event.FeedRenamed -> {
                    showSnackbar(R.string.rss_success_feed_rename)
                    viewModel.loadRssFeeds(serverId)
                }
                RssFeedsViewModel.Event.FeedMoved -> {
                    showSnackbar(R.string.rss_success_feed_move)
                    viewModel.loadRssFeeds(serverId)
                }
                RssFeedsViewModel.Event.FeedDeleted -> {
                    showSnackbar(R.string.rss_success_feed_delete)
                    viewModel.loadRssFeeds(serverId)
                }
                RssFeedsViewModel.Event.FolderAdded -> {
                    showSnackbar(R.string.rss_success_folder_add)
                    viewModel.loadRssFeeds(serverId)
                }
                RssFeedsViewModel.Event.FolderRenamed -> {
                    showSnackbar(R.string.rss_success_folder_rename)
                    viewModel.loadRssFeeds(serverId)
                }
                RssFeedsViewModel.Event.FolderMoved -> {
                    showSnackbar(R.string.rss_success_folder_move)
                    viewModel.loadRssFeeds(serverId)
                }
                RssFeedsViewModel.Event.FolderDeleted -> {
                    showSnackbar(R.string.rss_success_folder_delete)
                    viewModel.loadRssFeeds(serverId)
                }
            }
        }
    }

    private fun showAddFeedDialog(feedNode: RssFeedNode?) {
        lateinit var dialogBinding: DialogRssAddFeedBinding

        val dialog = showDialog(DialogRssAddFeedBinding::inflate) { binding ->
            dialogBinding = binding

            setTitle(R.string.rss_action_add_feed)
            setPositiveButton()
            setNegativeButton()
        }

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val feedUrl = dialogBinding.editFeedUrl.text.toString()
            if (feedUrl.isNotBlank()) {
                val name = dialogBinding.editName.text.toString().ifBlank { null }

                val itemPath = if (feedNode != null) {
                    (feedNode.path + (name ?: feedUrl)).joinToString("\\")
                } else {
                    name ?: feedUrl
                }
                viewModel.addRssFeed(serverId, feedUrl, itemPath)
                dialog.dismiss()
            } else {
                dialogBinding.inputLayoutFeedUrl.error = getString(R.string.rss_field_required)
            }
        }
    }

    private fun showAddFolderDialog(feedNode: RssFeedNode?) {
        lateinit var dialogBinding: DialogRssAddFolderBinding

        val dialog = showDialog(DialogRssAddFolderBinding::inflate) { binding ->
            dialogBinding = binding

            setTitle(R.string.rss_action_add_folder)
            setPositiveButton()
            setNegativeButton()
        }

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val name = dialogBinding.editName.text.toString()
            if (name.isNotBlank()) {
                val itemPath = if (feedNode != null) {
                    (feedNode.path + name).joinToString("\\")
                } else {
                    name
                }
                viewModel.addRssFolder(serverId, itemPath)
                dialog.dismiss()
            } else {
                dialogBinding.inputLayoutName.error = getString(R.string.rss_field_required)
            }
        }
    }

    private fun showLongClickMenu(feedNode: RssFeedNode, rootView: View) {
        val popupMenu = PopupMenu(requireContext(), rootView)
        val menuRes = if (feedNode.isFeed) R.menu.rss_feed_action else R.menu.rss_folder_action
        popupMenu.inflate(menuRes)
        MenuCompat.setGroupDividerEnabled(popupMenu.menu, true)

        if (feedNode.level == 0) {
            popupMenu.menu.setGroupVisible(R.id.group_folder, false)
        }

        popupMenu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.rename -> {
                    showRenameFeedFolderDialog(feedNode)
                }
                R.id.move -> {
                    movingItem = feedNode
                    actionMode = requireActivity().startActionMode(object : ActionMode.Callback {
                        override fun onCreateActionMode(mode: ActionMode, menu: Menu) = true
                        override fun onPrepareActionMode(mode: ActionMode, menu: Menu) = false
                        override fun onActionItemClicked(mode: ActionMode, item: MenuItem) = false
                        override fun onDestroyActionMode(mode: ActionMode) {
                            movingItem = null
                            actionMode = null
                        }
                    })
                    actionMode?.setTitle(R.string.rss_action_move_select_folder)
                }
                R.id.delete -> {
                    showDeleteFeedFolderDialog(feedNode)
                }
                R.id.add_feed -> {
                    showAddFeedDialog(feedNode)
                }
                R.id.add_folder -> {
                    showAddFolderDialog(feedNode)
                }
                else -> return@setOnMenuItemClickListener false
            }
            true
        }

        popupMenu.show()
    }

    private fun showRenameFeedFolderDialog(feedNode: RssFeedNode) {
        lateinit var dialogBinding: DialogRssRenameFeedFolderBinding

        val dialog = showDialog(DialogRssRenameFeedFolderBinding::inflate) { binding ->
            dialogBinding = binding

            binding.inputLayoutName.setTextWithoutAnimation(feedNode.name)

            if (feedNode.isFeed) {
                setTitle(R.string.rss_action_rename_feed)
                binding.inputLayoutName.setHint(R.string.rss_hint_feed_name)
            } else {
                setTitle(R.string.rss_action_rename_folder)
                binding.inputLayoutName.setHint(R.string.rss_hint_folder_name)
            }

            setPositiveButton()
            setNegativeButton()
        }

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val newName = dialogBinding.editName.text.toString()
            if (newName.isNotBlank()) {
                val from = feedNode.path.joinToString("\\")
                val to = (feedNode.path.dropLast(1) + newName).joinToString("\\")

                viewModel.renameItem(serverId, from, to, feedNode.isFeed)
                dialog.dismiss()
            } else {
                dialogBinding.inputLayoutName.error = getString(R.string.rss_field_required)
            }
        }
    }

    private fun showDeleteFeedFolderDialog(feedNode: RssFeedNode) {
        showDialog {
            if (feedNode.isFeed) {
                setTitle(R.string.rss_action_delete_feed)
                setMessage(getString(R.string.rss_confirm_delete_feed, feedNode.name))
            } else {
                setTitle(R.string.rss_action_delete_folder)
                setMessage(getString(R.string.rss_confirm_delete_folder, feedNode.name))
            }
            setPositiveButton { _, _ ->
                viewModel.deleteItem(serverId, feedNode.path.joinToString("\\"), feedNode.isFeed)
            }
            setNegativeButton()
        }
    }

    override fun onResume() {
        super.onResume()
        requireAppCompatActivity().supportActionBar?.setTitle(R.string.rss_title)
    }
}
