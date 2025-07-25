package dev.bartuzen.qbitcontroller.ui.search.plugins

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Update
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.bartuzen.qbitcontroller.model.Plugin
import dev.bartuzen.qbitcontroller.ui.components.ActionMenuItem
import dev.bartuzen.qbitcontroller.ui.components.AppBarActions
import dev.bartuzen.qbitcontroller.ui.components.Dialog
import dev.bartuzen.qbitcontroller.ui.components.LazyColumnItemMinHeight
import dev.bartuzen.qbitcontroller.ui.components.PullToRefreshBox
import dev.bartuzen.qbitcontroller.ui.components.SwipeableSnackbarHost
import dev.bartuzen.qbitcontroller.utils.EventEffect
import dev.bartuzen.qbitcontroller.utils.getErrorMessage
import dev.bartuzen.qbitcontroller.utils.getString
import dev.bartuzen.qbitcontroller.utils.jsonSaver
import dev.bartuzen.qbitcontroller.utils.stateListSaver
import dev.bartuzen.qbitcontroller.utils.stateMapSaver
import dev.bartuzen.qbitcontroller.utils.stringResource
import dev.bartuzen.qbitcontroller.utils.stringResourceSaver
import dev.bartuzen.qbitcontroller.utils.topAppBarColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import qbitcontroller.composeapp.generated.resources.Res
import qbitcontroller.composeapp.generated.resources.dialog_cancel
import qbitcontroller.composeapp.generated.resources.dialog_ok
import qbitcontroller.composeapp.generated.resources.search_plugins
import qbitcontroller.composeapp.generated.resources.search_plugins_action_install_plugins
import qbitcontroller.composeapp.generated.resources.search_plugins_action_save
import qbitcontroller.composeapp.generated.resources.search_plugins_action_update_plugins
import qbitcontroller.composeapp.generated.resources.search_plugins_cannot_be_empty
import qbitcontroller.composeapp.generated.resources.search_plugins_install_hint
import qbitcontroller.composeapp.generated.resources.search_plugins_install_success
import qbitcontroller.composeapp.generated.resources.search_plugins_save_success
import qbitcontroller.composeapp.generated.resources.search_plugins_update_success

@Composable
fun SearchPluginsScreen(
    serverId: Int,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SearchPluginsViewModel = koinViewModel(parameters = { parametersOf(serverId) }),
) {
    val plugins by viewModel.plugins.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val pluginsEnabledState = rememberSaveable(saver = stateMapSaver()) { mutableStateMapOf<String, Boolean>() }
    val pluginsToDelete = rememberSaveable(saver = stateListSaver()) { mutableStateListOf<String>() }

    EventEffect(viewModel.eventFlow) { event ->
        when (event) {
            is SearchPluginsViewModel.Event.Error -> {
                snackbarHostState.currentSnackbarData?.dismiss()
                scope.launch {
                    snackbarHostState.showSnackbar(getErrorMessage(event.error))
                }
            }
            SearchPluginsViewModel.Event.PluginsStateUpdated -> {
                snackbarHostState.currentSnackbarData?.dismiss()
                scope.launch {
                    snackbarHostState.showSnackbar(getString(Res.string.search_plugins_save_success))
                }
                viewModel.loadPlugins()
            }
            SearchPluginsViewModel.Event.PluginsInstalled -> {
                snackbarHostState.currentSnackbarData?.dismiss()
                scope.launch {
                    snackbarHostState.showSnackbar(getString(Res.string.search_plugins_install_success))
                }
                scope.launch {
                    delay(1000)
                    viewModel.loadPlugins()
                }
            }
            SearchPluginsViewModel.Event.PluginsUpdated -> {
                snackbarHostState.currentSnackbarData?.dismiss()
                scope.launch {
                    snackbarHostState.showSnackbar(getString(Res.string.search_plugins_update_success))
                }
                scope.launch {
                    delay(1000)
                    viewModel.loadPlugins()
                }
            }
        }
    }

    LaunchedEffect(plugins) {
        plugins?.let { pluginList ->
            pluginsToDelete.removeAll { name -> pluginList.none { it.name == name } }
            pluginsEnabledState.keys.removeAll { name -> pluginList.none { it.name == name } }
        }
    }

    var currentDialog by rememberSaveable(stateSaver = jsonSaver()) { mutableStateOf<Dialog?>(null) }
    when (currentDialog) {
        Dialog.InstallPlugin -> {
            InstallPluginDialog(
                onDismiss = { currentDialog = null },
                onConfirm = { sources ->
                    viewModel.installPlugin(sources)
                    currentDialog = null
                },
            )
        }
        null -> {}
    }

    val listState = rememberLazyListState()
    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets.safeDrawing.only(
            WindowInsetsSides.Horizontal + WindowInsetsSides.Top,
        ),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(Res.string.search_plugins),
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
                            title = stringResource(Res.string.search_plugins_action_save),
                            icon = Icons.Filled.Save,
                            onClick = { viewModel.savePlugins(pluginsEnabledState, pluginsToDelete) },
                            showAsAction = true,
                        ),
                        ActionMenuItem(
                            title = stringResource(Res.string.search_plugins_action_install_plugins),
                            icon = Icons.Filled.Add,
                            onClick = { currentDialog = Dialog.InstallPlugin },
                            showAsAction = true,
                        ),
                        ActionMenuItem(
                            title = stringResource(Res.string.search_plugins_action_update_plugins),
                            icon = Icons.Filled.Update,
                            onClick = { viewModel.updateAllPlugins() },
                            showAsAction = true,
                        ),
                    )

                    AppBarActions(items = actionMenuItems)
                },
                colors = listState.topAppBarColors(),
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
                .padding(innerPadding)
                .imePadding(),
        ) {
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(
                    items = plugins ?: emptyList(),
                    key = { it.name },
                ) { plugin ->
                    PluginItem(
                        plugin = plugin,
                        isEnabled = pluginsEnabledState[plugin.name] ?: plugin.isEnabled,
                        isDeleted = plugin.name in pluginsToDelete,
                        onEnabledChanged = { enabled ->
                            pluginsEnabledState[plugin.name] = enabled
                        },
                        onDeleteClicked = {
                            if (plugin.name in pluginsToDelete) {
                                pluginsToDelete.remove(plugin.name)
                            } else {
                                pluginsToDelete.add(plugin.name)
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

            val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
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
private fun PluginItem(
    plugin: Plugin,
    isEnabled: Boolean,
    isDeleted: Boolean,
    onEnabledChanged: (Boolean) -> Unit,
    onDeleteClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(
        modifier = modifier.alpha(if (isDeleted) 0.5f else 1f),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = CircleShape,
                        ),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Extension,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(20.dp),
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = plugin.fullName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = plugin.version,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Switch(
                    checked = isEnabled,
                    onCheckedChange = onEnabledChanged,
                    enabled = !isDeleted,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )

                IconButton(
                    onClick = onDeleteClicked,
                ) {
                    Icon(
                        imageVector = if (!isDeleted) Icons.Outlined.Delete else Icons.AutoMirrored.Outlined.Undo,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 4.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Link,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp),
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = plugin.url,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Serializable
private sealed class Dialog {
    @Serializable
    data object InstallPlugin : Dialog()
}

@Composable
private fun InstallPluginDialog(
    onDismiss: () -> Unit,
    onConfirm: (plugins: List<String>) -> Unit,
    modifier: Modifier = Modifier,
) {
    var text by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue()) }
    var error by rememberSaveable(
        stateSaver = stringResourceSaver(Res.string.search_plugins_cannot_be_empty),
    ) { mutableStateOf(null) }

    Dialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(Res.string.search_plugins_action_install_plugins)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = {
                    if (it.text != text.text) {
                        error = null
                    }
                    text = it
                },
                label = {
                    Text(
                        text = stringResource(Res.string.search_plugins_install_hint),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                isError = error != null,
                supportingText = error?.let { { Text(text = stringResource(it)) } },
                trailingIcon = error?.let { { Icon(imageVector = Icons.Filled.Error, contentDescription = null) } },
                maxLines = 10,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(),
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    if (text.text.isNotBlank()) {
                        onConfirm(text.text.split("\n"))
                    } else {
                        error = Res.string.search_plugins_cannot_be_empty
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
