package dev.bartuzen.qbitcontroller.ui.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.RssFeed
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.RssFeed
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.DrawerDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dev.bartuzen.qbitcontroller.data.ServerManager
import dev.bartuzen.qbitcontroller.ui.log.LogsNavHost
import dev.bartuzen.qbitcontroller.ui.rss.RssNavHost
import dev.bartuzen.qbitcontroller.ui.search.SearchNavHost
import dev.bartuzen.qbitcontroller.ui.settings.SettingsNavHost
import dev.bartuzen.qbitcontroller.ui.theme.AppTheme
import dev.bartuzen.qbitcontroller.ui.theme.isDarkTheme
import dev.bartuzen.qbitcontroller.ui.torrentlist.TorrentsNavHost
import dev.bartuzen.qbitcontroller.utils.DefaultTransitions
import dev.bartuzen.qbitcontroller.utils.PersistentLaunchedEffect
import dev.bartuzen.qbitcontroller.utils.Platform
import dev.bartuzen.qbitcontroller.utils.calculateWindowSizeClass
import dev.bartuzen.qbitcontroller.utils.currentPlatform
import dev.bartuzen.qbitcontroller.utils.jsonSaver
import dev.bartuzen.qbitcontroller.utils.notificationPermissionLauncher
import dev.bartuzen.qbitcontroller.utils.stringResource
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.koin.compose.koinInject
import qbitcontroller.composeapp.generated.resources.Res
import qbitcontroller.composeapp.generated.resources.destination_logs
import qbitcontroller.composeapp.generated.resources.destination_rss
import qbitcontroller.composeapp.generated.resources.destination_search
import qbitcontroller.composeapp.generated.resources.destination_settings
import qbitcontroller.composeapp.generated.resources.destination_torrents

@Composable
fun MainScreen(navigationFlow: Flow<DeepLinkDestination>? = null) {
    AppTheme {
        var showNotificationPermission by remember { mutableStateOf(false) }
        val notificationPermissionLauncher = notificationPermissionLauncher()

        LaunchedEffect(showNotificationPermission) {
            if (showNotificationPermission) {
                notificationPermissionLauncher?.invoke()
                showNotificationPermission = false
            }
        }

        val serverManager = koinInject<ServerManager>()
        var currentServer by rememberSaveable(stateSaver = jsonSaver()) {
            mutableStateOf(serverManager.serversFlow.value.firstOrNull())
        }
        DisposableEffect(serverManager) {
            val serversFlow = serverManager.serversFlow
            val listener = serverManager.addServerListener(
                add = { serverConfig ->
                    if (serversFlow.value.size == 1) {
                        currentServer = serverConfig
                    }
                },
                remove = { serverConfig ->
                    if (currentServer?.id == serverConfig.id) {
                        currentServer = serversFlow.value.firstOrNull()
                    }
                },
                change = { serverConfig ->
                    if (currentServer?.id == serverConfig.id) {
                        currentServer = serverConfig
                    }
                },
            )

            onDispose {
                serverManager.removeServerListener(listener)
            }
        }

        val tabs = remember(currentServer == null) {
            listOf(
                BottomNavigationItem(
                    title = Res.string.destination_torrents,
                    enabled = true,
                    unselectedIcon = Icons.AutoMirrored.Outlined.List,
                    selectedIcon = Icons.AutoMirrored.Filled.List,
                    destination = NavHostDestination.Torrents,
                ),
                BottomNavigationItem(
                    title = Res.string.destination_search,
                    enabled = currentServer != null,
                    unselectedIcon = Icons.Outlined.Search,
                    selectedIcon = Icons.Filled.Search,
                    destination = NavHostDestination.Search,
                ),
                BottomNavigationItem(
                    title = Res.string.destination_rss,
                    enabled = currentServer != null,
                    unselectedIcon = Icons.Outlined.RssFeed,
                    selectedIcon = Icons.Filled.RssFeed,
                    destination = NavHostDestination.Rss,
                ),
                BottomNavigationItem(
                    title = Res.string.destination_logs,
                    enabled = currentServer != null,
                    unselectedIcon = Icons.Outlined.Description,
                    selectedIcon = Icons.Filled.Description,
                    destination = NavHostDestination.Logs,
                ),
                BottomNavigationItem(
                    title = Res.string.destination_settings,
                    enabled = true,
                    unselectedIcon = Icons.Outlined.Settings,
                    selectedIcon = Icons.Filled.Settings,
                    destination = NavHostDestination.Settings,
                ),
            )
        }

        val navigateToStartChannels = remember { List(tabs.size) { Channel<Unit>() } }

        val navController = rememberNavController()
        var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
        PersistentLaunchedEffect(selectedTabIndex) {
            try {
                navController.navigate(tabs[selectedTabIndex].destination) {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            } catch (_: IllegalStateException) {
            }
        }

        val torrentsDeepLinkChannel = remember { Channel<DeepLinkDestination>() }
        val settingsDeepLinkChannel = remember { Channel<DeepLinkDestination>() }
        LaunchedEffect(navigationFlow) {
            navigationFlow?.collect { destination ->
                when (destination) {
                    is DeepLinkDestination.TorrentList -> {
                        selectedTabIndex = 0
                        if (destination.serverId != null) {
                            serverManager.getServerOrNull(destination.serverId)?.let { currentServer = it }
                        }
                        torrentsDeepLinkChannel.send(destination)
                    }
                    is DeepLinkDestination.Torrent -> {
                        selectedTabIndex = 0
                        torrentsDeepLinkChannel.send(destination)
                        serverManager.getServerOrNull(destination.serverId)?.let { currentServer = it }
                    }
                    is DeepLinkDestination.AddTorrent -> {
                        selectedTabIndex = 0
                        torrentsDeepLinkChannel.send(destination)
                    }
                    DeepLinkDestination.Settings -> {
                        selectedTabIndex = 4
                        settingsDeepLinkChannel.send(destination)
                    }
                }
            }
        }

        val scope = rememberCoroutineScope()
        val widthSizeClass = calculateWindowSizeClass().widthSizeClass
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
        ) {
            AnimatedVisibility(
                visible = widthSizeClass != WindowWidthSizeClass.Compact,
                enter = expandHorizontally(),
                exit = shrinkHorizontally(),
            ) {
                Box(modifier = Modifier.width(IntrinsicSize.Max)) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        modifier = Modifier
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState()),
                        windowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Start),
                    ) {
                        Spacer(modifier = Modifier.windowInsetsTopHeight(WindowInsets.safeDrawing))
                        Spacer(modifier = Modifier.weight(1f))

                        tabs.forEachIndexed { index, item ->
                            NavigationRailItem(
                                selected = index == selectedTabIndex,
                                onClick = {
                                    if (selectedTabIndex != index) {
                                        selectedTabIndex = index
                                    } else {
                                        scope.launch {
                                            navigateToStartChannels[selectedTabIndex].send(Unit)
                                        }
                                    }
                                },
                                label = { Text(text = stringResource(item.title)) },
                                icon = {
                                    Icon(
                                        imageVector = if (index == selectedTabIndex) {
                                            item.selectedIcon
                                        } else {
                                            item.unselectedIcon
                                        },
                                        contentDescription = null,
                                    )
                                },
                                enabled = item.enabled,
                            )
                        }

                        Spacer(modifier = Modifier.weight(1f))
                        Spacer(modifier = Modifier.windowInsetsBottomHeight(WindowInsets.safeDrawing))
                    }

                    Spacer(
                        modifier = Modifier
                            .windowInsetsTopHeight(WindowInsets.safeDrawing)
                            .fillMaxWidth()
                            .padding(WindowInsets.safeDrawing.only(WindowInsetsSides.Start).asPaddingValues())
                            .background(
                                DrawerDefaults.modalContainerColor.copy(
                                    alpha = if (isDarkTheme()) 0.5f else 0.9f,
                                ),
                            ),
                    )
                }
            }

            Scaffold(
                contentWindowInsets = WindowInsets.safeDrawing,
                bottomBar = {
                    AnimatedVisibility(
                        visible = widthSizeClass == WindowWidthSizeClass.Compact,
                        enter = expandVertically(),
                        exit = shrinkVertically(),
                    ) {
                        NavigationBar {
                            tabs.forEachIndexed { index, item ->
                                NavigationBarItem(
                                    selected = index == selectedTabIndex,
                                    onClick = {
                                        if (selectedTabIndex != index) {
                                            selectedTabIndex = index
                                        } else {
                                            scope.launch {
                                                navigateToStartChannels[selectedTabIndex].send(Unit)
                                            }
                                        }
                                    },
                                    label = { Text(text = stringResource(item.title)) },
                                    icon = {
                                        Icon(
                                            imageVector = if (index == selectedTabIndex) {
                                                item.selectedIcon
                                            } else {
                                                item.unselectedIcon
                                            },
                                            contentDescription = null,
                                        )
                                    },
                                    enabled = item.enabled,
                                )
                            }
                        }
                    }
                },
            ) { innerPadding ->
                val padding = if (widthSizeClass == WindowWidthSizeClass.Compact) {
                    PaddingValues(bottom = innerPadding.calculateBottomPadding())
                } else {
                    PaddingValues()
                }

                NavHost(
                    navController = navController,
                    startDestination = NavHostDestination.Torrents,
                    enterTransition = DefaultTransitions.NavBar.Enter,
                    exitTransition = DefaultTransitions.NavBar.Exit,
                    popEnterTransition = DefaultTransitions.NavBar.PopEnter,
                    popExitTransition = DefaultTransitions.NavBar.PopExit,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .consumeWindowInsets(padding)
                        .run {
                            if (widthSizeClass != WindowWidthSizeClass.Compact) {
                                consumeWindowInsets(WindowInsets.safeDrawing.only(WindowInsetsSides.Start))
                            } else {
                                this
                            }
                        },
                ) {
                    composable<NavHostDestination.Torrents> {
                        TorrentsNavHost(
                            currentServer = currentServer,
                            navigateToStartFlow = navigateToStartChannels[0].receiveAsFlow(),
                            torrentsDeepLinkFlow = torrentsDeepLinkChannel.receiveAsFlow(),
                            onSelectServer = { currentServer = serverManager.getServer(it) },
                            onShowNotificationPermission = { showNotificationPermission = true },
                        )
                    }

                    composable<NavHostDestination.Search> {
                        BackHandler {
                            if (currentPlatform != Platform.Mobile.IOS) {
                                selectedTabIndex = 0
                            }
                        }

                        SearchNavHost(
                            serverConfig = currentServer,
                            navigateToStartFlow = navigateToStartChannels[1].receiveAsFlow(),
                        )
                    }

                    composable<NavHostDestination.Rss> {
                        BackHandler {
                            if (currentPlatform != Platform.Mobile.IOS) {
                                selectedTabIndex = 0
                            }
                        }

                        RssNavHost(
                            serverConfig = currentServer,
                            navigateToStartFlow = navigateToStartChannels[2].receiveAsFlow(),
                        )
                    }

                    composable<NavHostDestination.Logs> {
                        BackHandler {
                            if (currentPlatform != Platform.Mobile.IOS) {
                                selectedTabIndex = 0
                            }
                        }

                        LogsNavHost(
                            serverConfig = currentServer,
                            navigateToStartFlow = navigateToStartChannels[3].receiveAsFlow(),
                        )
                    }

                    composable<NavHostDestination.Settings> {
                        BackHandler {
                            if (currentPlatform != Platform.Mobile.IOS) {
                                selectedTabIndex = 0
                            }
                        }

                        SettingsNavHost(
                            navigateToStartFlow = navigateToStartChannels[4].receiveAsFlow(),
                            onShowNotificationPermission = { showNotificationPermission = true },
                            settingsDeepLinkFlow = settingsDeepLinkChannel.receiveAsFlow(),
                        )
                    }
                }
            }
        }
    }
}

private data class BottomNavigationItem(
    val title: StringResource,
    val enabled: Boolean,
    val unselectedIcon: ImageVector,
    val selectedIcon: ImageVector,
    val destination: NavHostDestination,
)
