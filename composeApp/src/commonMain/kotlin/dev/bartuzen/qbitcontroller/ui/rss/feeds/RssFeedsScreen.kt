package dev.bartuzen.qbitcontroller.ui.rss.feeds

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardDoubleArrowDown
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RssFeed
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.bartuzen.qbitcontroller.model.RssFeedNode
import dev.bartuzen.qbitcontroller.ui.components.ActionMenuItem
import dev.bartuzen.qbitcontroller.ui.components.AppBarActions
import dev.bartuzen.qbitcontroller.ui.components.Dialog
import dev.bartuzen.qbitcontroller.ui.components.DropdownMenuItem
import dev.bartuzen.qbitcontroller.ui.components.SwipeableSnackbarHost
import dev.bartuzen.qbitcontroller.utils.EventEffect
import dev.bartuzen.qbitcontroller.utils.PersistentLaunchedEffect
import dev.bartuzen.qbitcontroller.utils.getErrorMessage
import dev.bartuzen.qbitcontroller.utils.getString
import dev.bartuzen.qbitcontroller.utils.jsonSaver
import dev.bartuzen.qbitcontroller.utils.rememberReplaceAndApplyStyle
import dev.bartuzen.qbitcontroller.utils.stateListSaver
import dev.bartuzen.qbitcontroller.utils.stringResource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.StringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import qbitcontroller.composeapp.generated.resources.Res
import qbitcontroller.composeapp.generated.resources.dialog_cancel
import qbitcontroller.composeapp.generated.resources.dialog_ok
import qbitcontroller.composeapp.generated.resources.error_required_field
import qbitcontroller.composeapp.generated.resources.rss_action_add
import qbitcontroller.composeapp.generated.resources.rss_action_add_feed
import qbitcontroller.composeapp.generated.resources.rss_action_add_folder
import qbitcontroller.composeapp.generated.resources.rss_action_delete_feed
import qbitcontroller.composeapp.generated.resources.rss_action_delete_folder
import qbitcontroller.composeapp.generated.resources.rss_action_edit_feed_url
import qbitcontroller.composeapp.generated.resources.rss_action_move_feed
import qbitcontroller.composeapp.generated.resources.rss_action_move_folder
import qbitcontroller.composeapp.generated.resources.rss_action_move_select_folder
import qbitcontroller.composeapp.generated.resources.rss_action_refresh_all
import qbitcontroller.composeapp.generated.resources.rss_action_rename_feed
import qbitcontroller.composeapp.generated.resources.rss_action_rename_folder
import qbitcontroller.composeapp.generated.resources.rss_add_feed_error
import qbitcontroller.composeapp.generated.resources.rss_add_folder_error
import qbitcontroller.composeapp.generated.resources.rss_added_feed
import qbitcontroller.composeapp.generated.resources.rss_confirm_delete_feed
import qbitcontroller.composeapp.generated.resources.rss_confirm_delete_folder
import qbitcontroller.composeapp.generated.resources.rss_delete_feed_error
import qbitcontroller.composeapp.generated.resources.rss_delete_folder_error
import qbitcontroller.composeapp.generated.resources.rss_hint_feed_name
import qbitcontroller.composeapp.generated.resources.rss_hint_feed_name_optional
import qbitcontroller.composeapp.generated.resources.rss_hint_feed_url
import qbitcontroller.composeapp.generated.resources.rss_hint_folder_name
import qbitcontroller.composeapp.generated.resources.rss_move_feed_error
import qbitcontroller.composeapp.generated.resources.rss_move_folder_error
import qbitcontroller.composeapp.generated.resources.rss_refresh_all_feeds_success
import qbitcontroller.composeapp.generated.resources.rss_rename_feed_error
import qbitcontroller.composeapp.generated.resources.rss_rename_folder_error
import qbitcontroller.composeapp.generated.resources.rss_rules
import qbitcontroller.composeapp.generated.resources.rss_success_feed_change_url
import qbitcontroller.composeapp.generated.resources.rss_success_feed_delete
import qbitcontroller.composeapp.generated.resources.rss_success_feed_move
import qbitcontroller.composeapp.generated.resources.rss_success_feed_rename
import qbitcontroller.composeapp.generated.resources.rss_success_folder_add
import qbitcontroller.composeapp.generated.resources.rss_success_folder_delete
import qbitcontroller.composeapp.generated.resources.rss_success_folder_move
import qbitcontroller.composeapp.generated.resources.rss_success_folder_rename
import qbitcontroller.composeapp.generated.resources.rss_title

@Composable
fun RssFeedsScreen(
    serverId: Int,
    articleUpdateFlow: Flow<Unit>,
    onNavigateToArticles: (feedPath: List<String>, uid: String?) -> Unit,
    onNavigateToRules: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RssFeedsViewModel = koinViewModel(parameters = { parametersOf(serverId) }),
) {
    val rssFeeds by viewModel.rssFeeds.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var movingItemId by rememberSaveable(stateSaver = jsonSaver()) { mutableStateOf<String?>(null) }
    val expandedNodes = rememberSaveable(saver = stateListSaver()) { mutableStateListOf("0-/") }

    val movingItem by remember(movingItemId, rssFeeds) { mutableStateOf(movingItemId?.let { findNodeById(rssFeeds, it) }) }

    LaunchedEffect(articleUpdateFlow) {
        articleUpdateFlow.collect { article ->
            viewModel.loadRssFeeds()
        }
    }

    EventEffect(viewModel.eventFlow) { event ->
        when (event) {
            is RssFeedsViewModel.Event.Error -> {
                snackbarHostState.currentSnackbarData?.dismiss()
                scope.launch {
                    snackbarHostState.showSnackbar(getErrorMessage(event.error))
                }
            }
            RssFeedsViewModel.Event.AllFeedsRefreshed -> {
                snackbarHostState.currentSnackbarData?.dismiss()
                scope.launch {
                    snackbarHostState.showSnackbar(getString(Res.string.rss_refresh_all_feeds_success))
                }
            }
            RssFeedsViewModel.Event.FeedAddError -> {
                snackbarHostState.currentSnackbarData?.dismiss()
                scope.launch {
                    snackbarHostState.showSnackbar(getString(Res.string.rss_add_feed_error))
                }
            }
            RssFeedsViewModel.Event.FeedRenameError -> {
                snackbarHostState.currentSnackbarData?.dismiss()
                scope.launch {
                    snackbarHostState.showSnackbar(getString(Res.string.rss_rename_feed_error))
                }
            }
            RssFeedsViewModel.Event.FeedMoveError -> {
                snackbarHostState.currentSnackbarData?.dismiss()
                scope.launch {
                    snackbarHostState.showSnackbar(getString(Res.string.rss_move_feed_error))
                }
            }
            RssFeedsViewModel.Event.FeedDeleteError -> {
                snackbarHostState.currentSnackbarData?.dismiss()
                scope.launch {
                    snackbarHostState.showSnackbar(getString(Res.string.rss_delete_feed_error))
                }
            }
            RssFeedsViewModel.Event.FolderAddError -> {
                snackbarHostState.currentSnackbarData?.dismiss()
                scope.launch {
                    snackbarHostState.showSnackbar(getString(Res.string.rss_add_folder_error))
                }
            }
            RssFeedsViewModel.Event.FolderRenameError -> {
                snackbarHostState.currentSnackbarData?.dismiss()
                scope.launch {
                    snackbarHostState.showSnackbar(getString(Res.string.rss_rename_folder_error))
                }
            }
            RssFeedsViewModel.Event.FolderMoveError -> {
                snackbarHostState.currentSnackbarData?.dismiss()
                scope.launch {
                    snackbarHostState.showSnackbar(getString(Res.string.rss_move_folder_error))
                }
            }
            RssFeedsViewModel.Event.FolderDeleteError -> {
                snackbarHostState.currentSnackbarData?.dismiss()
                scope.launch {
                    snackbarHostState.showSnackbar(getString(Res.string.rss_delete_folder_error))
                }
            }
            RssFeedsViewModel.Event.FeedAdded -> {
                snackbarHostState.currentSnackbarData?.dismiss()
                scope.launch {
                    snackbarHostState.showSnackbar(getString(Res.string.rss_added_feed))
                }
            }
            RssFeedsViewModel.Event.FeedRenamed -> {
                snackbarHostState.currentSnackbarData?.dismiss()
                scope.launch {
                    snackbarHostState.showSnackbar(getString(Res.string.rss_success_feed_rename))
                }
            }
            RssFeedsViewModel.Event.FeedUrlChanged -> {
                snackbarHostState.currentSnackbarData?.dismiss()
                scope.launch {
                    snackbarHostState.showSnackbar(getString(Res.string.rss_success_feed_change_url))
                }
            }
            RssFeedsViewModel.Event.FeedMoved -> {
                snackbarHostState.currentSnackbarData?.dismiss()
                scope.launch {
                    snackbarHostState.showSnackbar(getString(Res.string.rss_success_feed_move))
                }
            }
            RssFeedsViewModel.Event.FeedDeleted -> {
                snackbarHostState.currentSnackbarData?.dismiss()
                scope.launch {
                    snackbarHostState.showSnackbar(getString(Res.string.rss_success_feed_delete))
                }
            }
            RssFeedsViewModel.Event.FolderAdded -> {
                snackbarHostState.currentSnackbarData?.dismiss()
                scope.launch {
                    snackbarHostState.showSnackbar(getString(Res.string.rss_success_folder_add))
                }
            }
            RssFeedsViewModel.Event.FolderRenamed -> {
                snackbarHostState.currentSnackbarData?.dismiss()
                scope.launch {
                    snackbarHostState.showSnackbar(getString(Res.string.rss_success_folder_rename))
                }
            }
            RssFeedsViewModel.Event.FolderMoved -> {
                snackbarHostState.currentSnackbarData?.dismiss()
                scope.launch {
                    snackbarHostState.showSnackbar(getString(Res.string.rss_success_folder_move))
                }
            }
            RssFeedsViewModel.Event.FolderDeleted -> {
                snackbarHostState.currentSnackbarData?.dismiss()
                scope.launch {
                    snackbarHostState.showSnackbar(getString(Res.string.rss_success_folder_delete))
                }
            }
        }
    }

    var currentDialog by rememberSaveable(stateSaver = jsonSaver()) { mutableStateOf<Dialog?>(null) }
    when (val dialog = currentDialog) {
        is Dialog.AddFeed -> {
            val parentNode = remember(dialog.parentId, rssFeeds) {
                if (dialog.parentId == null) null else findNodeById(rssFeeds, dialog.parentId)
            }

            LaunchedEffect(dialog.parentId != null && parentNode == null) {
                if (dialog.parentId != null && parentNode == null) {
                    currentDialog = null
                }
            }

            AddFeedDialog(
                onDismiss = { currentDialog = null },
                onAddFeed = { url, name ->
                    val itemPath = if (parentNode != null) {
                        (parentNode.path + (name ?: url)).joinToString("\\")
                    } else {
                        name ?: url
                    }
                    viewModel.addRssFeed(url, itemPath)
                    currentDialog = null
                },
            )
        }
        is Dialog.AddFolder -> {
            val parentNode = remember(dialog.parentId, rssFeeds) {
                if (dialog.parentId == null) null else findNodeById(rssFeeds, dialog.parentId)
            }

            LaunchedEffect(dialog.parentId != null && parentNode == null) {
                if (dialog.parentId != null && parentNode == null) {
                    currentDialog = null
                }
            }

            AddFolderDialog(
                onDismiss = { currentDialog = null },
                onAddFolder = { name ->
                    val itemPath = if (parentNode != null) {
                        (parentNode.path + name).joinToString("\\")
                    } else {
                        name
                    }
                    viewModel.addRssFolder(itemPath)
                    currentDialog = null
                },
            )
        }
        is Dialog.RenameFeedFolder -> {
            val node = remember(dialog.nodeId, rssFeeds) {
                findNodeById(rssFeeds, dialog.nodeId)
            }

            LaunchedEffect(node == null) {
                if (node == null) {
                    currentDialog = null
                }
            }

            if (node != null) {
                RenameFeedFolderDialog(
                    node = node,
                    onDismiss = { currentDialog = null },
                    onRename = { newName ->
                        val from = node.path.joinToString("\\")
                        val to = (node.path.dropLast(1) + newName).joinToString("\\")
                        viewModel.renameItem(from, to, node.isFeed)
                        currentDialog = null
                    },
                )
            }
        }
        is Dialog.DeleteFeedFolder -> {
            val node = remember(dialog.nodeId, rssFeeds) {
                findNodeById(rssFeeds, dialog.nodeId)
            }

            LaunchedEffect(node == null) {
                if (node == null) {
                    currentDialog = null
                }
            }

            if (node != null) {
                DeleteFeedFolderDialog(
                    node = node,
                    onDismiss = { currentDialog = null },
                    onDelete = {
                        viewModel.deleteItem(node.path.joinToString("\\"), node.isFeed)
                        currentDialog = null
                    },
                )
            }
        }
        is Dialog.EditFeedUrl -> {
            val node = remember(dialog.nodeId, rssFeeds) {
                findNodeById(rssFeeds, dialog.nodeId)
            }

            LaunchedEffect(node == null) {
                if (node == null) {
                    currentDialog = null
                }
            }

            if (node != null && node.feed?.url != null) {
                EditFeedUrlDialog(
                    node = node,
                    onDismiss = { currentDialog = null },
                    onEditUrl = { newUrl ->
                        val path = node.path.joinToString("\\")
                        viewModel.setFeedUrl(path, newUrl)
                        currentDialog = null
                    },
                )
            }
        }
        else -> {}
    }

    LaunchedEffect(movingItemId, rssFeeds) {
        movingItemId?.let { nodeId ->
            if (findNodeById(rssFeeds, nodeId) == null) {
                movingItemId = null
            }
        }
    }

    BackHandler(enabled = movingItemId != null) {
        movingItemId = null
    }

    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets.safeDrawing.only(
            WindowInsetsSides.Horizontal + WindowInsetsSides.Top,
        ),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(Res.string.rss_title),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                actions = {
                    var showAddMenu by rememberSaveable { mutableStateOf(false) }

                    val actionMenuItems = listOf(
                        ActionMenuItem(
                            title = stringResource(Res.string.rss_action_add),
                            icon = Icons.Filled.Add,
                            onClick = { showAddMenu = true },
                            showAsAction = true,
                            dropdownMenu = {
                                val scrollState = rememberScrollState()
                                PersistentLaunchedEffect(showAddMenu) {
                                    if (showAddMenu) {
                                        scrollState.scrollTo(0)
                                    }
                                }

                                DropdownMenu(
                                    expanded = showAddMenu,
                                    onDismissRequest = { showAddMenu = false },
                                    scrollState = scrollState,
                                ) {
                                    Text(
                                        text = stringResource(Res.string.rss_action_add),
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                    )

                                    DropdownMenuItem(
                                        text = {
                                            Text(text = stringResource(Res.string.rss_action_add_feed))
                                        },
                                        leadingIcon = {
                                            Icon(imageVector = Icons.Filled.RssFeed, contentDescription = null)
                                        },
                                        onClick = {
                                            currentDialog = Dialog.AddFeed(null)
                                            showAddMenu = false
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = {
                                            Text(text = stringResource(Res.string.rss_action_add_folder))
                                        },
                                        leadingIcon = {
                                            Icon(imageVector = Icons.Filled.Folder, contentDescription = null)
                                        },
                                        onClick = {
                                            currentDialog = Dialog.AddFolder(null)
                                            showAddMenu = false
                                        },
                                    )
                                }
                            },
                        ),
                        ActionMenuItem(
                            title = stringResource(Res.string.rss_action_refresh_all),
                            icon = Icons.Filled.Refresh,
                            onClick = { viewModel.refreshAllFeeds() },
                            showAsAction = true,
                        ),
                        ActionMenuItem(
                            title = stringResource(Res.string.rss_rules),
                            icon = Icons.Filled.KeyboardDoubleArrowDown,
                            onClick = onNavigateToRules,
                            showAsAction = true,
                        ),
                    )

                    AppBarActions(items = actionMenuItems)
                },
                windowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top),
            )

            AnimatedVisibility(
                visible = movingItemId != null,
                enter = expandVertically(),
                exit = shrinkVertically(),
            ) {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(Res.string.rss_action_move_select_folder),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = { movingItemId = null },
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = null,
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    ),
                    windowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top),
                )
            }
        },
        snackbarHost = {
            SwipeableSnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom)),
            )
        },
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refreshRssFeeds() },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .imePadding(),
        ) {
            val flattenedNodes = remember(rssFeeds, expandedNodes.toList()) {
                rssFeeds?.let { node ->
                    processNodes(node, expandedNodes)
                }
            }

            val listState = rememberLazyListState()
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(
                    items = flattenedNodes ?: emptyList(),
                    key = { it.uniqueId },
                ) { node ->
                    FeedItem(
                        feedNode = node,
                        isExpanded = node.uniqueId in expandedNodes,
                        isMoving = movingItemId == node.uniqueId,
                        onClick = {
                            if (movingItem == null) {
                                onNavigateToArticles(node.path, node.feed?.uid)
                            } else if (node.isFolder) {
                                movingItem?.let { item ->
                                    val from = item.path.joinToString("\\")
                                    val to = (node.path + item.name).joinToString("\\")
                                    viewModel.moveItem(from, to, item.isFeed)
                                    movingItemId = null
                                }
                            }
                        },
                        onToggleExpand = {
                            if (node.uniqueId in expandedNodes) {
                                expandedNodes.remove(node.uniqueId)
                            } else {
                                expandedNodes.add(node.uniqueId)
                            }
                        },
                        onRename = { currentDialog = Dialog.RenameFeedFolder(node.uniqueId) },
                        onEditUrl = { currentDialog = Dialog.EditFeedUrl(node.uniqueId) },
                        onMove = { movingItemId = node.uniqueId },
                        onDelete = { currentDialog = Dialog.DeleteFeedFolder(node.uniqueId) },
                        onAddFeed = { currentDialog = Dialog.AddFeed(node.uniqueId) },
                        onAddFolder = { currentDialog = Dialog.AddFolder(node.uniqueId) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateItem(),
                    )
                }

                item {
                    Spacer(modifier = Modifier.windowInsetsBottomHeight(WindowInsets.safeDrawing))
                }
            }

            SideEffect {
                if (!listState.isScrollInProgress) {
                    listState.requestScrollToItem(
                        index = listState.firstVisibleItemIndex,
                        scrollOffset = listState.firstVisibleItemScrollOffset,
                    )
                }
            }

            AnimatedVisibility(
                visible = isLoading,
                enter = expandVertically(tween(durationMillis = 500)),
                exit = shrinkVertically(tween(durationMillis = 500)),
            ) {
                LinearProgressIndicator(
                    strokeCap = StrokeCap.Butt,
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter),
                )
            }
        }
    }
}

@Composable
private fun FeedItem(
    feedNode: RssFeedNode,
    isExpanded: Boolean,
    isMoving: Boolean,
    onClick: () -> Unit,
    onToggleExpand: () -> Unit,
    onRename: () -> Unit,
    onEditUrl: () -> Unit,
    onMove: () -> Unit,
    onDelete: () -> Unit,
    onAddFeed: () -> Unit,
    onAddFolder: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val containerColor by animateColorAsState(
        targetValue = if (isMoving) {
            MaterialTheme.colorScheme.surfaceContainerHighest
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
    )
    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(containerColor = containerColor),
        onClick = onClick,
        modifier = modifier.padding(start = (feedNode.level * 12).dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    end = 8.dp,
                    top = 12.dp,
                    bottom = 12.dp,
                ),
        ) {
            if (feedNode.children?.isNotEmpty() == true) {
                IconButton(onClick = onToggleExpand) {
                    val rotation by animateFloatAsState(
                        targetValue = if (isExpanded) 0f else -90f,
                    )
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowDown,
                        contentDescription = null,
                        modifier = Modifier.rotate(rotation),
                    )
                }
            } else {
                Spacer(modifier = Modifier.width(48.dp))
            }

            Icon(
                imageVector = if (feedNode.isFeed) Icons.Filled.RssFeed else Icons.Default.Folder,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp),
            )

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = feedNode.name,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )

            Box {
                var showMenu by rememberSaveable { mutableStateOf(false) }
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = null,
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                ) {
                    if (feedNode.level > 0) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = if (feedNode.isFeed) {
                                        stringResource(Res.string.rss_action_rename_feed)
                                    } else {
                                        stringResource(Res.string.rss_action_rename_folder)
                                    },
                                )
                            },
                            leadingIcon = { Icon(imageVector = Icons.Default.Edit, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                onRename()
                            },
                        )

                        if (feedNode.isFeed) {
                            DropdownMenuItem(
                                text = { Text(text = stringResource(Res.string.rss_action_edit_feed_url)) },
                                leadingIcon = { Icon(imageVector = Icons.Default.Link, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    onEditUrl()
                                },
                            )
                        }

                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = stringResource(
                                        if (feedNode.isFeed) {
                                            Res.string.rss_action_move_feed
                                        } else {
                                            Res.string.rss_action_move_folder
                                        },
                                    ),
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.DriveFileMove,
                                    contentDescription = null,
                                )
                            },
                            onClick = {
                                showMenu = false
                                onMove()
                            },
                        )

                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = stringResource(
                                        if (feedNode.isFeed) {
                                            Res.string.rss_action_delete_feed
                                        } else {
                                            Res.string.rss_action_delete_folder
                                        },
                                    ),
                                )
                            },
                            leadingIcon = { Icon(imageVector = Icons.Default.Delete, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                onDelete()
                            },
                        )

                        if (feedNode.isFolder) {
                            HorizontalDivider()
                        }
                    }

                    if (feedNode.isFolder) {
                        DropdownMenuItem(
                            text = { Text(text = stringResource(Res.string.rss_action_add_feed)) },
                            leadingIcon = { Icon(imageVector = Icons.Filled.RssFeed, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                onAddFeed()
                            },
                        )

                        DropdownMenuItem(
                            text = { Text(text = stringResource(Res.string.rss_action_add_folder)) },
                            leadingIcon = { Icon(imageVector = Icons.Default.Folder, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                onAddFolder()
                            },
                        )
                    }
                }
            }
        }
    }
}

@Serializable
private sealed class Dialog {
    @Serializable
    data class AddFeed(val parentId: String?) : Dialog()

    @Serializable
    data class AddFolder(val parentId: String?) : Dialog()

    @Serializable
    data class RenameFeedFolder(val nodeId: String) : Dialog()

    @Serializable
    data class DeleteFeedFolder(val nodeId: String) : Dialog()

    @Serializable
    data class EditFeedUrl(val nodeId: String) : Dialog()
}

@Composable
private fun AddFeedDialog(
    onDismiss: () -> Unit,
    onAddFeed: (url: String, name: String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var feedUrl by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue()) }
    var feedName by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue()) }
    var urlError by rememberSaveable { mutableStateOf<StringResource?>(null) }

    Dialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(Res.string.rss_action_add_feed)) },
        text = {
            val focusRequester = remember { FocusRequester() }
            PersistentLaunchedEffect {
                focusRequester.requestFocus()
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = feedUrl,
                    onValueChange = {
                        if (it.text != feedUrl.text) {
                            urlError = null
                        }
                        feedUrl = it
                    },
                    label = {
                        Text(
                            text = stringResource(Res.string.rss_hint_feed_url),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    singleLine = true,
                    isError = urlError != null,
                    supportingText = urlError?.let { { Text(text = stringResource(it)) } },
                    trailingIcon = urlError?.let { { Icon(imageVector = Icons.Filled.Error, contentDescription = null) } },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Next,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize()
                        .focusRequester(focusRequester),
                )

                OutlinedTextField(
                    value = feedName,
                    onValueChange = { feedName = it },
                    label = {
                        Text(
                            text = stringResource(Res.string.rss_hint_feed_name_optional),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    singleLine = true,
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (feedUrl.text.isNotBlank()) {
                                onAddFeed(feedUrl.text, feedName.text.ifBlank { null })
                            } else {
                                urlError = Res.string.error_required_field
                            }
                        },
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (feedUrl.text.isNotBlank()) {
                        onAddFeed(feedUrl.text, feedName.text.ifBlank { null })
                    } else {
                        urlError = Res.string.error_required_field
                    }
                },
            ) {
                Text(text = stringResource(Res.string.dialog_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(Res.string.dialog_cancel))
            }
        },
    )
}

@Composable
private fun AddFolderDialog(onDismiss: () -> Unit, onAddFolder: (name: String) -> Unit, modifier: Modifier = Modifier) {
    var folderName by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue()) }
    var nameError by rememberSaveable { mutableStateOf<StringResource?>(null) }

    Dialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(Res.string.rss_action_add_folder)) },
        text = {
            val focusRequester = remember { FocusRequester() }
            PersistentLaunchedEffect {
                focusRequester.requestFocus()
            }

            OutlinedTextField(
                value = folderName,
                onValueChange = {
                    if (it.text != folderName.text) {
                        nameError = null
                    }
                    folderName = it
                },
                label = {
                    Text(
                        text = stringResource(Res.string.rss_hint_folder_name),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                singleLine = true,
                isError = nameError != null,
                supportingText = nameError?.let { { Text(text = stringResource(it)) } },
                trailingIcon = nameError?.let { { Icon(imageVector = Icons.Filled.Error, contentDescription = null) } },
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (folderName.text.isNotBlank()) {
                            onAddFolder(folderName.text)
                        } else {
                            nameError = Res.string.error_required_field
                        }
                    },
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize()
                    .focusRequester(focusRequester),
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    if (folderName.text.isNotBlank()) {
                        onAddFolder(folderName.text)
                    } else {
                        nameError = Res.string.error_required_field
                    }
                },
            ) {
                Text(text = stringResource(Res.string.dialog_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(Res.string.dialog_cancel))
            }
        },
    )
}

@Composable
private fun RenameFeedFolderDialog(
    node: RssFeedNode,
    onDismiss: () -> Unit,
    onRename: (newName: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var name by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(node.name, TextRange(Int.MAX_VALUE)))
    }
    var nameError by rememberSaveable { mutableStateOf<StringResource?>(null) }

    Dialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (node.isFeed) {
                    stringResource(Res.string.rss_action_rename_feed)
                } else {
                    stringResource(Res.string.rss_action_rename_folder)
                },
            )
        },
        text = {
            val focusRequester = remember { FocusRequester() }
            PersistentLaunchedEffect {
                focusRequester.requestFocus()
            }

            OutlinedTextField(
                value = name,
                onValueChange = {
                    if (it.text != name.text) {
                        nameError = null
                    }
                    name = it
                },
                label = {
                    Text(
                        text = if (node.isFeed) {
                            stringResource(Res.string.rss_hint_feed_name)
                        } else {
                            stringResource(Res.string.rss_hint_folder_name)
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                singleLine = true,
                isError = nameError != null,
                supportingText = nameError?.let { { Text(text = stringResource(it)) } },
                trailingIcon = nameError?.let { { Icon(imageVector = Icons.Filled.Error, contentDescription = null) } },
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (name.text.isNotBlank()) {
                            onRename(name.text)
                        } else {
                            nameError = Res.string.error_required_field
                        }
                    },
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize()
                    .focusRequester(focusRequester),
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.text.isNotBlank()) {
                        onRename(name.text)
                    } else {
                        nameError = Res.string.error_required_field
                    }
                },
            ) {
                Text(text = stringResource(Res.string.dialog_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(Res.string.dialog_cancel))
            }
        },
    )
}

@Composable
private fun DeleteFeedFolderDialog(
    node: RssFeedNode,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Dialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (node.isFeed) {
                    stringResource(Res.string.rss_action_delete_feed)
                } else {
                    stringResource(Res.string.rss_action_delete_folder)
                },
            )
        },
        text = {
            Text(
                text = rememberReplaceAndApplyStyle(
                    text = if (node.isFeed) {
                        stringResource(Res.string.rss_confirm_delete_feed)
                    } else {
                        stringResource(Res.string.rss_confirm_delete_folder)
                    },
                    oldValue = "%1\$s",
                    newValue = node.name,
                    style = SpanStyle(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold),
                ),

            )
        },
        confirmButton = {
            Button(onClick = onDelete) {
                Text(text = stringResource(Res.string.dialog_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(Res.string.dialog_cancel))
            }
        },
    )
}

@Composable
private fun EditFeedUrlDialog(
    node: RssFeedNode,
    onDismiss: () -> Unit,
    onEditUrl: (newUrl: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var url by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(node.feed?.url ?: "", TextRange(Int.MAX_VALUE)))
    }
    var urlError by rememberSaveable { mutableStateOf<StringResource?>(null) }

    Dialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(Res.string.rss_action_edit_feed_url)) },
        text = {
            val focusRequester = remember { FocusRequester() }
            PersistentLaunchedEffect {
                focusRequester.requestFocus()
            }

            OutlinedTextField(
                value = url,
                onValueChange = {
                    if (it.text != url.text) {
                        urlError = null
                    }
                    url = it
                },
                label = {
                    Text(
                        text = stringResource(Res.string.rss_hint_feed_url),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                singleLine = true,
                isError = urlError != null,
                supportingText = urlError?.let { { Text(text = stringResource(it)) } },
                trailingIcon = urlError?.let { { Icon(imageVector = Icons.Filled.Error, contentDescription = null) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (url.text.isNotBlank()) {
                            onEditUrl(url.text)
                        } else {
                            urlError = Res.string.error_required_field
                        }
                    },
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize()
                    .focusRequester(focusRequester),
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    if (url.text.isNotBlank()) {
                        onEditUrl(url.text)
                    } else {
                        urlError = Res.string.error_required_field
                    }
                },
            ) {
                Text(text = stringResource(Res.string.dialog_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(Res.string.dialog_cancel))
            }
        },
    )
}

private fun processNodes(rootNode: RssFeedNode, expandedNodes: List<String>): List<RssFeedNode> {
    val result = mutableListOf<RssFeedNode>()
    val stack = ArrayDeque<RssFeedNode>()
    stack.add(rootNode)

    while (stack.isNotEmpty()) {
        val node = stack.removeLast()
        result.add(node)

        if (node.uniqueId in expandedNodes && node.children != null) {
            node.children.asReversed().forEach { grandChild ->
                stack.add(grandChild)
            }
        }
    }

    return result
}

private fun findNodeById(rootNode: RssFeedNode?, nodeId: String): RssFeedNode? {
    if (rootNode == null) {
        return null
    }

    val queue = ArrayDeque<RssFeedNode>()
    queue.add(rootNode)

    while (queue.isNotEmpty()) {
        val node = queue.removeFirst()
        if (node.uniqueId == nodeId) {
            return node
        }

        node.children?.forEach { child ->
            queue.add(child)
        }
    }

    return null
}
