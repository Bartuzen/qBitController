package dev.bartuzen.qbitcontroller.ui.addtorrent

import android.R.attr.singleLine
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.outlined.Error
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import dev.bartuzen.qbitcontroller.R
import dev.bartuzen.qbitcontroller.ui.components.ActionMenuItem
import dev.bartuzen.qbitcontroller.ui.components.AppBarActions
import dev.bartuzen.qbitcontroller.ui.components.CategoryChip
import dev.bartuzen.qbitcontroller.ui.components.CheckboxWithLabel
import dev.bartuzen.qbitcontroller.ui.components.SwipeableSnackbarHost
import dev.bartuzen.qbitcontroller.ui.components.TagChip
import dev.bartuzen.qbitcontroller.ui.theme.AppTheme
import dev.bartuzen.qbitcontroller.utils.EventEffect
import dev.bartuzen.qbitcontroller.utils.PersistentLaunchedEffect
import dev.bartuzen.qbitcontroller.utils.getErrorMessage
import dev.bartuzen.qbitcontroller.utils.getParcelableArrayListExtraCompat
import dev.bartuzen.qbitcontroller.utils.getParcelableExtraCompat
import dev.bartuzen.qbitcontroller.utils.measureTextWidth
import dev.bartuzen.qbitcontroller.utils.showToast
import dev.bartuzen.qbitcontroller.utils.stateListSaver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DecimalFormatSymbols

@AndroidEntryPoint
class AddTorrentActivity : AppCompatActivity() {
    object Extras {
        const val SERVER_ID = "dev.bartuzen.qbitcontroller.SERVER_ID"
        const val TORRENT_URL = "dev.bartuzen.qbitcontroller.TORRENT_URL"

        const val IS_ADDED = "dev.bartuzen.qbitcontroller.IS_ADDED"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val serverId = intent.getIntExtra(Extras.SERVER_ID, -1).takeIf { it != -1 }

        var torrentUrl = intent.getStringExtra(Extras.TORRENT_URL)
        var torrentFileUris: List<Uri>? = null

        if (torrentUrl == null) {
            when (intent.action) {
                Intent.ACTION_VIEW -> intent.data?.let { uri ->
                    when (uri.scheme) {
                        "magnet" -> torrentUrl = uri.toString()
                        "content" -> torrentFileUris = listOf(uri)
                    }
                }
                Intent.ACTION_SEND -> {
                    when (intent.type) {
                        "text/plain" -> intent.getStringExtra(Intent.EXTRA_TEXT)?.let { text ->
                            torrentUrl = text
                        }
                        "application/x-bittorrent" -> intent.getParcelableExtraCompat<Uri>(Intent.EXTRA_STREAM)?.let { uri ->
                            torrentFileUris = listOf(uri)
                        }
                    }
                }
                Intent.ACTION_SEND_MULTIPLE -> {
                    when (intent.type) {
                        "application/x-bittorrent" -> intent.getParcelableArrayListExtraCompat<Uri>(
                            Intent.EXTRA_STREAM,
                        )?.let { uris ->
                            torrentFileUris = uris
                        }
                    }
                }
            }
        }

        setContent {
            AppTheme {
                AddTorrentScreen(
                    initialServerId = serverId,
                    torrentUrl = torrentUrl,
                    torrentFileUris = torrentFileUris,
                    onNavigateBack = { finish() },
                    onTorrentAdded = {
                        if (intent.data == null) {
                            val resultIntent = Intent().apply {
                                putExtra(Extras.IS_ADDED, true)
                            }
                            setResult(RESULT_OK, resultIntent)
                        } else {
                            showToast(R.string.torrent_add_success)
                        }
                        finish()
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@Composable
private fun AddTorrentScreen(
    initialServerId: Int?,
    torrentUrl: String?,
    torrentFileUris: List<Uri>?,
    onNavigateBack: () -> Unit,
    onTorrentAdded: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AddTorrentViewModel = hiltViewModel(
        creationCallback = { factory: AddTorrentViewModel.Factory ->
            factory.create(initialServerId)
        },
    ),
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var isUrlMode by rememberSaveable { mutableStateOf(torrentFileUris == null) }

    var torrentLinkText by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(torrentUrl ?: ""))
    }
    var torrentLinkError by rememberSaveable { mutableStateOf<Int?>(null) }

    var torrentFileUris by rememberSaveable { mutableStateOf<List<Uri>>(torrentFileUris ?: emptyList()) }
    var torrentsFileName by rememberSaveable { mutableStateOf<String?>(null) }
    var torrentFileError by rememberSaveable { mutableStateOf<Boolean>(false) }

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

    var uploadSpeedError by rememberSaveable { mutableStateOf<Int?>(null) }
    var downloadSpeedError by rememberSaveable { mutableStateOf<Int?>(null) }

    val serverId by viewModel.serverId.collectAsStateWithLifecycle()

    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val serverData by viewModel.serverData.collectAsStateWithLifecycle()

    EventEffect(viewModel.eventFlow) { event ->
        when (event) {
            is AddTorrentViewModel.Event.Error -> {
                snackbarHostState.currentSnackbarData?.dismiss()
                scope.launch {
                    snackbarHostState.showSnackbar(getErrorMessage(context, event.error))
                }
            }
            AddTorrentViewModel.Event.FileNotFound -> {
                snackbarHostState.currentSnackbarData?.dismiss()
                scope.launch {
                    snackbarHostState.showSnackbar(context.getString(R.string.torrent_add_file_not_found))
                }
            }
            is AddTorrentViewModel.Event.FileReadError -> {
                snackbarHostState.currentSnackbarData?.dismiss()
                scope.launch {
                    snackbarHostState.showSnackbar(context.getString(R.string.error_unknown, event.error))
                }
            }
            AddTorrentViewModel.Event.TorrentAddError -> {
                snackbarHostState.currentSnackbarData?.dismiss()
                scope.launch {
                    snackbarHostState.showSnackbar(context.getString(R.string.torrent_add_error))
                }
            }
            AddTorrentViewModel.Event.InvalidTorrentFile -> {
                snackbarHostState.currentSnackbarData?.dismiss()
                scope.launch {
                    snackbarHostState.showSnackbar(context.getString(R.string.torrent_add_invalid_file))
                }
            }
            AddTorrentViewModel.Event.TorrentAdded -> {
                snackbarHostState.currentSnackbarData?.dismiss()
                onTorrentAdded()
            }
            AddTorrentViewModel.Event.NoServersFound -> {
                Toast.makeText(context, R.string.torrent_add_no_server, Toast.LENGTH_LONG).show()
                onNavigateBack()
            }
        }
    }

    LaunchedEffect(Unit) {
        val separatorIndex = ratioLimit.text.indexOfFirst { !it.isDigit() }
        if (separatorIndex != -1) {
            val decimalSeparator = DecimalFormatSymbols.getInstance().decimalSeparator
            val stringBuilder = StringBuilder(ratioLimit.text)
            stringBuilder[separatorIndex] = decimalSeparator
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

    val fileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments(),
    ) { uris ->
        if (uris.isNotEmpty()) {
            torrentFileUris = uris
            torrentFileError = false
        }
    }

    PersistentLaunchedEffect(torrentFileUris) {
        when (torrentFileUris.size) {
            0 -> {
                torrentsFileName = context.getString(R.string.torrent_add_click_to_select_file)
            }
            1 -> {
                withContext(Dispatchers.IO) {
                    val projection = arrayOf(MediaStore.MediaColumns.DISPLAY_NAME)
                    context.contentResolver.query(torrentFileUris.single(), projection, null, null, null)
                        ?.use { metaCursor ->
                            if (metaCursor.moveToFirst()) {
                                torrentsFileName = metaCursor.getString(0)
                            }
                        }
                }
            }
            else -> {
                torrentsFileName = context.resources.getQuantityString(
                    R.plurals.torrent_add_files_selected,
                    torrentFileUris.size,
                    torrentFileUris.size,
                )
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
            R.string.torrent_add_link_cannot_be_empty
        } else {
            null
        }

        torrentFileError = if (!isUrlMode && torrentFileUris.isEmpty()) {
            isValid = false
            true
        } else {
            false
        }

        val downloadLimit = convertSpeedToBytes(downloadSpeedLimit.text, dlSpeedLimitUnit)
        downloadSpeedError = if (downloadLimit == null) {
            isValid = false
            R.string.torrent_add_speed_limit_too_big
        } else {
            null
        }

        val uploadLimit = convertSpeedToBytes(uploadSpeedLimit.text, upSpeedLimitUnit)
        uploadSpeedError = if (uploadLimit == null) {
            isValid = false
            R.string.torrent_add_speed_limit_too_big
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
            fileUris = if (!isUrlMode) torrentFileUris else null,
            savePath = finalSavePath,
            category = selectedCategory,
            tags = selectedTags.toList(),
            stopCondition = stopCondition,
            contentLayout = contentLayout,
            torrentName = torrentName.text.ifBlank { null },
            downloadSpeedLimit = downloadLimit ?: 0,
            uploadSpeedLimit = uploadLimit ?: 0,
            ratioLimit = ratioLimit.text.replace(DecimalFormatSymbols.getInstance().decimalSeparator, '.').toDoubleOrNull(),
            seedingTimeLimit = seedingTimeLimit.text.toIntOrNull(),
            isPaused = !startTorrent,
            skipHashChecking = skipChecking,
            isAutoTorrentManagementEnabled = autoTmm,
            isSequentialDownloadEnabled = sequentialDownload,
            isFirstLastPiecePrioritized = prioritizeFirstLastPiece,
        )
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
                        text = stringResource(R.string.torrent_add_title),
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
                    val actionMenuItems = remember {
                        listOf(
                            ActionMenuItem(
                                title = context.getString(R.string.torrent_add_action_add_torrent),
                                icon = Icons.Filled.Add,
                                onClick = { tryAddTorrent() },
                                showAsAction = true,
                            ),
                        )
                    }

                    AppBarActions(items = actionMenuItems)
                },
                windowInsets = WindowInsets.safeDrawing
                    .exclude(WindowInsets.ime)
                    .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top),
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
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val servers by viewModel.servers.collectAsStateWithLifecycle()
                if (initialServerId == null && servers.size > 1) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        var expanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = !expanded },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            OutlinedTextField(
                                value = servers[serverId]?.displayName ?: "",
                                onValueChange = {},
                                label = {
                                    Text(
                                        text = stringResource(R.string.torrent_add_server),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                },
                                readOnly = true,
                                singleLine = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                            )

                            ExposedDropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false },
                            ) {
                                servers.forEach { (id, server) ->
                                    DropdownMenuItem(
                                        text = { Text(text = server.displayName) },
                                        onClick = {
                                            expanded = false
                                            viewModel.setServerId(id)
                                        },
                                    )
                                }
                            }
                        }
                    }
                }

                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = isUrlMode,
                        onClick = { isUrlMode = true },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(text = stringResource(R.string.torrent_add_button_url))
                    }
                    SegmentedButton(
                        selected = !isUrlMode,
                        onClick = { isUrlMode = false },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(text = stringResource(R.string.torrent_add_button_file))
                    }
                }

                if (isUrlMode) {
                    OutlinedTextField(
                        value = torrentLinkText,
                        onValueChange = {
                            torrentLinkText = it
                            if (torrentLinkError != null && it.text.isNotBlank()) {
                                torrentLinkError = null
                            }
                        },
                        label = {
                            Text(
                                text = stringResource(R.string.torrent_add_link_hint),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 4,
                        maxLines = 4,
                        isError = torrentLinkError != null,
                        supportingText = torrentLinkError?.let { { Text(text = stringResource(it)) } },
                        trailingIcon = torrentLinkError?.let {
                            {
                                Icon(imageVector = Icons.Filled.Error, contentDescription = null)
                            }
                        },
                    )
                } else {
                    OutlinedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { fileLauncher.launch(arrayOf("application/x-bittorrent")) },
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AttachFile,
                                contentDescription = null,
                            )
                            Text(
                                text = torrentsFileName ?: stringResource(R.string.torrent_add_click_to_select_file),
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

                Text(text = stringResource(R.string.torrent_add_category))

                if (isLoading) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                } else if (serverData?.categoryList?.isNotEmpty() == true) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        serverData?.categoryList?.forEach { category ->
                            CategoryChip(
                                category = category,
                                isSelected = selectedCategory == category,
                                onClick = {
                                    selectedCategory = if (selectedCategory == category) null else category
                                },
                            )
                        }
                    }
                } else {
                    Text(
                        text = stringResource(R.string.torrent_no_categories),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                Text(text = stringResource(R.string.torrent_add_tags))

                if (isLoading) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                } else if (serverData?.tagList?.isNotEmpty() == true) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        serverData?.tagList?.forEach { tag ->
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
                } else {
                    Text(
                        text = stringResource(R.string.torrent_no_tags),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                CheckboxWithLabel(
                    checked = startTorrent,
                    onCheckedChange = { startTorrent = it },
                    label = stringResource(R.string.torrent_add_start_torrent),
                )

                CheckboxWithLabel(
                    checked = skipChecking,
                    onCheckedChange = { skipChecking = it },
                    label = stringResource(R.string.torrent_add_skip_hash_checking),
                )

                CheckboxWithLabel(
                    checked = sequentialDownload,
                    onCheckedChange = { sequentialDownload = it },
                    label = stringResource(R.string.torrent_add_sequential_download),
                )

                CheckboxWithLabel(
                    checked = prioritizeFirstLastPiece,
                    onCheckedChange = { prioritizeFirstLastPiece = it },
                    label = stringResource(R.string.torrent_add_prioritize_first_last_piece),
                )

                var autoTmmExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = autoTmmExpanded,
                    onExpandedChange = { autoTmmExpanded = it },
                ) {
                    OutlinedTextField(
                        label = {
                            Text(
                                text = stringResource(R.string.torrent_add_torrent_management_mode),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        value = when (autoTmmIndex) {
                            0 -> stringResource(R.string.torrent_add_default)
                            1 -> stringResource(R.string.torrent_add_torrent_management_mode_manual)
                            2 -> stringResource(R.string.torrent_add_torrent_management_mode_auto)
                            else -> ""
                        },
                        onValueChange = {},
                        readOnly = true,
                        singleLine = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = autoTmmExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                    )

                    ExposedDropdownMenu(
                        expanded = autoTmmExpanded,
                        onDismissRequest = { autoTmmExpanded = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text(text = stringResource(R.string.torrent_add_default)) },
                            onClick = {
                                autoTmmIndex = 0
                                autoTmmExpanded = false
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(text = stringResource(R.string.torrent_add_torrent_management_mode_manual)) },
                            onClick = {
                                autoTmmIndex = 1
                                autoTmmExpanded = false
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(text = stringResource(R.string.torrent_add_torrent_management_mode_auto)) },
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
                                text = stringResource(R.string.torrent_add_stop_condition),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        value = when (stopConditionIndex) {
                            0 -> stringResource(R.string.torrent_add_default)
                            1 -> stringResource(R.string.torrent_add_stop_condition_none)
                            2 -> stringResource(R.string.torrent_add_stop_condition_metadata_received)
                            3 -> stringResource(R.string.torrent_add_stop_condition_files_checked)
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
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                    )

                    ExposedDropdownMenu(
                        expanded = stopConditionExpanded,
                        onDismissRequest = { stopConditionExpanded = false },
                    ) {
                        listOf(
                            R.string.torrent_add_default,
                            R.string.torrent_add_stop_condition_none,
                            R.string.torrent_add_stop_condition_metadata_received,
                            R.string.torrent_add_stop_condition_files_checked,
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
                                text = stringResource(R.string.torrent_add_content_layout),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        value = when (contentLayoutIndex) {
                            0 -> stringResource(R.string.torrent_add_default)
                            1 -> stringResource(R.string.torrent_add_content_layout_original)
                            2 -> stringResource(R.string.torrent_add_content_layout_subfolder)
                            3 -> stringResource(R.string.torrent_add_content_layout_no_subfolder)
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
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                    )

                    ExposedDropdownMenu(
                        expanded = contentLayoutExpanded,
                        onDismissRequest = { contentLayoutExpanded = false },
                    ) {
                        listOf(
                            R.string.torrent_add_default,
                            R.string.torrent_add_content_layout_original,
                            R.string.torrent_add_content_layout_subfolder,
                            R.string.torrent_add_content_layout_no_subfolder,
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

                OutlinedTextField(
                    value = savePath,
                    onValueChange = { savePath = it },
                    label = {
                        Text(
                            text = stringResource(R.string.torrent_add_save_path),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = autoTmmIndex != 2,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Next,
                    ),
                )

                OutlinedTextField(
                    value = torrentName,
                    onValueChange = { torrentName = it },
                    label = {
                        Text(
                            text = stringResource(R.string.torrent_add_name),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                )

                val ratioLimitRegex = remember {
                    val decimalSeparator = DecimalFormatSymbols.getInstance().decimalSeparator
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
                            text = stringResource(R.string.torrent_add_ratio_limit),
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
                            text = stringResource(R.string.torrent_add_seeding_time_limit),
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
                    measureTextWidth(stringResource(R.string.speed_kibibytes_per_second)),
                    measureTextWidth(stringResource(R.string.speed_mebibytes_per_second)),
                ) + 72.dp

                var upSpeedUnitExpanded by remember { mutableStateOf(false) }
                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = uploadSpeedLimit,
                        onValueChange = {
                            if (it.text.all { it.isDigit() }) {
                                uploadSpeedLimit = it
                                uploadSpeedError = null
                            }
                        },
                        label = {
                            Text(
                                text = stringResource(R.string.torrent_add_upload_speed_limit),
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
                        modifier = Modifier.weight(1f),
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
                                0 -> stringResource(R.string.speed_kibibytes_per_second)
                                1 -> stringResource(R.string.speed_mebibytes_per_second)
                                else -> ""
                            },
                            onValueChange = {},
                            readOnly = true,
                            singleLine = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = upSpeedUnitExpanded) },
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable),
                        )

                        ExposedDropdownMenu(
                            expanded = upSpeedUnitExpanded,
                            onDismissRequest = { upSpeedUnitExpanded = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text(text = stringResource(R.string.speed_kibibytes_per_second)) },
                                onClick = {
                                    upSpeedLimitUnit = 0
                                    upSpeedUnitExpanded = false
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(text = stringResource(R.string.speed_mebibytes_per_second)) },
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
                                downloadSpeedLimit = it
                                downloadSpeedError = null
                            }
                        },
                        label = {
                            Text(
                                text = stringResource(R.string.torrent_add_download_speed_limit),
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
                        modifier = Modifier.weight(1f),
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
                                0 -> stringResource(R.string.speed_kibibytes_per_second)
                                1 -> stringResource(R.string.speed_mebibytes_per_second)
                                else -> ""
                            },
                            onValueChange = {},
                            readOnly = true,
                            singleLine = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dlSpeedUnitExpanded) },
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable),
                        )

                        ExposedDropdownMenu(
                            expanded = dlSpeedUnitExpanded,
                            onDismissRequest = { dlSpeedUnitExpanded = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text(text = stringResource(R.string.speed_kibibytes_per_second)) },
                                onClick = {
                                    dlSpeedLimitUnit = 0
                                    dlSpeedUnitExpanded = false
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(text = stringResource(R.string.speed_mebibytes_per_second)) },
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
                visible = isAdding == true,
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
