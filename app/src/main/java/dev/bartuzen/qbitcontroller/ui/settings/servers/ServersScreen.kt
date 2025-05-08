package dev.bartuzen.qbitcontroller.ui.settings.servers

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.bartuzen.qbitcontroller.R
import dev.bartuzen.qbitcontroller.ui.components.ActionMenuItem
import dev.bartuzen.qbitcontroller.ui.components.AppBarActions
import dev.bartuzen.qbitcontroller.ui.components.EmptyListMessage
import dev.bartuzen.qbitcontroller.ui.components.SwipeableSnackbarHost
import dev.bartuzen.qbitcontroller.ui.settings.addeditserver.AddEditServerResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import me.zhanghai.compose.preference.Preference
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ServersScreen(
    addEditServerFlow: Flow<AddEditServerResult>,
    onNavigateBack: () -> Unit,
    onNavigateToAddEditServer: (serverId: Int?) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ServersViewModel = koinViewModel(),
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val servers by viewModel.servers.collectAsStateWithLifecycle()

    LaunchedEffect(addEditServerFlow) {
        addEditServerFlow.collectLatest { result ->
            snackbarHostState.currentSnackbarData?.dismiss()
            val text = when (result) {
                AddEditServerResult.Add -> R.string.settings_server_add_success
                AddEditServerResult.Edit -> R.string.settings_server_edit_success
                AddEditServerResult.Delete -> R.string.settings_server_remove_success
            }.let { context.getString(it) }
            scope.launch {
                snackbarHostState.showSnackbar(text)
            }
        }
    }

    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings_servers_add_server),
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
                    val items = listOf(
                        ActionMenuItem(
                            title = stringResource(R.string.settings_servers_add_server),
                            icon = Icons.Filled.Add,
                            onClick = { onNavigateToAddEditServer(null) },
                            showAsAction = true,
                        ),
                    )

                    AppBarActions(items = items)
                },
            )
        },
        snackbarHost = { SwipeableSnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        LazyColumn(
            contentPadding = innerPadding,
            modifier = Modifier.fillMaxSize(),
        ) {
            items(
                items = servers,
                key = { it.id },
            ) { serverConfig ->
                Preference(
                    title = serverConfig.name?.let { { Text(text = it) } },
                    summary = { Text(text = serverConfig.visibleUrl) },
                    onClick = { onNavigateToAddEditServer(serverConfig.id) },
                    modifier = Modifier.animateItem(),
                )
            }
        }

        AnimatedContent(
            targetState = servers.isEmpty(),
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize(),
        ) { noServersFound ->
            if (noServersFound) {
                EmptyListMessage(
                    icon = Icons.Filled.LinkOff,
                    title = stringResource(id = R.string.settings_servers_no_server_configured),
                    actionButton = {
                        Button(onClick = { onNavigateToAddEditServer(null) }) {
                            Text(text = stringResource(id = R.string.settings_servers_add_server))
                        }
                    },
                )
            }
        }
    }
}
