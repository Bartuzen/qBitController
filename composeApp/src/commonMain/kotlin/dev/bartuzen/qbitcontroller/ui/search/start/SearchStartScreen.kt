package dev.bartuzen.qbitcontroller.ui.search.start

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Animation
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.bartuzen.qbitcontroller.model.Plugin
import dev.bartuzen.qbitcontroller.ui.components.ActionMenuItem
import dev.bartuzen.qbitcontroller.ui.components.AppBarActions
import dev.bartuzen.qbitcontroller.ui.components.DropdownMenuItem
import dev.bartuzen.qbitcontroller.ui.components.PullToRefreshBox
import dev.bartuzen.qbitcontroller.ui.components.RadioButtonWithLabel
import dev.bartuzen.qbitcontroller.ui.components.SwipeableSnackbarHost
import dev.bartuzen.qbitcontroller.utils.EventEffect
import dev.bartuzen.qbitcontroller.utils.getErrorMessage
import dev.bartuzen.qbitcontroller.utils.stateListSaver
import dev.bartuzen.qbitcontroller.utils.stringResource
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import qbitcontroller.composeapp.generated.resources.Res
import qbitcontroller.composeapp.generated.resources.search_plugins
import qbitcontroller.composeapp.generated.resources.search_start_action_start
import qbitcontroller.composeapp.generated.resources.search_start_category
import qbitcontroller.composeapp.generated.resources.search_start_category_all
import qbitcontroller.composeapp.generated.resources.search_start_category_anime
import qbitcontroller.composeapp.generated.resources.search_start_category_books
import qbitcontroller.composeapp.generated.resources.search_start_category_games
import qbitcontroller.composeapp.generated.resources.search_start_category_movies
import qbitcontroller.composeapp.generated.resources.search_start_category_music
import qbitcontroller.composeapp.generated.resources.search_start_category_pictures
import qbitcontroller.composeapp.generated.resources.search_start_category_software
import qbitcontroller.composeapp.generated.resources.search_start_category_tv_shows
import qbitcontroller.composeapp.generated.resources.search_start_plugins
import qbitcontroller.composeapp.generated.resources.search_start_plugins_all
import qbitcontroller.composeapp.generated.resources.search_start_plugins_enabled
import qbitcontroller.composeapp.generated.resources.search_start_plugins_select
import qbitcontroller.composeapp.generated.resources.search_start_query
import qbitcontroller.composeapp.generated.resources.search_title

@Composable
fun SearchStartScreen(
    serverId: Int,
    onNavigateToPlugins: () -> Unit,
    onNavigateToSearchResult: (searchQuery: String, category: String, plugins: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SearchStartViewModel = koinViewModel(parameters = { parametersOf(serverId) }),
) {
    val plugins by viewModel.plugins.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var searchQuery by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue()) }
    var selectedCategoryIndex by rememberSaveable { mutableIntStateOf(0) }
    var selectedPluginOption by rememberSaveable { mutableStateOf(PluginSelection.Enabled) }
    val selectedPlugins = rememberSaveable(saver = stateListSaver()) { mutableStateListOf<String>() }

    EventEffect(viewModel.eventFlow) { event ->
        when (event) {
            is SearchStartViewModel.Event.Error -> {
                snackbarHostState.currentSnackbarData?.dismiss()
                scope.launch {
                    snackbarHostState.showSnackbar(getErrorMessage(event.error))
                }
            }
        }
    }

    LaunchedEffect(plugins) {
        plugins?.let { pluginList ->
            selectedPlugins.removeAll { name -> pluginList.none { it.name == name } }
        }
    }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets.safeDrawing.only(
            WindowInsetsSides.Horizontal + WindowInsetsSides.Top,
        ),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(Res.string.search_title),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                actions = {
                    val actionMenuItems = listOf(
                        ActionMenuItem(
                            title = stringResource(Res.string.search_plugins),
                            icon = Icons.Filled.Extension,
                            onClick = onNavigateToPlugins,
                            showAsAction = true,
                        ),
                        ActionMenuItem(
                            title = stringResource(Res.string.search_start_action_start),
                            icon = Icons.Filled.Search,
                            onClick = {
                                val category = when (selectedCategoryIndex) {
                                    0 -> "all"
                                    1 -> "anime"
                                    2 -> "books"
                                    3 -> "games"
                                    4 -> "movies"
                                    5 -> "music"
                                    6 -> "pictures"
                                    7 -> "software"
                                    8 -> "tv"
                                    else -> "all"
                                }

                                val pluginsParam = when (selectedPluginOption) {
                                    PluginSelection.Enabled -> "enabled"
                                    PluginSelection.All -> "all"
                                    PluginSelection.Selected -> selectedPlugins.joinToString("|")
                                }

                                onNavigateToSearchResult(searchQuery.text, category, pluginsParam)
                            },
                            showAsAction = true,
                        ),
                    )

                    AppBarActions(items = actionMenuItems)
                },
                scrollBehavior = scrollBehavior,
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
            onRefresh = { viewModel.refreshPlugins() },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .imePadding(),
        ) {
            val listState = rememberLazyListState()
            LazyColumn(
                state = listState,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(scrollBehavior.nestedScrollConnection),
            ) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 16.dp, top = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = {
                                    Text(
                                        text = stringResource(Res.string.search_start_query),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Filled.Search,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            )

                            var expanded by rememberSaveable { mutableStateOf(false) }
                            val categories = remember {
                                listOf(
                                    Res.string.search_start_category_all,
                                    Res.string.search_start_category_anime,
                                    Res.string.search_start_category_books,
                                    Res.string.search_start_category_games,
                                    Res.string.search_start_category_movies,
                                    Res.string.search_start_category_music,
                                    Res.string.search_start_category_pictures,
                                    Res.string.search_start_category_software,
                                    Res.string.search_start_category_tv_shows,
                                )
                            }

                            ExposedDropdownMenuBox(
                                expanded = expanded,
                                onExpandedChange = { expanded = it },
                                modifier = Modifier,
                            ) {
                                OutlinedTextField(
                                    value = stringResource(categories[selectedCategoryIndex]),
                                    onValueChange = {},
                                    readOnly = true,
                                    singleLine = true,
                                    label = {
                                        Text(
                                            text = stringResource(Res.string.search_start_category),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                    leadingIcon = {
                                        val icon = when (selectedCategoryIndex) {
                                            0 -> Icons.Filled.Category
                                            1 -> Icons.Filled.Animation
                                            2 -> Icons.Filled.Book
                                            3 -> Icons.Filled.SportsEsports
                                            4 -> Icons.Filled.Movie
                                            5 -> Icons.Filled.MusicNote
                                            6 -> Icons.Filled.Image
                                            7 -> Icons.Filled.Computer
                                            8 -> Icons.Filled.Tv
                                            else -> Icons.Filled.Category
                                        }
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                        )
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                                )

                                ExposedDropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false },
                                ) {
                                    categories.forEachIndexed { index, category ->
                                        DropdownMenuItem(
                                            text = { Text(text = stringResource(category)) },
                                            onClick = {
                                                selectedCategoryIndex = index
                                                expanded = false
                                            },
                                            leadingIcon = {
                                                val icon = when (index) {
                                                    0 -> Icons.Filled.Category
                                                    1 -> Icons.Filled.Animation
                                                    2 -> Icons.Filled.Book
                                                    3 -> Icons.Filled.SportsEsports
                                                    4 -> Icons.Filled.Movie
                                                    5 -> Icons.Filled.MusicNote
                                                    6 -> Icons.Filled.Image
                                                    7 -> Icons.Filled.Computer
                                                    8 -> Icons.Filled.Tv
                                                    else -> Icons.Filled.Category
                                                }
                                                Icon(
                                                    imageVector = icon,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                )
                                            },
                                            colors = MenuDefaults.itemColors(
                                                textColor = if (selectedCategoryIndex == index) {
                                                    MaterialTheme.colorScheme.primary
                                                } else {
                                                    Color.Unspecified
                                                },
                                            ),
                                        )
                                    }
                                }
                            }
                        }

                        HorizontalDivider()

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = stringResource(Res.string.search_start_plugins),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .selectableGroup()
                                    .border(
                                        width = OutlinedTextFieldDefaults.UnfocusedBorderThickness,
                                        color = OutlinedTextFieldDefaults.colors().unfocusedIndicatorColor,
                                        shape = OutlinedTextFieldDefaults.shape,
                                    )
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                RadioButtonWithLabel(
                                    selected = selectedPluginOption == PluginSelection.Enabled,
                                    onClick = { selectedPluginOption = PluginSelection.Enabled },
                                    label = stringResource(Res.string.search_start_plugins_enabled),
                                )
                                RadioButtonWithLabel(
                                    selected = selectedPluginOption == PluginSelection.All,
                                    onClick = { selectedPluginOption = PluginSelection.All },
                                    label = stringResource(Res.string.search_start_plugins_all),
                                )
                                RadioButtonWithLabel(
                                    selected = selectedPluginOption == PluginSelection.Selected,
                                    onClick = { selectedPluginOption = PluginSelection.Selected },
                                    label = stringResource(Res.string.search_start_plugins_select),
                                )
                            }
                        }
                    }
                }

                if (selectedPluginOption == PluginSelection.Selected) {
                    items(
                        items = plugins ?: emptyList(),
                        key = { it.name },
                    ) { plugin ->
                        PluginItem(
                            plugin = plugin,
                            selectedPlugins = selectedPlugins,
                            onClick = {
                                if (plugin.name in selectedPlugins) {
                                    selectedPlugins.remove(plugin.name)
                                } else {
                                    selectedPlugins.add(plugin.name)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .animateItem(),
                        )
                    }
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
                visible = isLoading == true,
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
private fun PluginItem(plugin: Plugin, selectedPlugins: List<String>, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (plugin.name in selectedPlugins) {
                MaterialTheme.colorScheme.surfaceContainerHighest
            } else {
                MaterialTheme.colorScheme.surfaceContainer
            },
        ),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(24.dp),
        ) {
            Checkbox(
                checked = plugin.name in selectedPlugins,
                onCheckedChange = null,
            )

            Text(
                text = plugin.fullName,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

private enum class PluginSelection {
    Enabled,
    All,
    Selected,
}
