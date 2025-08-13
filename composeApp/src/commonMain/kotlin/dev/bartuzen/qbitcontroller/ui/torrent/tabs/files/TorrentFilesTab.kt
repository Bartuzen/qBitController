package dev.bartuzen.qbitcontroller.ui.torrent.tabs.files

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoNotDisturbAlt
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.FlipToBack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.HorizontalRule
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardDoubleArrowUp
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.bartuzen.qbitcontroller.model.TorrentFileNode
import dev.bartuzen.qbitcontroller.model.TorrentFilePriority
import dev.bartuzen.qbitcontroller.ui.components.ActionMenuItem
import dev.bartuzen.qbitcontroller.ui.components.AppBarActions
import dev.bartuzen.qbitcontroller.ui.components.Dialog
import dev.bartuzen.qbitcontroller.ui.components.DropdownMenuItem
import dev.bartuzen.qbitcontroller.ui.components.LazyColumnItemMinHeight
import dev.bartuzen.qbitcontroller.ui.components.PlatformBackHandler
import dev.bartuzen.qbitcontroller.ui.components.PullToRefreshBox
import dev.bartuzen.qbitcontroller.ui.icons.Priority
import dev.bartuzen.qbitcontroller.ui.theme.LocalCustomColors
import dev.bartuzen.qbitcontroller.utils.EventEffect
import dev.bartuzen.qbitcontroller.utils.PersistentLaunchedEffect
import dev.bartuzen.qbitcontroller.utils.floorToDecimal
import dev.bartuzen.qbitcontroller.utils.formatBytes
import dev.bartuzen.qbitcontroller.utils.formatFilePriority
import dev.bartuzen.qbitcontroller.utils.getErrorMessage
import dev.bartuzen.qbitcontroller.utils.getString
import dev.bartuzen.qbitcontroller.utils.jsonSaver
import dev.bartuzen.qbitcontroller.utils.stateListSaver
import dev.bartuzen.qbitcontroller.utils.stringResource
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.pluralStringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import qbitcontroller.composeapp.generated.resources.Res
import qbitcontroller.composeapp.generated.resources.action_select_all
import qbitcontroller.composeapp.generated.resources.action_select_inverse
import qbitcontroller.composeapp.generated.resources.dialog_ok
import qbitcontroller.composeapp.generated.resources.torrent_error_not_found
import qbitcontroller.composeapp.generated.resources.torrent_files_action_priority
import qbitcontroller.composeapp.generated.resources.torrent_files_action_rename
import qbitcontroller.composeapp.generated.resources.torrent_files_details_format
import qbitcontroller.composeapp.generated.resources.torrent_files_error_path_is_invalid_or_in_use
import qbitcontroller.composeapp.generated.resources.torrent_files_file_renamed_success
import qbitcontroller.composeapp.generated.resources.torrent_files_folder_renamed_success
import qbitcontroller.composeapp.generated.resources.torrent_files_priority_do_not_download
import qbitcontroller.composeapp.generated.resources.torrent_files_priority_high
import qbitcontroller.composeapp.generated.resources.torrent_files_priority_maximum
import qbitcontroller.composeapp.generated.resources.torrent_files_priority_mixed
import qbitcontroller.composeapp.generated.resources.torrent_files_priority_normal
import qbitcontroller.composeapp.generated.resources.torrent_files_priority_update_success
import qbitcontroller.composeapp.generated.resources.torrent_files_rename_file
import qbitcontroller.composeapp.generated.resources.torrent_files_rename_file_hint
import qbitcontroller.composeapp.generated.resources.torrent_files_rename_folder
import qbitcontroller.composeapp.generated.resources.torrent_files_rename_folder_hint
import qbitcontroller.composeapp.generated.resources.torrent_files_selected

@Composable
fun TorrentFilesTab(
    serverId: Int,
    torrentHash: String,
    isScreenActive: Boolean,
    snackbarEventFlow: MutableSharedFlow<String>,
    bottomBarStateEventFlow: MutableSharedFlow<Triple<Int, Dp, Boolean>>,
    modifier: Modifier = Modifier,
    viewModel: TorrentFilesViewModel = koinViewModel(parameters = { parametersOf(serverId, torrentHash) }),
) {
    val filesNode by viewModel.filesNode.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val isNaturalLoading by viewModel.isNaturalLoading.collectAsStateWithLifecycle()

    val expandedNodes = rememberSaveable(saver = stateListSaver()) { mutableStateListOf<String>() }
    val selectedFiles = rememberSaveable(saver = stateListSaver()) { mutableStateListOf<String>() }
    var currentDialog by rememberSaveable(stateSaver = jsonSaver()) { mutableStateOf<Dialog?>(null) }

    val flattenedNodes = remember(filesNode, expandedNodes.toList()) {
        filesNode?.let { node ->
            processNodes(node, expandedNodes)
        }
    }

    LaunchedEffect(flattenedNodes?.toList()) {
        selectedFiles.removeAll { path -> flattenedNodes?.find { it.path == path } == null }
    }

    LaunchedEffect(isScreenActive) {
        viewModel.setScreenActive(isScreenActive)
    }

    PlatformBackHandler(enabled = isScreenActive && selectedFiles.isNotEmpty()) {
        selectedFiles.clear()
    }

    EventEffect(viewModel.eventFlow) { event ->
        when (event) {
            is TorrentFilesViewModel.Event.Error -> {
                snackbarEventFlow.emit(getErrorMessage(event.error))
            }
            TorrentFilesViewModel.Event.TorrentNotFound -> {
                snackbarEventFlow.emit(getString(Res.string.torrent_error_not_found))
            }
            TorrentFilesViewModel.Event.PathIsInvalidOrInUse -> {
                snackbarEventFlow.emit(getString(Res.string.torrent_files_error_path_is_invalid_or_in_use))
            }
            TorrentFilesViewModel.Event.FilePriorityUpdated -> {
                snackbarEventFlow.emit(getString(Res.string.torrent_files_priority_update_success))
            }
            TorrentFilesViewModel.Event.FileRenamed -> {
                snackbarEventFlow.emit(getString(Res.string.torrent_files_file_renamed_success))
            }
            TorrentFilesViewModel.Event.FolderRenamed -> {
                snackbarEventFlow.emit(getString(Res.string.torrent_files_folder_renamed_success))
            }
        }
    }

    when (currentDialog) {
        is Dialog.Rename -> {
            val file = remember(filesNode, selectedFiles.toList()) {
                if (selectedFiles.size == 1) {
                    filesNode?.findChildNode(selectedFiles.first())
                } else {
                    null
                }
            }

            LaunchedEffect(file == null) {
                if (file == null) {
                    currentDialog = null
                }
            }

            if (file != null) {
                RenameDialog(
                    initialName = file.name,
                    isFile = file is TorrentFileNode.File,
                    onDismiss = { currentDialog = null },
                    onRename = { newName ->
                        val separator = file.separator
                        val newPath = if (file.path.contains(separator)) {
                            "${file.path.substringBeforeLast(separator)}$separator$newName"
                        } else {
                            newName
                        }
                        when (file) {
                            is TorrentFileNode.File -> viewModel.renameFile(file.path, newPath)
                            is TorrentFileNode.Folder -> viewModel.renameFolder(file.path, newPath)
                        }
                        currentDialog = null
                        selectedFiles.clear()
                    },
                )
            }
        }
        else -> {}
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal),
        bottomBar = {
            var bottomBarHeight by remember { mutableStateOf(0.dp) }
            val visibleState = remember { MutableTransitionState(selectedFiles.isNotEmpty()) }

            LaunchedEffect(selectedFiles.isNotEmpty()) {
                visibleState.targetState = selectedFiles.isNotEmpty()
            }

            LaunchedEffect(visibleState.isIdle, visibleState.currentState) {
                if (visibleState.isIdle && !visibleState.currentState) {
                    bottomBarHeight = 0.dp
                }
            }

            LaunchedEffect(bottomBarHeight, visibleState.isIdle) {
                bottomBarStateEventFlow.emit(
                    Triple(1, bottomBarHeight, !visibleState.isIdle),
                )
            }

            val density = LocalDensity.current
            AnimatedVisibility(
                visibleState = visibleState,
                enter = expandVertically(),
                exit = shrinkVertically(),
                modifier = Modifier.onGloballyPositioned {
                    bottomBarHeight = with(density) { it.size.height.toDp() }
                },
            ) {
                TopAppBar(
                    title = {
                        var selectedSize by rememberSaveable { mutableIntStateOf(0) }

                        LaunchedEffect(selectedFiles.size) {
                            if (selectedFiles.isNotEmpty()) {
                                selectedSize = selectedFiles.size
                            }
                        }

                        Text(
                            text = pluralStringResource(
                                Res.plurals.torrent_files_selected,
                                selectedSize,
                                selectedSize,
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    ),
                    navigationIcon = {
                        IconButton(onClick = { selectedFiles.clear() }) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = null,
                            )
                        }
                    },
                    actions = {
                        var showPriorityMenu by rememberSaveable { mutableStateOf(false) }
                        val actionMenuItems = listOf(
                            ActionMenuItem(
                                title = stringResource(Res.string.torrent_files_action_priority),
                                icon = Icons.Outlined.Priority,
                                onClick = { showPriorityMenu = true },
                                showAsAction = true,
                                dropdownMenu = {
                                    val scrollState = rememberScrollState()
                                    PersistentLaunchedEffect(showPriorityMenu) {
                                        if (showPriorityMenu) {
                                            scrollState.scrollTo(0)
                                        }
                                    }

                                    DropdownMenu(
                                        expanded = showPriorityMenu,
                                        onDismissRequest = { showPriorityMenu = false },
                                        scrollState = scrollState,
                                    ) {
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = stringResource(Res.string.torrent_files_priority_do_not_download),
                                                )
                                            },
                                            leadingIcon = {
                                                Icon(imageVector = Icons.Filled.DoNotDisturbAlt, contentDescription = null)
                                            },
                                            onClick = {
                                                viewModel.setFilePriority(
                                                    filePaths = selectedFiles.toList(),
                                                    priority = TorrentFilePriority.DO_NOT_DOWNLOAD,
                                                )
                                                showPriorityMenu = false
                                                selectedFiles.clear()
                                            },
                                        )
                                        DropdownMenuItem(
                                            text = {
                                                Text(text = stringResource(Res.string.torrent_files_priority_normal))
                                            },
                                            leadingIcon = {
                                                Icon(imageVector = Icons.Filled.HorizontalRule, contentDescription = null)
                                            },
                                            onClick = {
                                                viewModel.setFilePriority(
                                                    filePaths = selectedFiles.toList(),
                                                    priority = TorrentFilePriority.NORMAL,
                                                )
                                                showPriorityMenu = false
                                                selectedFiles.clear()
                                            },
                                        )
                                        DropdownMenuItem(
                                            text = {
                                                Text(text = stringResource(Res.string.torrent_files_priority_high))
                                            },
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = Icons.Filled.KeyboardDoubleArrowUp,
                                                    contentDescription = null,
                                                )
                                            },
                                            onClick = {
                                                viewModel.setFilePriority(
                                                    filePaths = selectedFiles.toList(),
                                                    priority = TorrentFilePriority.HIGH,
                                                )
                                                showPriorityMenu = false
                                                selectedFiles.clear()
                                            },
                                        )
                                        DropdownMenuItem(
                                            text = {
                                                Text(text = stringResource(Res.string.torrent_files_priority_maximum))
                                            },
                                            leadingIcon = {
                                                Icon(imageVector = Icons.Filled.PriorityHigh, contentDescription = null)
                                            },
                                            onClick = {
                                                viewModel.setFilePriority(
                                                    filePaths = selectedFiles.toList(),
                                                    priority = TorrentFilePriority.MAXIMUM,
                                                )
                                                showPriorityMenu = false
                                                selectedFiles.clear()
                                            },
                                        )
                                    }
                                },
                            ),
                            ActionMenuItem(
                                title = stringResource(Res.string.torrent_files_action_rename),
                                icon = Icons.Filled.DriveFileRenameOutline,
                                onClick = { currentDialog = Dialog.Rename },
                                showAsAction = true,
                                isEnabled = selectedFiles.size == 1,
                            ),
                            ActionMenuItem(
                                title = stringResource(Res.string.action_select_all),
                                icon = Icons.Filled.SelectAll,
                                showAsAction = false,
                                onClick = onClick@{
                                    val newFiles = flattenedNodes
                                        ?.filter { it.path !in selectedFiles }
                                        ?.map { it.path }
                                        ?: return@onClick
                                    selectedFiles.addAll(newFiles)
                                },
                            ),
                            ActionMenuItem(
                                title = stringResource(Res.string.action_select_inverse),
                                icon = Icons.Filled.FlipToBack,
                                showAsAction = false,
                                onClick = onClick@{
                                    val newFiles = flattenedNodes
                                        ?.filter { it.path !in selectedFiles }
                                        ?.map { it.path }
                                        ?: return@onClick
                                    selectedFiles.clear()
                                    selectedFiles.addAll(newFiles)
                                },
                            ),
                        )

                        AppBarActions(items = actionMenuItems, bottom = true)
                    },
                    windowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom),
                )
            }
        },
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refreshFiles() },
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .imePadding(),
        ) {
            val listState = rememberLazyListState()
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(
                    items = flattenedNodes ?: emptyList(),
                    key = { it.path },
                ) { node ->
                    FileItem(
                        fileNode = node,
                        selected = node.path in selectedFiles,
                        expanded = node.path in expandedNodes,
                        onClick = {
                            if (selectedFiles.isNotEmpty()) {
                                if (node.path !in selectedFiles) {
                                    selectedFiles += node.path
                                } else {
                                    selectedFiles -= node.path
                                }
                            } else {
                                if (node.path in expandedNodes) {
                                    expandedNodes.remove(node.path)
                                } else {
                                    expandedNodes.add(node.path)
                                }
                            }
                        },
                        onLongClick = {
                            if (node.path !in selectedFiles) {
                                selectedFiles += node.path
                            } else {
                                selectedFiles -= node.path
                            }
                        },
                        onToggleExpand = {
                            if (node.path in expandedNodes) {
                                expandedNodes.remove(node.path)
                            } else {
                                expandedNodes.add(node.path)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateItem(),
                    )
                }

                item {
                    LazyColumnItemMinHeight()
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
                visible = isNaturalLoading == true,
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
private fun FileItem(
    fileNode: TorrentFileNode,
    selected: Boolean,
    expanded: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onToggleExpand: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Unspecified,
        ),
        modifier = modifier.padding(start = ((fileNode.level - 1) * 12).dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick,
                )
                .padding(
                    end = 16.dp,
                    top = 12.dp,
                    bottom = 12.dp,
                ),
        ) {
            if (fileNode is TorrentFileNode.Folder) {
                IconButton(onClick = onToggleExpand) {
                    val rotation by animateFloatAsState(
                        targetValue = if (expanded) 0f else -90f,
                    )
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowDown,
                        contentDescription = null,
                        modifier = Modifier.rotate(rotation),
                    )
                }
            } else {
                Spacer(modifier = Modifier.size(48.dp))
            }

            Icon(
                imageVector = if (fileNode is TorrentFileNode.File) {
                    Icons.AutoMirrored.Filled.InsertDriveFile
                } else {
                    Icons.Filled.Folder
                },
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp),
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = fileNode.name,
                    style = MaterialTheme.typography.bodyLarge,
                )

                val progressColor by animateColorAsState(
                    targetValue = when (fileNode.priority) {
                        TorrentFilePriority.DO_NOT_DOWNLOAD -> LocalCustomColors.current.filePriorityDoNotDownload
                        TorrentFilePriority.NORMAL -> LocalCustomColors.current.filePriorityNormal
                        TorrentFilePriority.HIGH -> LocalCustomColors.current.filePriorityHigh
                        TorrentFilePriority.MAXIMUM -> LocalCustomColors.current.filePriorityMaximum
                        null -> LocalCustomColors.current.filePriorityMixed
                    },
                    animationSpec = tween(),
                )
                val trackColor = progressColor.copy(alpha = 0.38f)

                val progressAnimated by animateFloatAsState(
                    targetValue = fileNode.progress.toFloat(),
                    animationSpec = tween(),
                )
                LinearProgressIndicator(
                    progress = { progressAnimated },
                    color = progressColor,
                    trackColor = trackColor,
                    modifier = Modifier.fillMaxWidth(),
                )

                val priorityText = fileNode.priority?.let { formatFilePriority(it) }
                    ?: stringResource(Res.string.torrent_files_priority_mixed)

                val progressText = if (fileNode.progress < 1) {
                    (fileNode.progress * 100).floorToDecimal(1).toString()
                } else {
                    "100"
                }
                Text(
                    text = stringResource(
                        Res.string.torrent_files_details_format,
                        priorityText,
                        formatBytes(fileNode.downloadedSize),
                        formatBytes(fileNode.size),
                        progressText,
                    ),
                )
            }
        }
    }
}

private fun processNodes(rootNode: TorrentFileNode, expandedNodes: List<String>): List<TorrentFileNode> {
    val result = mutableListOf<TorrentFileNode>()
    val stack = ArrayDeque<TorrentFileNode>()
    stack.add(rootNode)

    while (stack.isNotEmpty()) {
        val node = stack.removeLast()
        result.add(node)

        if ((node.level == 0 || node.path in expandedNodes) && node is TorrentFileNode.Folder) {
            node.children.asReversed().forEach { grandChild ->
                stack.add(grandChild)
            }
        }
    }

    return result.drop(1)
}

@Serializable
private sealed class Dialog {
    @Serializable
    data object Rename : Dialog()
}

@Composable
private fun RenameDialog(
    initialName: String,
    isFile: Boolean,
    onDismiss: () -> Unit,
    onRename: (newName: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var name by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(initialName, TextRange(Int.MAX_VALUE)))
    }

    Dialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isFile) {
                    stringResource(Res.string.torrent_files_rename_file)
                } else {
                    stringResource(Res.string.torrent_files_rename_folder)
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
                onValueChange = { name = it },
                label = {
                    Text(
                        text = if (isFile) {
                            stringResource(Res.string.torrent_files_rename_file_hint)
                        } else {
                            stringResource(Res.string.torrent_files_rename_folder_hint)
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                singleLine = true,
                keyboardActions = KeyboardActions(
                    onDone = { onRename(name.text) },
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
            )
        },
        confirmButton = {
            Button(
                onClick = { onRename(name.text) },
            ) {
                Text(text = stringResource(Res.string.dialog_ok))
            }
        },
    )
}
