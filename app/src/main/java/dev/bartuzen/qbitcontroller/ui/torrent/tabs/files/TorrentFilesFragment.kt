package dev.bartuzen.qbitcontroller.ui.torrent.tabs.files

import android.os.Bundle
import android.view.ActionMode
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.material.color.MaterialColors
import dagger.hilt.android.AndroidEntryPoint
import dev.bartuzen.qbitcontroller.R
import dev.bartuzen.qbitcontroller.model.TorrentFileNode
import dev.bartuzen.qbitcontroller.model.TorrentFilePriority
import dev.bartuzen.qbitcontroller.ui.components.Dialog
import dev.bartuzen.qbitcontroller.ui.theme.AppTheme
import dev.bartuzen.qbitcontroller.ui.theme.LocalCustomColors
import dev.bartuzen.qbitcontroller.utils.EventEffect
import dev.bartuzen.qbitcontroller.utils.PersistentLaunchedEffect
import dev.bartuzen.qbitcontroller.utils.floorToDecimal
import dev.bartuzen.qbitcontroller.utils.formatBytes
import dev.bartuzen.qbitcontroller.utils.formatFilePriority
import dev.bartuzen.qbitcontroller.utils.getErrorMessage
import dev.bartuzen.qbitcontroller.utils.harmonizeWithPrimary
import dev.bartuzen.qbitcontroller.utils.jsonSaver
import dev.bartuzen.qbitcontroller.utils.showSnackbar
import dev.bartuzen.qbitcontroller.utils.stateListSaver
import dev.bartuzen.qbitcontroller.utils.view
import kotlinx.serialization.Serializable

@AndroidEntryPoint
class TorrentFilesFragment() : Fragment(R.layout.fragment_torrent_files) {
    private val serverId get() = arguments?.getInt("serverId", -1).takeIf { it != -1 }!!
    private val torrentHash get() = arguments?.getString("torrentHash")!!

    constructor(serverId: Int, torrentHash: String) : this() {
        arguments = bundleOf(
            "serverId" to serverId,
            "torrentHash" to torrentHash,
        )
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
        ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                AppTheme {
                    var currentLifecycle by remember { mutableStateOf(lifecycle.currentState) }
                    DisposableEffect(Unit) {
                        val observer = LifecycleEventObserver { _, event ->
                            currentLifecycle = event.targetState
                        }
                        lifecycle.addObserver(observer)

                        onDispose {
                            lifecycle.removeObserver(observer)
                        }
                    }

                    TorrentFilesTab(
                        fragment = this@TorrentFilesFragment,
                        serverId = serverId,
                        torrentHash = torrentHash,
                        isScreenActive = currentLifecycle.isAtLeast(Lifecycle.State.RESUMED),
                    )
                }
            }
        }
}

@Composable
private fun TorrentFilesTab(
    fragment: TorrentFilesFragment,
    serverId: Int,
    torrentHash: String,
    isScreenActive: Boolean,
    modifier: Modifier = Modifier,
    viewModel: TorrentFilesViewModel = hiltViewModel(
        creationCallback = { factory: TorrentFilesViewModel.Factory ->
            factory.create(serverId, torrentHash)
        },
    ),
) {
    val activity = fragment.requireActivity()
    val context = LocalContext.current

    val filesNode by viewModel.filesNode.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val isNaturalLoading by viewModel.isNaturalLoading.collectAsStateWithLifecycle()

    val expandedNodes = rememberSaveable(saver = stateListSaver()) { mutableStateListOf<String>() }
    val selectedFiles = rememberSaveable(saver = stateListSaver()) { mutableStateListOf<String>() }
    var actionMode by remember { mutableStateOf<ActionMode?>(null) }
    var currentDialog by rememberSaveable(stateSaver = jsonSaver()) { mutableStateOf<Dialog?>(null) }

    val flattenedNodes = remember(filesNode, expandedNodes.toList()) {
        filesNode?.let { node ->
            processNodes(node, expandedNodes)
        }
    }
    val updatedFlattenedNodes by rememberUpdatedState(flattenedNodes)

    LaunchedEffect(selectedFiles.isNotEmpty()) {
        if (selectedFiles.isNotEmpty()) {
            actionMode = activity.startActionMode(
                object : ActionMode.Callback {
                    override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
                        mode.menuInflater.inflate(R.menu.torrent_files_selection, menu)
                        return true
                    }

                    override fun onPrepareActionMode(mode: ActionMode, menu: Menu) = false

                    override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
                        when (item.itemId) {
                            R.id.menu_priority_do_not_download -> {
                                viewModel.setFilePriority(
                                    filePaths = selectedFiles.toList(),
                                    priority = TorrentFilePriority.DO_NOT_DOWNLOAD,
                                )
                                actionMode?.finish()
                            }
                            R.id.menu_priority_normal -> {
                                viewModel.setFilePriority(
                                    filePaths = selectedFiles.toList(),
                                    priority = TorrentFilePriority.NORMAL,
                                )
                                actionMode?.finish()
                            }
                            R.id.menu_priority_high -> {
                                viewModel.setFilePriority(
                                    filePaths = selectedFiles.toList(),
                                    priority = TorrentFilePriority.HIGH,
                                )
                                actionMode?.finish()
                            }
                            R.id.menu_priority_maximum -> {
                                viewModel.setFilePriority(
                                    filePaths = selectedFiles.toList(),
                                    priority = TorrentFilePriority.MAXIMUM,
                                )
                                actionMode?.finish()
                            }
                            R.id.menu_rename -> {
                                currentDialog = Dialog.Rename
                            }
                            R.id.menu_select_all -> {
                                val newFiles = updatedFlattenedNodes
                                    ?.filter { it.path !in selectedFiles }
                                    ?.map { it.path }
                                    ?: return false
                                selectedFiles.addAll(newFiles)
                            }
                            R.id.menu_select_inverse -> {
                                val newFiles = updatedFlattenedNodes
                                    ?.filter { it.path !in selectedFiles }
                                    ?.map { it.path }
                                    ?: return false
                                selectedFiles.clear()
                                selectedFiles.addAll(newFiles)
                            }
                            else -> return false
                        }

                        return true
                    }

                    override fun onDestroyActionMode(mode: ActionMode) {
                        actionMode = null
                        selectedFiles.clear()
                    }
                },
            )
        } else {
            actionMode?.finish()
        }
    }

    LaunchedEffect(selectedFiles.size == 1) {
        actionMode?.menu?.findItem(R.id.menu_rename)?.isEnabled = selectedFiles.size == 1
    }

    LaunchedEffect(selectedFiles.size) {
        if (selectedFiles.isNotEmpty()) {
            actionMode?.title = context.resources.getQuantityString(
                R.plurals.torrent_files_selected,
                selectedFiles.size,
                selectedFiles.size,
            )
        }
    }

    LaunchedEffect(flattenedNodes?.toList()) {
        selectedFiles.removeAll { path -> flattenedNodes?.find { it.path == path } == null }
    }

    LaunchedEffect(isScreenActive) {
        viewModel.setScreenActive(isScreenActive)
        if (!isScreenActive) {
            actionMode?.finish()
        }
    }

    EventEffect(viewModel.eventFlow) { event ->
        when (event) {
            is TorrentFilesViewModel.Event.Error -> {
                fragment.showSnackbar(getErrorMessage(context, event.error), view = activity.view)
            }
            TorrentFilesViewModel.Event.TorrentNotFound -> {
                fragment.showSnackbar(R.string.torrent_error_not_found, view = activity.view)
            }
            TorrentFilesViewModel.Event.PathIsInvalidOrInUse -> {
                fragment.showSnackbar(R.string.torrent_files_error_path_is_invalid_or_in_use, view = activity.view)
            }
            TorrentFilesViewModel.Event.FilePriorityUpdated -> {
                fragment.showSnackbar(R.string.torrent_files_priority_update_success, view = activity.view)
            }
            TorrentFilesViewModel.Event.FileRenamed -> {
                fragment.showSnackbar(R.string.torrent_files_file_renamed_success, view = activity.view)
            }
            TorrentFilesViewModel.Event.FolderRenamed -> {
                fragment.showSnackbar(R.string.torrent_files_folder_renamed_success, view = activity.view)
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
                        actionMode?.finish()
                    },
                )
            }
        }
        else -> {}
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = { viewModel.refreshFiles() },
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
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
            containerColor = if (selected) MaterialTheme.colorScheme.surfaceContainerHigh else Color.Unspecified,
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
                    targetValue = harmonizeWithPrimary(
                        when (fileNode.priority) {
                            TorrentFilePriority.DO_NOT_DOWNLOAD -> LocalCustomColors.current.filePriorityDoNotDownload
                            TorrentFilePriority.NORMAL -> LocalCustomColors.current.filePriorityNormal
                            TorrentFilePriority.HIGH -> LocalCustomColors.current.filePriorityHigh
                            TorrentFilePriority.MAXIMUM -> LocalCustomColors.current.filePriorityMaximum
                            null -> LocalCustomColors.current.filePriorityMixed
                        },
                    ),
                    animationSpec = tween(),
                )
                val trackColor = progressColor.copy(alpha = MaterialColors.ALPHA_DISABLED)

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
                    ?: stringResource(R.string.torrent_files_priority_mixed)

                val progressText = if (fileNode.progress < 1) {
                    (fileNode.progress * 100).floorToDecimal(1).toString()
                } else {
                    "100"
                }
                Text(
                    text = stringResource(
                        R.string.torrent_files_details_format,
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
                    stringResource(R.string.torrent_files_rename_file)
                } else {
                    stringResource(R.string.torrent_files_rename_folder)
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
                            stringResource(R.string.torrent_files_rename_file_hint)
                        } else {
                            stringResource(R.string.torrent_files_rename_folder_hint)
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
            TextButton(
                onClick = { onRename(name.text) },
            ) {
                Text(text = stringResource(R.string.dialog_ok))
            }
        },
    )
}
