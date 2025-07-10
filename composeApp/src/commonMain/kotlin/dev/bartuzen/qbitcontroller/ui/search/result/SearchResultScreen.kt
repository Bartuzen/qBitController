package dev.bartuzen.qbitcontroller.ui.search.result

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.height
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowRight
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.bartuzen.qbitcontroller.data.SearchSort
import dev.bartuzen.qbitcontroller.model.Search
import dev.bartuzen.qbitcontroller.ui.components.ActionMenuItem
import dev.bartuzen.qbitcontroller.ui.components.AppBarActions
import dev.bartuzen.qbitcontroller.ui.components.Dialog
import dev.bartuzen.qbitcontroller.ui.components.DropdownMenuItem
import dev.bartuzen.qbitcontroller.ui.components.LazyColumnItemMinHeight
import dev.bartuzen.qbitcontroller.ui.components.PlatformBackHandler
import dev.bartuzen.qbitcontroller.ui.components.PullToRefreshBox
import dev.bartuzen.qbitcontroller.ui.components.SearchBar
import dev.bartuzen.qbitcontroller.ui.components.SwipeableSnackbarHost
import dev.bartuzen.qbitcontroller.ui.theme.LocalCustomColors
import dev.bartuzen.qbitcontroller.utils.EventEffect
import dev.bartuzen.qbitcontroller.utils.PersistentLaunchedEffect
import dev.bartuzen.qbitcontroller.utils.excludeBottom
import dev.bartuzen.qbitcontroller.utils.formatBytes
import dev.bartuzen.qbitcontroller.utils.formatUri
import dev.bartuzen.qbitcontroller.utils.getErrorMessage
import dev.bartuzen.qbitcontroller.utils.getString
import dev.bartuzen.qbitcontroller.utils.jsonSaver
import dev.bartuzen.qbitcontroller.utils.measureTextWidth
import dev.bartuzen.qbitcontroller.utils.rememberReplaceAndApplyStyle
import dev.bartuzen.qbitcontroller.utils.rememberSearchStyle
import dev.bartuzen.qbitcontroller.utils.stateListSaver
import dev.bartuzen.qbitcontroller.utils.stringResource
import dev.bartuzen.qbitcontroller.utils.topAppBarColor
import dev.bartuzen.qbitcontroller.utils.topAppBarColors
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.pluralStringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import qbitcontroller.composeapp.generated.resources.Res
import qbitcontroller.composeapp.generated.resources.action_search
import qbitcontroller.composeapp.generated.resources.dialog_cancel
import qbitcontroller.composeapp.generated.resources.dialog_ok
import qbitcontroller.composeapp.generated.resources.search_result_action_download
import qbitcontroller.composeapp.generated.resources.search_result_action_filter
import qbitcontroller.composeapp.generated.resources.search_result_action_sort
import qbitcontroller.composeapp.generated.resources.search_result_action_sort_leechers
import qbitcontroller.composeapp.generated.resources.search_result_action_sort_name
import qbitcontroller.composeapp.generated.resources.search_result_action_sort_reverse
import qbitcontroller.composeapp.generated.resources.search_result_action_sort_search_engine
import qbitcontroller.composeapp.generated.resources.search_result_action_sort_seeders
import qbitcontroller.composeapp.generated.resources.search_result_action_sort_size
import qbitcontroller.composeapp.generated.resources.search_result_action_stop_search
import qbitcontroller.composeapp.generated.resources.search_result_details
import qbitcontroller.composeapp.generated.resources.search_result_download
import qbitcontroller.composeapp.generated.resources.search_result_filter_max
import qbitcontroller.composeapp.generated.resources.search_result_filter_min
import qbitcontroller.composeapp.generated.resources.search_result_filter_reset
import qbitcontroller.composeapp.generated.resources.search_result_filter_results
import qbitcontroller.composeapp.generated.resources.search_result_filter_seeds
import qbitcontroller.composeapp.generated.resources.search_result_filter_size
import qbitcontroller.composeapp.generated.resources.search_result_leechers
import qbitcontroller.composeapp.generated.resources.search_result_no_browser
import qbitcontroller.composeapp.generated.resources.search_result_open_description
import qbitcontroller.composeapp.generated.resources.search_result_seeders
import qbitcontroller.composeapp.generated.resources.search_result_showing_count
import qbitcontroller.composeapp.generated.resources.search_result_site
import qbitcontroller.composeapp.generated.resources.search_result_size
import qbitcontroller.composeapp.generated.resources.search_result_stop_success
import qbitcontroller.composeapp.generated.resources.search_result_title
import qbitcontroller.composeapp.generated.resources.search_result_torrents_selected
import qbitcontroller.composeapp.generated.resources.size_bytes
import qbitcontroller.composeapp.generated.resources.size_exbibytes
import qbitcontroller.composeapp.generated.resources.size_gibibytes
import qbitcontroller.composeapp.generated.resources.size_kibibytes
import qbitcontroller.composeapp.generated.resources.size_mebibytes
import qbitcontroller.composeapp.generated.resources.size_pebibytes
import qbitcontroller.composeapp.generated.resources.size_tebibytes
import qbitcontroller.composeapp.generated.resources.torrent_add_success

@Composable
fun SearchResultScreen(
    serverId: Int,
    searchQuery: String,
    category: String,
    plugins: String,
    addTorrentFlow: Flow<Unit>,
    onNavigateBack: () -> Unit,
    onNavigateToAddTorrent: (torrentUrl: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SearchResultViewModel =
        koinViewModel(parameters = { parametersOf(serverId, searchQuery, category, plugins) }),
) {
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val searchCount by viewModel.searchCount.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val isSearchContinuing by viewModel.isSearchContinuing.collectAsStateWithLifecycle()
    val currentSorting by viewModel.searchSort.collectAsStateWithLifecycle()
    val isReverseSorting by viewModel.isReverseSearchSorting.collectAsStateWithLifecycle()

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var filterQuery by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue()) }
    var isSearchMode by rememberSaveable { mutableStateOf(false) }
    val selectedTorrents = rememberSaveable(saver = stateListSaver()) { mutableStateListOf<String>() }

    LaunchedEffect(addTorrentFlow) {
        addTorrentFlow.collect {
            snackbarHostState.currentSnackbarData?.dismiss()
            scope.launch {
                snackbarHostState.showSnackbar(getString(Res.string.torrent_add_success))
            }
        }
    }

    EventEffect(viewModel.eventFlow) { event ->
        when (event) {
            is SearchResultViewModel.Event.Error -> {
                snackbarHostState.currentSnackbarData?.dismiss()
                scope.launch {
                    snackbarHostState.showSnackbar(getErrorMessage(event.error))
                }
            }
            SearchResultViewModel.Event.SearchStopped -> {
                snackbarHostState.currentSnackbarData?.dismiss()
                scope.launch {
                    snackbarHostState.showSnackbar(getString(Res.string.search_result_stop_success))
                }
            }
        }
    }

    PlatformBackHandler(enabled = isSearchMode) {
        isSearchMode = false
        filterQuery = TextFieldValue()
        viewModel.setFilterQuery("")
    }

    PlatformBackHandler(enabled = selectedTorrents.isNotEmpty()) {
        selectedTorrents.clear()
    }

    var currentDialog by rememberSaveable(stateSaver = jsonSaver()) { mutableStateOf<Dialog?>(null) }
    when (val dialog = currentDialog) {
        is Dialog.Details -> {
            LaunchedEffect(searchResults) {
                if (searchResults?.contains(dialog.searchResult) != true) {
                    currentDialog = null
                }
            }

            val uriHandler = LocalUriHandler.current
            DetailsDialog(
                searchResult = dialog.searchResult,
                onDismiss = { currentDialog = null },
                onDownload = {
                    onNavigateToAddTorrent(dialog.searchResult.fileUrl)
                    currentDialog = null
                },
                onOpenDescription = {
                    try {
                        uriHandler.openUri(dialog.searchResult.descriptionLink)
                    } catch (_: IllegalArgumentException) {
                        snackbarHostState.currentSnackbarData?.dismiss()
                        scope.launch {
                            snackbarHostState.showSnackbar(getString(Res.string.search_result_no_browser))
                        }
                    }

                    currentDialog = null
                },
            )
        }
        is Dialog.Filter -> {
            FilterDialog(
                filter = viewModel.filter.collectAsStateWithLifecycle().value,
                onDismiss = { currentDialog = null },
                onConfirm = { filter ->
                    viewModel.setFilter(filter)
                    currentDialog = null
                },
                onReset = {
                    viewModel.resetFilter()
                    currentDialog = null
                },
            )
        }
        null -> {}
    }

    val listState = rememberLazyListState()
    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = {
                    if (!isSearchMode) {
                        Text(
                            text = searchQuery.ifBlank { stringResource(Res.string.search_result_title) },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    } else {
                        val focusRequester = remember { FocusRequester() }
                        PersistentLaunchedEffect {
                            focusRequester.requestFocus()
                        }

                        SearchBar(
                            value = filterQuery,
                            onValueChange = {
                                filterQuery = it
                                viewModel.setFilterQuery(it.text)
                            },
                            placeholder = stringResource(Res.string.search_result_filter_results),
                            modifier = Modifier.focusRequester(focusRequester),
                        )
                    }
                },
                navigationIcon = {
                    if (!isSearchMode) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = null,
                            )
                        }
                    } else {
                        IconButton(
                            onClick = {
                                isSearchMode = false
                                filterQuery = TextFieldValue()
                                viewModel.setFilterQuery("")
                            },
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = null,
                            )
                        }
                    }
                },
                actions = {
                    var showSortMenu by rememberSaveable { mutableStateOf(false) }
                    val actionMenuItems = listOf(
                        if (!isSearchMode) {
                            ActionMenuItem(
                                title = stringResource(Res.string.action_search),
                                icon = Icons.Filled.Search,
                                onClick = { isSearchMode = true },
                                showAsAction = true,
                            )
                        } else {
                            ActionMenuItem(
                                title = null,
                                icon = Icons.Filled.Close,
                                onClick = {
                                    filterQuery = TextFieldValue()
                                    viewModel.setFilterQuery("")
                                },
                                isHidden = filterQuery.text.isEmpty(),
                                showAsAction = true,
                            )
                        },
                        ActionMenuItem(
                            title = stringResource(Res.string.search_result_action_filter),
                            icon = Icons.Filled.FilterList,
                            onClick = { currentDialog = Dialog.Filter },
                            showAsAction = true,
                        ),
                        ActionMenuItem(
                            title = stringResource(Res.string.search_result_action_sort),
                            icon = Icons.AutoMirrored.Filled.Sort,
                            onClick = { showSortMenu = true },
                            showAsAction = true,
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowRight,
                                    contentDescription = null,
                                )
                            },
                            dropdownMenu = {
                                val scrollState = rememberScrollState()
                                LaunchedEffect(showSortMenu) {
                                    if (showSortMenu) {
                                        scrollState.scrollTo(0)
                                    }
                                }

                                DropdownMenu(
                                    expanded = showSortMenu,
                                    onDismissRequest = { showSortMenu = false },
                                    scrollState = scrollState,
                                ) {
                                    Text(
                                        text = stringResource(Res.string.search_result_action_sort),
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                    )

                                    val sortOptions = remember {
                                        listOf(
                                            Res.string.search_result_action_sort_name to SearchSort.NAME,
                                            Res.string.search_result_action_sort_size to SearchSort.SIZE,
                                            Res.string.search_result_action_sort_seeders to SearchSort.SEEDERS,
                                            Res.string.search_result_action_sort_leechers to SearchSort.LEECHERS,
                                            Res.string.search_result_action_sort_search_engine to SearchSort.SEARCH_ENGINE,
                                        )
                                    }
                                    sortOptions.forEach { (stringId, searchSort) ->
                                        DropdownMenuItem(
                                            text = {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                ) {
                                                    RadioButton(
                                                        selected = currentSorting == searchSort,
                                                        onClick = null,
                                                    )
                                                    Text(text = stringResource(stringId))
                                                }
                                            },
                                            onClick = {
                                                viewModel.setSearchSort(searchSort)
                                                showSortMenu = false
                                            },
                                        )
                                    }
                                    HorizontalDivider()
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            ) {
                                                Checkbox(
                                                    checked = isReverseSorting,
                                                    onCheckedChange = null,
                                                )
                                                Text(text = stringResource(Res.string.search_result_action_sort_reverse))
                                            }
                                        },
                                        onClick = {
                                            viewModel.changeReverseSorting()
                                            showSortMenu = false
                                        },
                                    )
                                }
                            },
                        ),
                        ActionMenuItem(
                            title = stringResource(Res.string.search_result_action_stop_search),
                            icon = Icons.Outlined.Cancel,
                            onClick = { viewModel.stopSearch() },
                            showAsAction = true,
                            isEnabled = isSearchContinuing,
                        ),
                    )

                    AppBarActions(items = actionMenuItems)
                },
                colors = listState.topAppBarColors(),
                windowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top),
            )
        },
        bottomBar = {
            Box {
                Spacer(modifier = Modifier.windowInsetsBottomHeight(WindowInsets.safeDrawing))

                AnimatedVisibility(
                    visible = selectedTorrents.isNotEmpty(),
                    enter = expandVertically(),
                    exit = shrinkVertically(),
                    modifier = Modifier.align(Alignment.BottomCenter),
                ) {
                    TopAppBar(
                        title = {
                            var selectedSize by rememberSaveable { mutableIntStateOf(0) }

                            LaunchedEffect(selectedTorrents.size) {
                                if (selectedTorrents.isNotEmpty()) {
                                    selectedSize = selectedTorrents.size
                                }
                            }

                            Text(
                                text = pluralStringResource(
                                    Res.plurals.search_result_torrents_selected,
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
                            IconButton(onClick = { selectedTorrents.clear() }) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = null,
                                )
                            }
                        },
                        actions = {
                            val actionMenuItems = listOf(
                                ActionMenuItem(
                                    title = stringResource(Res.string.search_result_action_download),
                                    onClick = {
                                        onNavigateToAddTorrent(
                                            selectedTorrents.joinToString("\n") {
                                                Json.decodeFromString<Search.Result>(it).fileUrl
                                            },
                                        )
                                    },
                                    showAsAction = true,
                                    icon = Icons.Filled.Download,
                                ),
                            )

                            AppBarActions(items = actionMenuItems, bottom = true)
                        },
                        windowInsets = WindowInsets.safeDrawing.only(
                            WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom,
                        ),
                    )
                }
            }
        },
        snackbarHost = {
            SwipeableSnackbarHost(hostState = snackbarHostState)
        },
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding.excludeBottom())
                .consumeWindowInsets(innerPadding)
                .imePadding(),
        ) {
            Column {
                Text(
                    text = stringResource(
                        Res.string.search_result_showing_count,
                        searchResults?.size ?: 0,
                        searchCount ?: 0,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(topAppBarColor(listState))
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                )

                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(
                        items = searchResults ?: emptyList(),
                        key = { Json.encodeToString(it) },
                    ) { searchResult ->
                        SearchResultItem(
                            searchResult = searchResult,
                            selected = Json.encodeToString(searchResult) in selectedTorrents,
                            filterSearchQuery = filterQuery.text.ifEmpty { null },
                            onClick = {
                                if (selectedTorrents.isNotEmpty()) {
                                    val searchJson = Json.encodeToString(searchResult)
                                    if (searchJson !in selectedTorrents) {
                                        selectedTorrents += searchJson
                                    } else {
                                        selectedTorrents -= searchJson
                                    }
                                } else {
                                    currentDialog = Dialog.Details(searchResult)
                                }
                            },
                            onLongClick = {
                                val searchJson = Json.encodeToString(searchResult)
                                if (searchJson !in selectedTorrents) {
                                    selectedTorrents += searchJson
                                } else {
                                    selectedTorrents -= searchJson
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateItem(),
                        )
                    }

                    item {
                        LazyColumnItemMinHeight()
                        Spacer(modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding()))
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
            }

            AnimatedVisibility(
                visible = isSearchContinuing,
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
private fun SearchResultItem(
    searchResult: Search.Result,
    selected: Boolean,
    filterSearchQuery: String?,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.surfaceContainerHigh
            } else {
                Color.Unspecified
            },
        ),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick,
                )
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            val name = if (filterSearchQuery != null) {
                rememberSearchStyle(
                    text = searchResult.fileName,
                    searchQuery = filterSearchQuery,
                    style = SpanStyle(
                        color = MaterialTheme.colorScheme.onPrimary,
                        background = MaterialTheme.colorScheme.primary,
                    ),
                )
            } else {
                AnnotatedString(searchResult.fileName)
            }
            Text(
                text = name,
                style = MaterialTheme.typography.titleMedium,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Storage,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = if (searchResult.fileSize != null) {
                                formatBytes(searchResult.fileSize)
                            } else {
                                "-"
                            },
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Language,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = formatUri(searchResult.siteUrl),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ArrowUpward,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = LocalCustomColors.current.seederColor,
                        )
                        Text(
                            text = searchResult.seeders?.toString() ?: "-",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = LocalCustomColors.current.seederColor,
                                fontWeight = FontWeight.SemiBold,
                            ),
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ArrowDownward,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = LocalCustomColors.current.leecherColor,
                        )
                        Text(
                            text = searchResult.leechers?.toString() ?: "-",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = LocalCustomColors.current.leecherColor,
                                fontWeight = FontWeight.SemiBold,
                            ),
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
    data class Details(val searchResult: Search.Result) : Dialog()

    @Serializable
    data object Filter : Dialog()
}

@Composable
private fun DetailsDialog(
    searchResult: Search.Result,
    onDismiss: () -> Unit,
    onDownload: () -> Unit,
    onOpenDescription: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Dialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        title = {
            Text(text = stringResource(Res.string.search_result_details))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = searchResult.fileName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                HorizontalDivider()

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        InfoRow(
                            icon = Icons.Outlined.Storage,
                            label = stringResource(Res.string.search_result_size),
                            value = if (searchResult.fileSize != null) formatBytes(searchResult.fileSize) else "-",
                        )

                        InfoRow(
                            icon = Icons.Outlined.Language,
                            label = stringResource(Res.string.search_result_site),
                            value = formatUri(searchResult.siteUrl),
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround,
                    ) {
                        PeerInfoCard(
                            icon = Icons.Outlined.ArrowUpward,
                            count = searchResult.seeders?.toString() ?: "-",
                            label = stringResource(Res.string.search_result_seeders),
                            color = LocalCustomColors.current.seederColor,
                        )

                        PeerInfoCard(
                            icon = Icons.Outlined.ArrowDownward,
                            count = searchResult.leechers?.toString() ?: "-",
                            label = stringResource(Res.string.search_result_leechers),
                            color = LocalCustomColors.current.leecherColor,
                        )
                    }
                }
            }
        },
        confirmButton = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Button(
                    onClick = onDownload,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Download,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = stringResource(Res.string.search_result_download))
                }

                FilledTonalButton(
                    onClick = onOpenDescription,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Description,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = stringResource(Res.string.search_result_open_description))
                }
            }
        },
    )
}

@Composable
private fun InfoRow(icon: ImageVector, label: String, value: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = rememberReplaceAndApplyStyle(
                text = label,
                oldValue = "%1\$s",
                newValue = value,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                ).toSpanStyle(),
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun PeerInfoCard(icon: ImageVector, count: String, label: String, color: Color) {
    Card(
        colors = CardDefaults.elevatedCardColors(
            containerColor = color.copy(alpha = 0.1f),
        ),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = count,
                    style = MaterialTheme.typography.titleMedium,
                    color = color,
                )
            }

            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun FilterDialog(
    filter: SearchResultViewModel.Filter,
    onDismiss: () -> Unit,
    onConfirm: (filter: SearchResultViewModel.Filter) -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var seedsMin by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(filter.seedsMin?.toString() ?: ""))
    }
    var seedsMax by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(filter.seedsMax?.toString() ?: ""))
    }
    var sizeMin by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(filter.sizeMin?.toString() ?: ""))
    }
    var sizeMax by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(filter.sizeMax?.toString() ?: ""))
    }
    var sizeMinUnit by rememberSaveable { mutableIntStateOf(filter.sizeMinUnit) }
    var sizeMaxUnit by rememberSaveable { mutableIntStateOf(filter.sizeMaxUnit) }

    val sizeUnits = remember {
        listOf(
            Res.string.size_bytes,
            Res.string.size_kibibytes,
            Res.string.size_mebibytes,
            Res.string.size_gibibytes,
            Res.string.size_tebibytes,
            Res.string.size_pebibytes,
            Res.string.size_exbibytes,
        )
    }

    Dialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(Res.string.search_result_action_filter)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ArrowUpward,
                        contentDescription = null,
                        tint = LocalCustomColors.current.seederColor,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        text = stringResource(Res.string.search_result_filter_seeds),
                        style = MaterialTheme.typography.titleMedium,
                        color = LocalCustomColors.current.seederColor,
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = seedsMin,
                        onValueChange = {
                            if (it.text.all { it.isDigit() }) {
                                seedsMin = it
                            }
                        },
                        label = {
                            Text(
                                text = stringResource(Res.string.search_result_filter_min),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next,
                        ),
                        modifier = Modifier.weight(1f),
                    )

                    OutlinedTextField(
                        value = seedsMax,
                        onValueChange = {
                            if (it.text.all { it.isDigit() }) {
                                seedsMax = it
                            }
                        },
                        label = {
                            Text(
                                text = stringResource(Res.string.search_result_filter_max),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next,
                        ),
                        modifier = Modifier.weight(1f),
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Storage,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        text = stringResource(Res.string.search_result_filter_size),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }

                val speedUnitDropdownWidth = sizeUnits.maxOf { measureTextWidth(stringResource(it)) } + 72.dp

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = sizeMin,
                        onValueChange = {
                            if (it.text.all { it.isDigit() }) {
                                sizeMin = it
                            }
                        },
                        label = {
                            Text(
                                text = stringResource(Res.string.search_result_filter_min),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next,
                        ),
                        modifier = Modifier.weight(1f),
                    )

                    var expanded by rememberSaveable { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = it },
                        modifier = Modifier
                            .width(speedUnitDropdownWidth)
                            .padding(top = 8.dp),
                    ) {
                        OutlinedTextField(
                            value = stringResource(sizeUnits[sizeMinUnit]),
                            onValueChange = {},
                            readOnly = true,
                            singleLine = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable),
                        )

                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                        ) {
                            sizeUnits.forEachIndexed { sizeUnit, stringId ->
                                DropdownMenuItem(
                                    text = { Text(text = stringResource(stringId)) },
                                    onClick = {
                                        sizeMinUnit = sizeUnit
                                        expanded = false
                                    },
                                )
                            }
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = sizeMax,
                        onValueChange = {
                            if (it.text.all { it.isDigit() }) {
                                sizeMax = it
                            }
                        },
                        label = {
                            Text(
                                text = stringResource(Res.string.search_result_filter_max),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next,
                        ),
                        modifier = Modifier.weight(1f),
                    )

                    var expanded by rememberSaveable { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = it },
                        modifier = Modifier
                            .width(speedUnitDropdownWidth)
                            .padding(top = 8.dp),
                    ) {
                        OutlinedTextField(
                            value = stringResource(sizeUnits[sizeMaxUnit]),
                            onValueChange = {},
                            readOnly = true,
                            singleLine = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable),
                        )

                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                        ) {
                            sizeUnits.forEachIndexed { sizeUnit, stringId ->
                                DropdownMenuItem(
                                    text = { Text(text = stringResource(stringId)) },
                                    onClick = {
                                        sizeMaxUnit = sizeUnit
                                        expanded = false
                                    },
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = onReset) {
                    Text(text = stringResource(Res.string.search_result_filter_reset))
                }

                Spacer(modifier = Modifier.weight(1f))

                TextButton(onClick = onDismiss) {
                    Text(text = stringResource(Res.string.dialog_cancel))
                }

                Button(
                    onClick = {
                        val newFilter = SearchResultViewModel.Filter(
                            seedsMin = seedsMin.text.toIntOrNull(),
                            seedsMax = seedsMax.text.toIntOrNull(),
                            sizeMin = sizeMin.text.toLongOrNull(),
                            sizeMax = sizeMax.text.toLongOrNull(),
                            sizeMinUnit = sizeMinUnit,
                            sizeMaxUnit = sizeMaxUnit,
                        )
                        onConfirm(newFilter)
                    },
                ) {
                    Text(text = stringResource(Res.string.dialog_ok))
                }
            }
        },
    )
}
