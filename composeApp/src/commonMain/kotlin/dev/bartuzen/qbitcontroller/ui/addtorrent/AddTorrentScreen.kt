package dev.bartuzen.qbitcontroller.ui.addtorrent

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.outlined.Error
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.bartuzen.qbitcontroller.ui.components.ActionMenuItem
import dev.bartuzen.qbitcontroller.ui.components.AppBarActions
import dev.bartuzen.qbitcontroller.ui.components.CategoryChip
import dev.bartuzen.qbitcontroller.ui.components.CheckboxWithLabel
import dev.bartuzen.qbitcontroller.ui.components.DropdownMenuItem
import dev.bartuzen.qbitcontroller.ui.components.PullToRefreshBox
import dev.bartuzen.qbitcontroller.ui.components.SwipeableSnackbarHost
import dev.bartuzen.qbitcontroller.ui.components.TagChip
import dev.bartuzen.qbitcontroller.utils.EventEffect
import dev.bartuzen.qbitcontroller.utils.PersistentLaunchedEffect
import dev.bartuzen.qbitcontroller.utils.PlatformFileSerializer
import dev.bartuzen.qbitcontroller.utils.getDecimalSeparator
import dev.bartuzen.qbitcontroller.utils.getErrorMessage
import dev.bartuzen.qbitcontroller.utils.getString
import dev.bartuzen.qbitcontroller.utils.jsonSaver
import dev.bartuzen.qbitcontroller.utils.measureTextWidth
import dev.bartuzen.qbitcontroller.utils.stateListSaver
import dev.bartuzen.qbitcontroller.utils.stringResource
import dev.bartuzen.qbitcontroller.utils.stringResourceSaver
import dev.bartuzen.qbitcontroller.utils.toPlatformFile
import dev.bartuzen.qbitcontroller.utils.topAppBarColors
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.FileKitMode
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.openFilePicker
import io.github.vinceglb.filekit.name
import kotlinx.coroutines.launch
import kotlinx.serialization.builtins.ListSerializer
import org.jetbrains.compose.resources.pluralStringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import qbitcontroller.composeapp.generated.resources.Res
import qbitcontroller.composeapp.generated.resources.error_unknown
import qbitcontroller.composeapp.generated.resources.speed_kibibytes_per_second
import qbitcontroller.composeapp.generated.resources.speed_mebibytes_per_second
import qbitcontroller.composeapp.generated.resources.torrent_add_action_add_torrent
import qbitcontroller.composeapp.generated.resources.torrent_add_button_file
import qbitcontroller.composeapp.generated.resources.torrent_add_button_url
import qbitcontroller.composeapp.generated.resources.torrent_add_category
import qbitcontroller.composeapp.generated.resources.torrent_add_click_to_select_file
import qbitcontroller.composeapp.generated.resources.torrent_add_content_layout
import qbitcontroller.composeapp.generated.resources.torrent_add_content_layout_no_subfolder
import qbitcontroller.composeapp.generated.resources.torrent_add_content_layout_original
import qbitcontroller.composeapp.generated.resources.torrent_add_content_layout_subfolder
import qbitcontroller.composeapp.generated.resources.torrent_add_default
import qbitcontroller.composeapp.generated.resources.torrent_add_download_speed_limit
import qbitcontroller.composeapp.generated.resources.torrent_add_error
import qbitcontroller.composeapp.generated.resources.torrent_add_file_not_found
import qbitcontroller.composeapp.generated.resources.torrent_add_files_selected
import qbitcontroller.composeapp.generated.resources.torrent_add_invalid_file
import qbitcontroller.composeapp.generated.resources.torrent_add_link_cannot_be_empty
import qbitcontroller.composeapp.generated.resources.torrent_add_link_hint
import qbitcontroller.composeapp.generated.resources.torrent_add_name
import qbitcontroller.composeapp.generated.resources.torrent_add_prioritize_first_last_piece
import qbitcontroller.composeapp.generated.resources.torrent_add_ratio_limit
import qbitcontroller.composeapp.generated.resources.torrent_add_save_path
import qbitcontroller.composeapp.generated.resources.torrent_add_seeding_time_limit
import qbitcontroller.composeapp.generated.resources.torrent_add_sequential_download
import qbitcontroller.composeapp.generated.resources.torrent_add_server
import qbitcontroller.composeapp.generated.resources.torrent_add_skip_hash_checking
import qbitcontroller.composeapp.generated.resources.torrent_add_speed_limit_too_big
import qbitcontroller.composeapp.generated.resources.torrent_add_start_torrent
import qbitcontroller.composeapp.generated.resources.torrent_add_stop_condition
import qbitcontroller.composeapp.generated.resources.torrent_add_stop_condition_files_checked
import qbitcontroller.composeapp.generated.resources.torrent_add_stop_condition_metadata_received
import qbitcontroller.composeapp.generated.resources.torrent_add_stop_condition_none
import qbitcontroller.composeapp.generated.resources.torrent_add_tags
import qbitcontroller.composeapp.generated.resources.torrent_add_title
import qbitcontroller.composeapp.generated.resources.torrent_add_torrent_management_mode
import qbitcontroller.composeapp.generated.resources.torrent_add_torrent_management_mode_auto
import qbitcontroller.composeapp.generated.resources.torrent_add_torrent_management_mode_manual
import qbitcontroller.composeapp.generated.resources.torrent_add_upload_speed_limit
import qbitcontroller.composeapp.generated.resources.torrent_no_categories
import qbitcontroller.composeapp.generated.resources.torrent_no_tags

object AddTorrentKeys {
    const val TorrentAdded = "addTorrent.torrentAdded"
}

@Composable
fun AddTorrentScreen(
    initialServerId: Int?,
    torrentUrl: String?,
    torrentFileUris: List<String>?,
    onNavigateBack: () -> Unit,
    onAddTorrent: (serverId: Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AddTorrentViewModel = koinViewModel(parameters = { parametersOf(initialServerId) }),
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var isUrlMode by rememberSaveable { mutableStateOf(torrentFileUris == null) }

    var torrentLinkText by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(torrentUrl ?: ""))
    }
    var torrentLinkError by rememberSaveable(
        stateSaver = stringResourceSaver(Res.string.torrent_add_link_cannot_be_empty),
    ) { mutableStateOf(null) }

    var torrentFiles by rememberSaveable(stateSaver = jsonSaver(serializer = ListSerializer(PlatformFileSerializer))) {
        mutableStateOf(torrentFileUris?.map { it.toPlatformFile() } ?: emptyList())
    }
    var torrentFileError by rememberSaveable { mutableStateOf(false) }

    var selectedCategory by rememberSaveable { mutableStateOf<String?>(null) }
    val selectedTags = rememberSaveable(saver = stateListSaver()) { mutableStateListOf<String>() }

    var startTorrent by rememberSaveable { mutableStateOf(true) }
    var skipChecking by rememberSaveable { mutableStateOf(false) }
    var sequentialDownload by rememberSaveable { mutableStateOf(false) }
    var prioritizeFirstLastPiece by rememberSaveable { mutableStateOf(false) }

    var autoTmmIndex by rememberSaveable { mutableIntStateOf(0) }
    var stopConditionIndex by rememberSaveable { mutableIntStateOf(0) }
    var contentLayoutIndex by rememberSaveable { mutableIntStateOf(0) }

    var savePath by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue()) }
    var torrentName by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue()) }
    var ratioLimit by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue()) }
    var seedingTimeLimit by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue()) }
    var uploadSpeedLimit by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue()) }
    var downloadSpeedLimit by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue()) }
    var upSpeedLimitUnit by rememberSaveable { mutableIntStateOf(0) }
    var dlSpeedLimitUnit by rememberSaveable { mutableIntStateOf(0) }

    var uploadSpeedError by rememberSaveable(
        stateSaver = stringResourceSaver(Res.string.torrent_add_speed_limit_too_big),
    ) { mutableStateOf(null) }
    var downloadSpeedError by rememberSaveable(
        stateSaver = stringResourceSaver(Res.string.torrent_add_speed_limit_too_big),
    ) { mutableStateOf(null) }

    val serverId by viewModel.serverId.collectAsStateWithLifecycle()

    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val serverData by viewModel.serverData.collectAsStateWithLifecycle()

    EventEffect(viewModel.eventFlow) { event ->
        when (event) {
            is AddTorrentViewModel.Event.Error -> {
                snackbarHostState.currentSnackbarData?.dismiss()
                scope.launch {
                    snackbarHostState.showSnackbar(getErrorMessage(event.error))
                }
            }
            AddTorrentViewModel.Event.FileNotFound -> {
                snackbarHostState.currentSnackbarData?.dismiss()
                scope.launch {
                    snackbarHostState.showSnackbar(getString(Res.string.torrent_add_file_not_found))
                }
            }
            is AddTorrentViewModel.Event.FileReadError -> {
                snackbarHostState.currentSnackbarData?.dismiss()
                scope.launch {
                    snackbarHostState.showSnackbar(getString(Res.string.error_unknown, event.error))
                }
            }
            AddTorrentViewModel.Event.TorrentAddError -> {
                snackbarHostState.currentSnackbarData?.dismiss()
                scope.launch {
                    snackbarHostState.showSnackbar(getString(Res.string.torrent_add_error))
                }
            }
            AddTorrentViewModel.Event.InvalidTorrentFile -> {
                snackbarHostState.currentSnackbarData?.dismiss()
                scope.launch {
                    snackbarHostState.showSnackbar(getString(Res.string.torrent_add_invalid_file))
                }
            }
            is AddTorrentViewModel.Event.TorrentAdded -> {
                onAddTorrent(event.serverId)
            }
            AddTorrentViewModel.Event.NoServersFound -> {
                onNavigateBack()
            }
        }
    }

    LaunchedEffect(Unit) {
        val separatorIndex = ratioLimit.text.indexOfFirst { !it.isDigit() }
        if (separatorIndex != -1) {
            val stringBuilder = StringBuilder(ratioLimit.text)
            stringBuilder[separatorIndex] = getDecimalSeparator()
            ratioLimit = ratioLimit.copy(stringBuilder.toString())
        }
    }

    PersistentLaunchedEffect(serverId) {
        savePath = TextFieldValue()
    }

    PersistentLaunchedEffect(serverData == null) {
        if (savePath.text.isBlank()) {
            serverData?.let {
                savePath = TextFieldValue(it.defaultSavePath)
            }
        }
    }

    fun convertSpeedToBytes(speed: String, unit: Int): Int? {
        if (speed.isEmpty()) {
            return 0
        }

        val limit = speed.toIntOrNull() ?: return null
        return when (unit) {
            0 -> {
                if (limit > 2_000_000) {
                    null
                } else {
                    limit * 1024
                }
            }
            1 -> {
                if (limit > 2_000_000 / 1024) {
                    null
                } else {
                    limit * 1024 * 1024
                }
            }
            else -> null
        }
    }

    fun tryAddTorrent() {
        val currentServerId = serverId ?: return
        var isValid = true

        torrentLinkError = if (isUrlMode && torrentLinkText.text.isBlank()) {
            isValid = false
            Res.string.torrent_add_link_cannot_be_empty
        } else {
            null
        }

        torrentFileError = if (!isUrlMode && torrentFiles.isEmpty()) {
            isValid = false
            true
        } else {
            false
        }

        val downloadLimit = convertSpeedToBytes(downloadSpeedLimit.text, dlSpeedLimitUnit)
        downloadSpeedError = if (downloadLimit == null) {
            isValid = false
            Res.string.torrent_add_speed_limit_too_big
        } else {
            null
        }

        val uploadLimit = convertSpeedToBytes(uploadSpeedLimit.text, upSpeedLimitUnit)
        uploadSpeedError = if (uploadLimit == null) {
            isValid = false
            Res.string.torrent_add_speed_limit_too_big
        } else {
            null
        }

        if (!isValid) {
            return
        }

        val autoTmm = when (autoTmmIndex) {
            1 -> false
            2 -> true
            else -> null
        }

        val stopCondition = when (stopConditionIndex) {
            1 -> "None"
            2 -> "MetadataReceived"
            3 -> "FilesChecked"
            else -> null
        }

        val contentLayout = when (contentLayoutIndex) {
            1 -> "Original"
            2 -> "Subfolder"
            3 -> "NoSubfolder"
            else -> null
        }

        val savePathText = savePath.text.ifBlank { null }
        val finalSavePath = when (autoTmm) {
            null -> if (savePathText != serverData?.defaultSavePath) savePathText else null
            true -> null
            false -> savePathText
        }

        viewModel.addTorrent(
            serverId = currentServerId,
            links = if (isUrlMode) torrentLinkText.text.split("\n") else null,
            files = if (!isUrlMode) torrentFiles else null,
            savePath = finalSavePath,
            category = selectedCategory,
            tags = selectedTags.toList(),
            stopCondition = stopCondition,
            contentLayout = contentLayout,
            torrentName = torrentName.text.ifBlank { null },
            downloadSpeedLimit = downloadLimit ?: 0,
            uploadSpeedLimit = uploadLimit ?: 0,
            ratioLimit = ratioLimit.text.replace(getDecimalSeparator(), '.').toDoubleOrNull(),
            seedingTimeLimit = seedingTimeLimit.text.toIntOrNull(),
            isPaused = !startTorrent,
            skipHashChecking = skipChecking,
            isAutoTorrentManagementEnabled = autoTmm,
            isSequentialDownloadEnabled = sequentialDownload,
            isFirstLastPiecePrioritized = prioritizeFirstLastPiece,
        )
    }

    val scrollState = rememberScrollState()
    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets.safeDrawing.only(
            WindowInsetsSides.Horizontal + WindowInsetsSides.Top,
        ),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(Res.string.torrent_add_title),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                        )
                    }
                },
                actions = {
                    val actionMenuItems = listOf(
                        ActionMenuItem(
                            title = stringResource(Res.string.torrent_add_action_add_torrent),
                            icon = Icons.Filled.Add,
                            onClick = { tryAddTorrent() },
                            showAsAction = true,
                        ),
                    )

                    AppBarActions(items = actionMenuItems)
                },
                colors = scrollState.topAppBarColors(),
                windowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top),
            )
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
            onRefresh = { serverId?.let { viewModel.refreshData(it) } },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .imePadding(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val servers by viewModel.servers.collectAsStateWithLifecycle()
                if (initialServerId == null && servers.size > 1) {
                    var expanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        OutlinedTextField(
                            value = servers.find { it.id == serverId }?.displayName ?: "",
                            onValueChange = {},
                            label = {
                                Text(
                                    text = stringResource(Res.string.torrent_add_server),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            },
                            readOnly = true,
                            singleLine = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                        )

                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                        ) {
                            servers.forEach { serverConfig ->
                                DropdownMenuItem(
                                    text = { Text(text = serverConfig.displayName) },
                                    onClick = {
                                        expanded = false
                                        viewModel.setServerId(serverConfig.id)
                                    },
                                )
                            }
                        }
                    }
                }

                if (torrentUrl == null && torrentFileUris == null) {
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            selected = isUrlMode,
                            onClick = { isUrlMode = true },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        ) {
                            Text(text = stringResource(Res.string.torrent_add_button_url))
                        }
                        SegmentedButton(
                            selected = !isUrlMode,
                            onClick = { isUrlMode = false },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        ) {
                            Text(text = stringResource(Res.string.torrent_add_button_file))
                        }
                    }
                }

                AnimatedContent(
                    targetState = isUrlMode,
                    transitionSpec = {
                        (
                            fadeIn(animationSpec = tween(220, delayMillis = 90)) +
                                scaleIn(initialScale = 0.92f, animationSpec = tween(220, delayMillis = 90))
                            )
                            .togetherWith(fadeOut(animationSpec = tween(90)))
                            .using(SizeTransform(clip = false))
                    },
                ) { isUrlMode ->
                    if (isUrlMode) {
                        OutlinedTextField(
                            value = torrentLinkText,
                            onValueChange = {
                                if (it.text != torrentLinkText.text) {
                                    torrentLinkError = null
                                }
                                torrentLinkText = it
                            },
                            label = {
                                Text(
                                    text = stringResource(Res.string.torrent_add_link_hint),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateContentSize(),
                            minLines = 4,
                            maxLines = 4,
                            readOnly = torrentUrl != null,
                            isError = torrentLinkError != null,
                            supportingText = torrentLinkError?.let { { Text(text = stringResource(it)) } },
                            trailingIcon = torrentLinkError?.let {
                                {
                                    Icon(imageVector = Icons.Filled.Error, contentDescription = null)
                                }
                            },
                        )
                    } else {
                        val borderColor by animateColorAsState(
                            targetValue = if (torrentFileError) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.outlineVariant
                            },
                            animationSpec = tween(durationMillis = 150),
                        )

                        OutlinedCard(
                            border = BorderStroke(1.dp, borderColor),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier
                                    .then(
                                        if (torrentFileUris == null) {
                                            Modifier.clickable {
                                                scope.launch {
                                                    val files = FileKit.openFilePicker(
                                                        mode = FileKitMode.Multiple(),
                                                        type = FileKitType.File("torrent"),
                                                    )
                                                    if (files?.isNotEmpty() == true) {
                                                        torrentFiles = files
                                                        torrentFileError = false
                                                    }
                                                }
                                            }
                                        } else {
                                            Modifier
                                        },
                                    )
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AttachFile,
                                    contentDescription = null,
                                )
                                Text(
                                    text = when (torrentFiles.size) {
                                        0 -> stringResource(Res.string.torrent_add_click_to_select_file)
                                        1 -> torrentFiles.single().name
                                        else -> pluralStringResource(
                                            Res.plurals.torrent_add_files_selected,
                                            torrentFiles.size,
                                            torrentFiles.size,
                                        )
                                    },
                                    modifier = Modifier.weight(1f),
                                )
                                if (torrentFileError) {
                                    Icon(
                                        imageVector = Icons.Outlined.Error,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                    )
                                }
                            }
                        }
                    }
                }

                Text(text = stringResource(Res.string.torrent_add_category))

                AnimatedContent(
                    targetState = isLoading to serverData?.categoryList,
                ) { (isLoading, categoryList) ->
                    when {
                        isLoading -> LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        categoryList?.isNotEmpty() == true -> FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            categoryList.forEach { category ->
                                CategoryChip(
                                    category = category,
                                    isSelected = selectedCategory == category,
                                    onClick = {
                                        selectedCategory = if (selectedCategory == category) null else category
                                    },
                                )
                            }
                        }
                        else -> Text(
                            text = stringResource(Res.string.torrent_no_categories),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }

                Text(text = stringResource(Res.string.torrent_add_tags))

                AnimatedContent(
                    targetState = isLoading to serverData?.tagList,
                ) { (isLoading, tagList) ->
                    when {
                        isLoading -> LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        tagList?.isNotEmpty() == true -> FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            tagList.forEach { tag ->
                                TagChip(
                                    tag = tag,
                                    isSelected = selectedTags.contains(tag),
                                    onClick = {
                                        if (selectedTags.contains(tag)) {
                                            selectedTags.remove(tag)
                                        } else {
                                            selectedTags.add(tag)
                                        }
                                    },
                                )
                            }
                        }
                        else -> Text(
                            text = stringResource(Res.string.torrent_no_tags),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }

                CheckboxWithLabel(
                    checked = startTorrent,
                    onCheckedChange = { startTorrent = it },
                    label = stringResource(Res.string.torrent_add_start_torrent),
                )

                CheckboxWithLabel(
                    checked = skipChecking,
                    onCheckedChange = { skipChecking = it },
                    label = stringResource(Res.string.torrent_add_skip_hash_checking),
                )

                CheckboxWithLabel(
                    checked = sequentialDownload,
                    onCheckedChange = { sequentialDownload = it },
                    label = stringResource(Res.string.torrent_add_sequential_download),
                )

                CheckboxWithLabel(
                    checked = prioritizeFirstLastPiece,
                    onCheckedChange = { prioritizeFirstLastPiece = it },
                    label = stringResource(Res.string.torrent_add_prioritize_first_last_piece),
                )

                var autoTmmExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = autoTmmExpanded,
                    onExpandedChange = { autoTmmExpanded = it },
                ) {
                    OutlinedTextField(
                        label = {
                            Text(
                                text = stringResource(Res.string.torrent_add_torrent_management_mode),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        value = when (autoTmmIndex) {
                            0 -> stringResource(Res.string.torrent_add_default)
                            1 -> stringResource(Res.string.torrent_add_torrent_management_mode_manual)
                            2 -> stringResource(Res.string.torrent_add_torrent_management_mode_auto)
                            else -> ""
                        },
                        onValueChange = {},
                        readOnly = true,
                        singleLine = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = autoTmmExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                    )

                    ExposedDropdownMenu(
                        expanded = autoTmmExpanded,
                        onDismissRequest = { autoTmmExpanded = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text(text = stringResource(Res.string.torrent_add_default)) },
                            onClick = {
                                autoTmmIndex = 0
                                autoTmmExpanded = false
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(text = stringResource(Res.string.torrent_add_torrent_management_mode_manual)) },
                            onClick = {
                                autoTmmIndex = 1
                                autoTmmExpanded = false
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(text = stringResource(Res.string.torrent_add_torrent_management_mode_auto)) },
                            onClick = {
                                autoTmmIndex = 2
                                autoTmmExpanded = false
                            },
                        )
                    }
                }

                var stopConditionExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = stopConditionExpanded,
                    onExpandedChange = { stopConditionExpanded = it },
                ) {
                    OutlinedTextField(
                        label = {
                            Text(
                                text = stringResource(Res.string.torrent_add_stop_condition),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        value = when (stopConditionIndex) {
                            0 -> stringResource(Res.string.torrent_add_default)
                            1 -> stringResource(Res.string.torrent_add_stop_condition_none)
                            2 -> stringResource(Res.string.torrent_add_stop_condition_metadata_received)
                            3 -> stringResource(Res.string.torrent_add_stop_condition_files_checked)
                            else -> ""
                        },
                        onValueChange = {},
                        readOnly = true,
                        singleLine = true,
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = stopConditionExpanded)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                    )

                    ExposedDropdownMenu(
                        expanded = stopConditionExpanded,
                        onDismissRequest = { stopConditionExpanded = false },
                    ) {
                        listOf(
                            Res.string.torrent_add_default,
                            Res.string.torrent_add_stop_condition_none,
                            Res.string.torrent_add_stop_condition_metadata_received,
                            Res.string.torrent_add_stop_condition_files_checked,
                        ).forEachIndexed { index, stringRes ->
                            DropdownMenuItem(
                                text = { Text(text = stringResource(stringRes)) },
                                onClick = {
                                    stopConditionIndex = index
                                    stopConditionExpanded = false
                                },
                            )
                        }
                    }
                }

                var contentLayoutExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = contentLayoutExpanded,
                    onExpandedChange = { contentLayoutExpanded = it },
                ) {
                    OutlinedTextField(
                        label = {
                            Text(
                                text = stringResource(Res.string.torrent_add_content_layout),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        value = when (contentLayoutIndex) {
                            0 -> stringResource(Res.string.torrent_add_default)
                            1 -> stringResource(Res.string.torrent_add_content_layout_original)
                            2 -> stringResource(Res.string.torrent_add_content_layout_subfolder)
                            3 -> stringResource(Res.string.torrent_add_content_layout_no_subfolder)
                            else -> ""
                        },
                        onValueChange = {},
                        readOnly = true,
                        singleLine = true,
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = contentLayoutExpanded)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                    )

                    ExposedDropdownMenu(
                        expanded = contentLayoutExpanded,
                        onDismissRequest = { contentLayoutExpanded = false },
                    ) {
                        listOf(
                            Res.string.torrent_add_default,
                            Res.string.torrent_add_content_layout_original,
                            Res.string.torrent_add_content_layout_subfolder,
                            Res.string.torrent_add_content_layout_no_subfolder,
                        ).forEachIndexed { index, stringRes ->
                            DropdownMenuItem(
                                text = { Text(text = stringResource(stringRes)) },
                                onClick = {
                                    contentLayoutIndex = index
                                    contentLayoutExpanded = false
                                },
                            )
                        }
                    }
                }

                val directorySuggestions by viewModel.directorySuggestions.collectAsStateWithLifecycle()
                var savePathExpanded by remember { mutableStateOf(false) }

                // Hide suggestions when IME is folded
                val density = LocalDensity.current
                val isImeVisible = WindowInsets.ime.getBottom(density) > 0
                LaunchedEffect(isImeVisible) {
                    if (!isImeVisible) {
                        savePathExpanded = false
                    }
                }

                ExposedDropdownMenuBox(
                    expanded = savePathExpanded,
                    onExpandedChange = { savePathExpanded = it },
                ) {
                    OutlinedTextField(
                        value = savePath,
                        onValueChange = {
                            savePath = it
                            savePathExpanded = true
                            serverId?.let { id ->
                                viewModel.searchDirectories(id, it.text)
                            }
                        },
                        label = {
                            Text(
                                text = stringResource(Res.string.torrent_add_save_path),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable),
                        enabled = autoTmmIndex != 2,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Uri,
                            imeAction = ImeAction.Next,
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = {
                                savePathExpanded = false
                                defaultKeyboardAction(ImeAction.Next)
                            },
                        ),
                    )

                    if (directorySuggestions.isNotEmpty()) {
                        ExposedDropdownMenu(
                            expanded = savePathExpanded,
                            onDismissRequest = { savePathExpanded = false },
                            modifier = Modifier.heightIn(max = 240.dp),
                        ) {
                            directorySuggestions.forEach { suggestion ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = buildAnnotatedString {
                                                val matchLength = savePath.text.length
                                                if (suggestion.startsWith(savePath.text, ignoreCase = true)) {
                                                    withStyle(
                                                        style = SpanStyle(background = Color.LightGray.copy(alpha = 0.5f)),
                                                    ) {
                                                        append(suggestion.take(matchLength))
                                                    }
                                                    append(suggestion.drop(matchLength))
                                                } else {
                                                    append(suggestion)
                                                }
                                            },
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    },
                                    onClick = {
                                        savePath = TextFieldValue(
                                            text = suggestion,
                                            selection = TextRange(suggestion.length),
                                        )
                                        savePathExpanded = true
                                        // Trigger search again for the new path to load its subdirectories
                                        serverId?.let { id ->
                                            viewModel.searchDirectories(id, suggestion)
                                        }
                                    },
                                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = torrentName,
                    onValueChange = { torrentName = it },
                    label = {
                        Text(
                            text = stringResource(Res.string.torrent_add_name),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                )

                val ratioLimitRegex = remember {
                    val decimalSeparator = getDecimalSeparator()
                    Regex("^\\d*\\$decimalSeparator?\\d*$|^$")
                }
                OutlinedTextField(
                    value = ratioLimit,
                    onValueChange = { newValue ->
                        if (ratioLimitRegex.matches(newValue.text)) {
                            ratioLimit = newValue
                        }
                    },
                    label = {
                        Text(
                            text = stringResource(Res.string.torrent_add_ratio_limit),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Next,
                    ),
                )

                OutlinedTextField(
                    value = seedingTimeLimit,
                    onValueChange = {
                        if (it.text.all { it.isDigit() }) {
                            seedingTimeLimit = it
                        }
                    },
                    label = {
                        Text(
                            text = stringResource(Res.string.torrent_add_seeding_time_limit),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next,
                    ),
                )

                val speedUnitDropdownWidth = max(
                    measureTextWidth(stringResource(Res.string.speed_kibibytes_per_second)),
                    measureTextWidth(stringResource(Res.string.speed_mebibytes_per_second)),
                ) + 72.dp

                var upSpeedUnitExpanded by remember { mutableStateOf(false) }
                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = uploadSpeedLimit,
                        onValueChange = {
                            if (it.text.all { it.isDigit() }) {
                                if (it.text != uploadSpeedLimit.text) {
                                    uploadSpeedError = null
                                }
                                uploadSpeedLimit = it
                            }
                        },
                        label = {
                            Text(
                                text = stringResource(Res.string.torrent_add_upload_speed_limit),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        isError = uploadSpeedError != null,
                        supportingText = uploadSpeedError?.let { { Text(text = stringResource(it)) } },
                        trailingIcon = uploadSpeedError?.let {
                            {
                                Icon(imageVector = Icons.Filled.Error, contentDescription = null)
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .animateContentSize(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next,
                        ),
                    )

                    Spacer(modifier = Modifier.padding(horizontal = 8.dp))

                    ExposedDropdownMenuBox(
                        expanded = upSpeedUnitExpanded,
                        onExpandedChange = { upSpeedUnitExpanded = it },
                        modifier = Modifier
                            .width(speedUnitDropdownWidth)
                            .padding(top = 8.dp),
                    ) {
                        OutlinedTextField(
                            value = when (upSpeedLimitUnit) {
                                0 -> stringResource(Res.string.speed_kibibytes_per_second)
                                1 -> stringResource(Res.string.speed_mebibytes_per_second)
                                else -> ""
                            },
                            onValueChange = {},
                            readOnly = true,
                            singleLine = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = upSpeedUnitExpanded) },
                            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                        )

                        ExposedDropdownMenu(
                            expanded = upSpeedUnitExpanded,
                            onDismissRequest = { upSpeedUnitExpanded = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text(text = stringResource(Res.string.speed_kibibytes_per_second)) },
                                onClick = {
                                    upSpeedLimitUnit = 0
                                    upSpeedUnitExpanded = false
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(text = stringResource(Res.string.speed_mebibytes_per_second)) },
                                onClick = {
                                    upSpeedLimitUnit = 1
                                    upSpeedUnitExpanded = false
                                },
                            )
                        }
                    }
                }

                var dlSpeedUnitExpanded by remember { mutableStateOf(false) }
                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = downloadSpeedLimit,
                        onValueChange = {
                            if (it.text.all { it.isDigit() }) {
                                if (it.text != downloadSpeedLimit.text) {
                                    downloadSpeedError = null
                                }
                                downloadSpeedLimit = it
                            }
                        },
                        label = {
                            Text(
                                text = stringResource(Res.string.torrent_add_download_speed_limit),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        isError = downloadSpeedError != null,
                        supportingText = downloadSpeedError?.let { { Text(text = stringResource(it)) } },
                        trailingIcon = downloadSpeedError?.let {
                            {
                                Icon(imageVector = Icons.Filled.Error, contentDescription = null)
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .animateContentSize(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next,
                        ),
                    )

                    Spacer(modifier = Modifier.padding(horizontal = 8.dp))

                    ExposedDropdownMenuBox(
                        expanded = dlSpeedUnitExpanded,
                        onExpandedChange = { dlSpeedUnitExpanded = it },
                        modifier = Modifier
                            .width(speedUnitDropdownWidth)
                            .padding(top = 8.dp),
                    ) {
                        OutlinedTextField(
                            value = when (dlSpeedLimitUnit) {
                                0 -> stringResource(Res.string.speed_kibibytes_per_second)
                                1 -> stringResource(Res.string.speed_mebibytes_per_second)
                                else -> ""
                            },
                            onValueChange = {},
                            readOnly = true,
                            singleLine = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dlSpeedUnitExpanded) },
                            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                        )

                        ExposedDropdownMenu(
                            expanded = dlSpeedUnitExpanded,
                            onDismissRequest = { dlSpeedUnitExpanded = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text(text = stringResource(Res.string.speed_kibibytes_per_second)) },
                                onClick = {
                                    dlSpeedLimitUnit = 0
                                    dlSpeedUnitExpanded = false
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(text = stringResource(Res.string.speed_mebibytes_per_second)) },
                                onClick = {
                                    dlSpeedLimitUnit = 1
                                    dlSpeedUnitExpanded = false
                                },
                            )
                        }
                    }
                }

                Spacer(
                    modifier = Modifier.windowInsetsBottomHeight(WindowInsets.safeDrawing),
                )
            }

            val isAdding by viewModel.isAdding.collectAsStateWithLifecycle()
            AnimatedVisibility(
                visible = isAdding,
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
