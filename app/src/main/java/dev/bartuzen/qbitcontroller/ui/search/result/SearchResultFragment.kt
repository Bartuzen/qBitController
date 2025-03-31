package dev.bartuzen.qbitcontroller.ui.search.result

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.height
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowRight
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import dev.bartuzen.qbitcontroller.R
import dev.bartuzen.qbitcontroller.data.SearchSort
import dev.bartuzen.qbitcontroller.model.Search
import dev.bartuzen.qbitcontroller.ui.addtorrent.AddTorrentActivity
import dev.bartuzen.qbitcontroller.ui.components.ActionMenuItem
import dev.bartuzen.qbitcontroller.ui.components.AppBarActions
import dev.bartuzen.qbitcontroller.ui.components.Dialog
import dev.bartuzen.qbitcontroller.ui.components.SearchBar
import dev.bartuzen.qbitcontroller.ui.components.SwipeableSnackbarHost
import dev.bartuzen.qbitcontroller.ui.theme.AppTheme
import dev.bartuzen.qbitcontroller.ui.theme.LocalCustomColors
import dev.bartuzen.qbitcontroller.utils.EventEffect
import dev.bartuzen.qbitcontroller.utils.PersistentLaunchedEffect
import dev.bartuzen.qbitcontroller.utils.dropdownMenuHeight
import dev.bartuzen.qbitcontroller.utils.formatBytes
import dev.bartuzen.qbitcontroller.utils.formatUri
import dev.bartuzen.qbitcontroller.utils.getErrorMessage
import dev.bartuzen.qbitcontroller.utils.harmonizeWithPrimary
import dev.bartuzen.qbitcontroller.utils.jsonSaver
import dev.bartuzen.qbitcontroller.utils.measureTextWidth
import dev.bartuzen.qbitcontroller.utils.rememberReplaceAndApplyStyle
import dev.bartuzen.qbitcontroller.utils.rememberSearchStyle
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@AndroidEntryPoint
class SearchResultFragment() : Fragment() {
    private val serverId get() = arguments?.getInt("serverId", -1).takeIf { it != -1 }!!
    private val searchQuery get() = arguments?.getString("searchQuery")!!
    private val category get() = arguments?.getString("category")!!
    private val plugins get() = arguments?.getString("plugins")!!

    constructor(serverId: Int, searchQuery: String, category: String, plugins: String) : this() {
        arguments = bundleOf(
            "serverId" to serverId,
            "searchQuery" to searchQuery,
            "category" to category,
            "plugins" to plugins,
        )
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
        ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                AppTheme {
                    SearchResultScreen(
                        serverId = serverId,
                        searchQuery = searchQuery,
                        category = category,
                        plugins = plugins,
                        onNavigateBack = {
                            if (parentFragmentManager.backStackEntryCount > 0) {
                                parentFragmentManager.popBackStack()
                            } else {
                                requireActivity().finish()
                            }
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
}

@Composable
private fun SearchResultScreen(
    serverId: Int,
    searchQuery: String,
    category: String,
    plugins: String,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SearchResultViewModel = hiltViewModel(
        creationCallback = { factory: SearchResultViewModel.Factory ->
            factory.create(serverId, searchQuery, category, plugins)
        },
    ),
) {
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val searchCount by viewModel.searchCount.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val isSearchContinuing by viewModel.isSearchContinuing.collectAsStateWithLifecycle()
    val currentSorting by viewModel.searchSort.collectAsStateWithLifecycle()
    val isReverseSorting by viewModel.isReverseSearchSorting.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var filterQuery by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue()) }
    var isSearchMode by rememberSaveable { mutableStateOf(false) }

    EventEffect(viewModel.eventFlow) { event ->
        when (event) {
            is SearchResultViewModel.Event.Error -> {
                snackbarHostState.currentSnackbarData?.dismiss()
                scope.launch {
                    snackbarHostState.showSnackbar(getErrorMessage(context, event.error))
                }
            }
            SearchResultViewModel.Event.SearchStopped -> {
                snackbarHostState.currentSnackbarData?.dismiss()
                scope.launch {
                    snackbarHostState.showSnackbar(context.getString(R.string.search_result_stop_success))
                }
            }
        }
    }

    BackHandler(enabled = isSearchMode) {
        isSearchMode = false
        filterQuery = TextFieldValue()
        viewModel.setFilterQuery("")
    }

    val addTorrentLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val isAdded = result.data?.getBooleanExtra(AddTorrentActivity.Extras.IS_ADDED, false) == true
            if (isAdded) {
                snackbarHostState.currentSnackbarData?.dismiss()
                scope.launch {
                    snackbarHostState.showSnackbar(context.getString(R.string.torrent_add_success))
                }
            }
        }
    }

    var currentDialog by rememberSaveable(stateSaver = jsonSaver()) { mutableStateOf<Dialog?>(null) }
    when (val dialog = currentDialog) {
        is Dialog.Details -> {
            LaunchedEffect(searchResults) {
                if (searchResults?.contains(dialog.searchResult) != true) {
                    currentDialog = null
                }
            }

            DetailsDialog(
                searchResult = dialog.searchResult,
                onDismiss = { currentDialog = null },
                onDownload = {
                    val intent = Intent(context, AddTorrentActivity::class.java).apply {
                        putExtra(AddTorrentActivity.Extras.SERVER_ID, serverId)
                        putExtra(AddTorrentActivity.Extras.TORRENT_URL, dialog.searchResult.fileUrl)
                    }
                    addTorrentLauncher.launch(intent)
                    currentDialog = null
                },
                onOpenDescription = {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, dialog.searchResult.descriptionLink.toUri())
                        context.startActivity(intent)
                    } catch (_: ActivityNotFoundException) {
                        snackbarHostState.currentSnackbarData?.dismiss()
                        scope.launch {
                            snackbarHostState.showSnackbar(context.getString(R.string.search_result_no_browser))
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

    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets.safeDrawing.only(
            WindowInsetsSides.Horizontal + WindowInsetsSides.Top,
        ),
        topBar = {
            TopAppBar(
                title = {
                    if (!isSearchMode) {
                        Text(
                            text = searchQuery.ifBlank { stringResource(R.string.search_result_title) },
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
                            placeholder = stringResource(R.string.search_result_filter_results),
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
                    val actionMenuItems =
                        remember(currentSorting, isReverseSorting, isSearchContinuing, isSearchMode, filterQuery.text) {
                            listOf(
                                if (!isSearchMode) {
                                    ActionMenuItem(
                                        title = context.getString(R.string.action_search),
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
                                    title = context.getString(R.string.search_result_action_filter),
                                    icon = Icons.Filled.FilterList,
                                    onClick = { currentDialog = Dialog.Filter },
                                    showAsAction = true,
                                ),
                                ActionMenuItem(
                                    title = context.getString(R.string.search_result_action_sort),
                                    icon = Icons.AutoMirrored.Filled.Sort,
                                    onClick = { showSortMenu = true },
                                    showAsAction = true,
                                    trailingIcon = Icons.AutoMirrored.Filled.ArrowRight,
                                ),
                                ActionMenuItem(
                                    title = context.getString(R.string.search_result_action_stop_search),
                                    icon = Icons.Outlined.Cancel,
                                    onClick = { viewModel.stopSearch() },
                                    showAsAction = true,
                                    isEnabled = isSearchContinuing,
                                ),
                            )
                        }

                    AppBarActions(items = actionMenuItems)

                    val sortMenuScrollState = rememberScrollState()
                    LaunchedEffect(showSortMenu) {
                        if (showSortMenu) {
                            sortMenuScrollState.scrollTo(0)
                        }
                    }

                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false },
                        scrollState = sortMenuScrollState,
                        modifier = Modifier.dropdownMenuHeight(),
                    ) {
                        Text(
                            text = stringResource(R.string.search_result_action_sort),
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        )

                        val sortOptions = remember {
                            listOf(
                                R.string.search_result_action_sort_name to SearchSort.NAME,
                                R.string.search_result_action_sort_size to SearchSort.SIZE,
                                R.string.search_result_action_sort_seeders to SearchSort.SEEDERS,
                                R.string.search_result_action_sort_leechers to SearchSort.LEECHERS,
                                R.string.search_result_action_sort_search_engine to SearchSort.SEARCH_ENGINE,
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
                                    Text(text = stringResource(R.string.search_result_action_sort_reverse))
                                }
                            },
                            onClick = {
                                viewModel.changeReverseSorting()
                                showSortMenu = false
                            },
                        )
                    }
                },
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
            onRefresh = { viewModel.refresh() },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .imePadding(),
        ) {
            Column {
                Text(
                    text = stringResource(
                        R.string.search_result_showing_count,
                        searchResults?.size ?: 0,
                        searchCount ?: 0,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                )

                val listState = rememberLazyListState()
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
                            filterSearchQuery = filterQuery.text.ifEmpty { null },
                            onClick = { currentDialog = Dialog.Details(searchResult) },
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
    filterSearchQuery: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(
        modifier = modifier,
        onClick = onClick,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
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
                            tint = harmonizeWithPrimary(LocalCustomColors.current.seederColor),
                        )
                        Text(
                            text = searchResult.seeders?.toString() ?: "-",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = harmonizeWithPrimary(LocalCustomColors.current.seederColor),
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
                            tint = harmonizeWithPrimary(LocalCustomColors.current.leecherColor),
                        )
                        Text(
                            text = searchResult.leechers?.toString() ?: "-",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = harmonizeWithPrimary(LocalCustomColors.current.leecherColor),
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
            Text(text = stringResource(R.string.search_result_details))
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
                            label = stringResource(R.string.search_result_size),
                            value = if (searchResult.fileSize != null) formatBytes(searchResult.fileSize) else "-",
                        )

                        InfoRow(
                            icon = Icons.Outlined.Language,
                            label = stringResource(R.string.search_result_site),
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
                            label = stringResource(R.string.search_result_seeders),
                            color = harmonizeWithPrimary(LocalCustomColors.current.seederColor),
                        )

                        PeerInfoCard(
                            icon = Icons.Outlined.ArrowDownward,
                            count = searchResult.leechers?.toString() ?: "-",
                            label = stringResource(R.string.search_result_leechers),
                            color = harmonizeWithPrimary(LocalCustomColors.current.leecherColor),
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
                    Text(text = stringResource(R.string.search_result_download))
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
                    Text(text = stringResource(R.string.search_result_open_description))
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
            R.string.size_bytes,
            R.string.size_kibibytes,
            R.string.size_mebibytes,
            R.string.size_gibibytes,
            R.string.size_tebibytes,
            R.string.size_pebibytes,
            R.string.size_exbibytes,
        )
    }

    Dialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.search_result_action_filter)) },
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
                        tint = harmonizeWithPrimary(LocalCustomColors.current.seederColor),
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        text = stringResource(R.string.search_result_filter_seeds),
                        style = MaterialTheme.typography.titleMedium,
                        color = harmonizeWithPrimary(LocalCustomColors.current.seederColor),
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
                                text = stringResource(R.string.search_result_filter_min),
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
                                text = stringResource(R.string.search_result_filter_max),
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
                        text = stringResource(R.string.search_result_filter_size),
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
                                text = stringResource(R.string.search_result_filter_min),
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
                                text = stringResource(R.string.search_result_filter_max),
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
                    Text(text = stringResource(R.string.search_result_filter_reset))
                }

                Spacer(modifier = Modifier.weight(1f))

                TextButton(onClick = onDismiss) {
                    Text(text = stringResource(R.string.dialog_cancel))
                }

                TextButton(
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
                    Text(text = stringResource(R.string.dialog_ok))
                }
            }
        },
    )
}
