package dev.bartuzen.qbitcontroller.ui.main

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import dev.bartuzen.qbitcontroller.data.SettingsManager
import dev.bartuzen.qbitcontroller.data.Theme
import dev.bartuzen.qbitcontroller.model.ServerConfig
import dev.bartuzen.qbitcontroller.ui.addtorrent.AddTorrentKeys
import dev.bartuzen.qbitcontroller.ui.addtorrent.AddTorrentScreen
import dev.bartuzen.qbitcontroller.ui.log.LogScreen
import dev.bartuzen.qbitcontroller.ui.rss.articles.RssArticlesKeys
import dev.bartuzen.qbitcontroller.ui.rss.articles.RssArticlesScreen
import dev.bartuzen.qbitcontroller.ui.rss.editrule.EditRssRuleScreen
import dev.bartuzen.qbitcontroller.ui.rss.feeds.RssFeedsScreen
import dev.bartuzen.qbitcontroller.ui.rss.rules.RssRulesScreen
import dev.bartuzen.qbitcontroller.ui.search.plugins.SearchPluginsScreen
import dev.bartuzen.qbitcontroller.ui.search.result.SearchResultScreen
import dev.bartuzen.qbitcontroller.ui.search.start.SearchStartScreen
import dev.bartuzen.qbitcontroller.ui.settings.SettingsScreen
import dev.bartuzen.qbitcontroller.ui.settings.addeditserver.AddEditServerKeys
import dev.bartuzen.qbitcontroller.ui.settings.addeditserver.AddEditServerResult
import dev.bartuzen.qbitcontroller.ui.settings.addeditserver.AddEditServerScreen
import dev.bartuzen.qbitcontroller.ui.settings.addeditserver.advanced.AdvancedServerSettingsKeys
import dev.bartuzen.qbitcontroller.ui.settings.addeditserver.advanced.AdvancedServerSettingsScreen
import dev.bartuzen.qbitcontroller.ui.settings.appearance.AppearanceSettingsScreen
import dev.bartuzen.qbitcontroller.ui.settings.general.GeneralSettingsScreen
import dev.bartuzen.qbitcontroller.ui.settings.network.NetworkSettingsScreen
import dev.bartuzen.qbitcontroller.ui.settings.servers.ServersScreen
import dev.bartuzen.qbitcontroller.ui.theme.AppTheme
import dev.bartuzen.qbitcontroller.ui.torrent.TorrentKeys
import dev.bartuzen.qbitcontroller.ui.torrent.TorrentScreen
import dev.bartuzen.qbitcontroller.ui.torrentlist.TorrentListScreen
import dev.bartuzen.qbitcontroller.utils.notificationPermissionLauncher
import dev.bartuzen.qbitcontroller.utils.serializableNavType
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import org.koin.compose.koinInject
import kotlin.reflect.typeOf

@Composable
fun MainScreen(navController: NavHostController = rememberNavController(), serverIdChannel: Channel<Int> = Channel<Int>()) {
    val settingsManager = koinInject<SettingsManager>()
    val theme by settingsManager.theme.flow.collectAsStateWithLifecycle()
    val darkTheme = when (theme) {
        Theme.LIGHT -> false
        Theme.DARK -> true
        Theme.SYSTEM_DEFAULT -> isSystemInDarkTheme()
    }

    AppTheme(darkTheme = darkTheme) {
        var showNotificationPermission by remember { mutableStateOf(false) }
        val notificationPermissionLauncher = notificationPermissionLauncher()

        LaunchedEffect(showNotificationPermission) {
            if (showNotificationPermission) {
                notificationPermissionLauncher?.invoke()
                showNotificationPermission = false
            }
        }

        val transitionDuration = 400
        NavHost(
            navController = navController,
            startDestination = Destination.TorrentList,
            enterTransition = {
                slideInHorizontally(
                    initialOffsetX = { it / 10 },
                    animationSpec = tween(transitionDuration),
                ) + fadeIn(animationSpec = tween(transitionDuration))
            },
            exitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { -it / 10 },
                    animationSpec = tween(transitionDuration),
                ) + fadeOut(animationSpec = tween(transitionDuration))
            },
            popEnterTransition = {
                slideInHorizontally(
                    initialOffsetX = { -it / 10 },
                    animationSpec = tween(transitionDuration),
                ) + fadeIn(animationSpec = tween(transitionDuration))
            },
            popExitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { it / 10 },
                    animationSpec = tween(transitionDuration),
                ) + fadeOut(animationSpec = tween(transitionDuration))
            },
        ) {
            composable<Destination.TorrentList> {
                val addTorrentChannel = remember { Channel<Int>() }
                val torrentAdded = it.savedStateHandle.get<Int?>(AddTorrentKeys.TorrentAdded)
                LaunchedEffect(torrentAdded) {
                    if (torrentAdded != null) {
                        addTorrentChannel.send(torrentAdded)
                        it.savedStateHandle.remove<Int?>(AddTorrentKeys.TorrentAdded)
                    }
                }

                val deleteTorrentChannel = remember { Channel<Unit>() }
                val torrentDeleted = it.savedStateHandle.get<Boolean?>(TorrentKeys.TorrentDeleted)
                LaunchedEffect(torrentDeleted) {
                    if (torrentDeleted != null) {
                        deleteTorrentChannel.send(Unit)
                        it.savedStateHandle.remove<Boolean?>(TorrentKeys.TorrentDeleted)
                    }
                }

                val isScreenActive = navController.currentDestination == it.destination
                TorrentListScreen(
                    isScreenActive = isScreenActive,
                    serverIdFlow = serverIdChannel.receiveAsFlow(),
                    addTorrentFlow = addTorrentChannel.receiveAsFlow(),
                    deleteTorrentFlow = deleteTorrentChannel.receiveAsFlow(),
                    onNavigateToTorrent = { serverId, torrentHash, torrentName ->
                        navController.navigate(Destination.Torrent(serverId, torrentHash, torrentName))
                    },
                    onNavigateToAddTorrent = { initialServerId ->
                        navController.navigate(Destination.AddTorrent(initialServerId))
                    },
                    onNavigateToRss = { serverId ->
                        navController.navigate(Destination.Rss.Feeds(serverId))
                    },
                    onNavigateToSearch = { serverId ->
                        navController.navigate(Destination.Search.Start(serverId))
                    },
                    onNavigateToLog = { serverId ->
                        navController.navigate(Destination.Log(serverId))
                    },
                    onNavigateToSettings = {
                        navController.navigate(Destination.Settings.Main)
                    },
                    onNavigateToAddEditServer = { serverId ->
                        navController.navigate(Destination.Settings.AddEditServer(serverId))
                    },
                )
            }

            composable<Destination.Torrent> {
                val args = it.toRoute<Destination.Torrent>()
                TorrentScreen(
                    serverId = args.serverId,
                    torrentHash = args.torrentHash,
                    torrentName = args.torrentName,
                    onNavigateBack = { navController.navigateUp() },
                    onDeleteTorrent = {
                        navController.previousBackStackEntry
                            ?.savedStateHandle
                            ?.set(TorrentKeys.TorrentDeleted, true)

                        navController.navigateUp()
                    },
                )
            }

            composable<Destination.AddTorrent> {
                val args = it.toRoute<Destination.AddTorrent>()
                AddTorrentScreen(
                    initialServerId = args.initialServerId,
                    torrentUrl = args.torrentUrl,
                    torrentFileUris = args.torrentFileUris,
                    onNavigateBack = { navController.navigateUp() },
                    onAddTorrent = { serverId ->
                        navController.previousBackStackEntry
                            ?.savedStateHandle
                            ?.set(AddTorrentKeys.TorrentAdded, serverId)

                        navController.navigateUp()
                    },
                )
            }

            composable<Destination.Rss.Feeds> {
                val args = it.toRoute<Destination.Rss.Feeds>()

                val articleUpdate = remember { Channel<Unit>() }
                val isUpdated = it.savedStateHandle.get<Boolean?>(RssArticlesKeys.IsUpdated)
                LaunchedEffect(isUpdated) {
                    if (isUpdated != null) {
                        articleUpdate.send(Unit)
                        it.savedStateHandle.remove<Boolean?>(RssArticlesKeys.IsUpdated)
                    }
                }

                RssFeedsScreen(
                    serverId = args.serverId,
                    articleUpdateFlow = articleUpdate.receiveAsFlow(),
                    onNavigateBack = { navController.navigateUp() },
                    onNavigateToArticles = { feedPath, uid ->
                        navController.navigate(Destination.Rss.Articles(args.serverId, feedPath, uid))
                    },
                    onNavigateToRules = {
                        navController.navigate(Destination.Rss.Rules(args.serverId))
                    },
                )
            }

            composable<Destination.Rss.Articles> {
                val args = it.toRoute<Destination.Rss.Articles>()

                val addTorrentChannel = remember { Channel<Unit>() }
                val torrentAdded = it.savedStateHandle.get<Int?>(AddTorrentKeys.TorrentAdded)
                LaunchedEffect(torrentAdded) {
                    if (torrentAdded != null) {
                        addTorrentChannel.send(Unit)
                        it.savedStateHandle.remove<Int?>(AddTorrentKeys.TorrentAdded)
                    }
                }

                RssArticlesScreen(
                    serverId = args.serverId,
                    feedPath = args.feedPath,
                    uid = args.uid,
                    addTorrentFlow = addTorrentChannel.receiveAsFlow(),
                    onFeedPathChange = {
                        navController.previousBackStackEntry
                            ?.savedStateHandle
                            ?.set(RssArticlesKeys.IsUpdated, true)
                    },
                    onNavigateBack = { navController.navigateUp() },
                    onNavigateToAddTorrent = { torrentUrl ->
                        navController.navigate(Destination.AddTorrent(args.serverId, torrentUrl))
                    },
                )
            }

            composable<Destination.Rss.Rules> {
                val args = it.toRoute<Destination.Rss.Rules>()
                RssRulesScreen(
                    serverId = args.serverId,
                    onNavigateBack = { navController.navigateUp() },
                    onNavigateToEditRule = { ruleName ->
                        navController.navigate(Destination.Rss.EditRule(args.serverId, ruleName))
                    },
                )
            }

            composable<Destination.Rss.EditRule> {
                val args = it.toRoute<Destination.Rss.EditRule>()
                EditRssRuleScreen(
                    serverId = args.serverId,
                    ruleName = args.ruleName,
                    onNavigateBack = { navController.navigateUp() },
                )
            }

            composable<Destination.Search.Start> {
                val args = it.toRoute<Destination.Search.Start>()
                SearchStartScreen(
                    serverId = args.serverId,
                    onNavigateBack = { navController.navigateUp() },
                    onNavigateToSearchResult = { searchQuery, category, plugins ->
                        navController.navigate(
                            Destination.Search.Result(args.serverId, searchQuery, category, plugins),
                        )
                    },
                    onNavigateToPlugins = { navController.navigate(Destination.Search.Plugins(args.serverId)) },
                )
            }

            composable<Destination.Search.Result> {
                val args = it.toRoute<Destination.Search.Result>()

                val addTorrentChannel = remember { Channel<Unit>() }
                val torrentAdded = it.savedStateHandle.get<Int?>(AddTorrentKeys.TorrentAdded)
                LaunchedEffect(torrentAdded) {
                    if (torrentAdded != null) {
                        addTorrentChannel.send(Unit)
                        it.savedStateHandle.remove<Int?>(AddTorrentKeys.TorrentAdded)
                    }
                }

                SearchResultScreen(
                    serverId = args.serverId,
                    searchQuery = args.searchQuery,
                    category = args.category,
                    plugins = args.plugins,
                    addTorrentFlow = addTorrentChannel.receiveAsFlow(),
                    onNavigateBack = { navController.navigateUp() },
                    onNavigateToAddTorrent = { torrentUrl ->
                        navController.navigate(Destination.AddTorrent(args.serverId, torrentUrl))
                    },
                )
            }

            composable<Destination.Search.Plugins> {
                val args = it.toRoute<Destination.Search.Plugins>()
                SearchPluginsScreen(
                    serverId = args.serverId,
                    onNavigateBack = { navController.navigateUp() },
                )
            }

            composable<Destination.Log> {
                val args = it.toRoute<Destination.Log>()
                LogScreen(
                    serverId = args.serverId,
                    onNavigateBack = { navController.navigateUp() },
                )
            }

            composable<Destination.Settings.Main> {
                SettingsScreen(
                    onNavigateBack = { navController.navigateUp() },
                    onNavigateToServerSettings = {
                        navController.navigate(Destination.Settings.Server)
                    },
                    onNavigateToGeneralSettings = {
                        navController.navigate(Destination.Settings.General)
                    },
                    onNavigateToAppearanceSettings = {
                        navController.navigate(Destination.Settings.Appearance)
                    },
                    onNavigateToNetworkSettings = {
                        navController.navigate(Destination.Settings.Network)
                    },
                )
            }

            composable<Destination.Settings.Server> {
                val addEditServerChannel = remember { Channel<AddEditServerResult>() }
                val addEditServerResult = it.savedStateHandle.get<AddEditServerResult>(AddEditServerKeys.Result)
                LaunchedEffect(addEditServerResult) {
                    if (addEditServerResult != null) {
                        addEditServerChannel.send(addEditServerResult)
                        it.savedStateHandle.remove<AddEditServerResult>(AddEditServerKeys.Result)
                    }
                }

                ServersScreen(
                    addEditServerFlow = addEditServerChannel.receiveAsFlow(),
                    onNavigateBack = { navController.navigateUp() },
                    onNavigateToAddEditServer = { serverId ->
                        navController.navigate(Destination.Settings.AddEditServer(serverId))
                    },
                )
            }

            composable<Destination.Settings.AddEditServer> {
                val args = it.toRoute<Destination.Settings.AddEditServer>()

                val advancedSettingsChannel = remember { Channel<ServerConfig.AdvancedSettings>() }
                val advancedSettings = it.savedStateHandle.get<ServerConfig.AdvancedSettings?>(
                    AdvancedServerSettingsKeys.AdvancedSettings,
                )
                LaunchedEffect(advancedSettings) {
                    if (advancedSettings != null) {
                        advancedSettingsChannel.send(advancedSettings)

                        it.savedStateHandle.remove<ServerConfig.AdvancedSettings?>(
                            AdvancedServerSettingsKeys.AdvancedSettings,
                        )
                    }
                }

                AddEditServerScreen(
                    serverId = args.serverId,
                    advancedSettingsFlow = advancedSettingsChannel.receiveAsFlow(),
                    onNavigateBack = { result ->
                        navController.previousBackStackEntry
                            ?.savedStateHandle
                            ?.set(AddEditServerKeys.Result, result)

                        navController.navigateUp()

                        if (result == AddEditServerResult.Add) {
                            showNotificationPermission = true
                        }
                    },
                    onNavigateToAdvancedSettings = { advancedSettings ->
                        navController.navigate(Destination.Settings.Advanced(advancedSettings))
                    },
                )
            }

            composable<Destination.Settings.Advanced>(
                typeMap = mapOf(
                    typeOf<ServerConfig.AdvancedSettings>() to
                        serializableNavType<ServerConfig.AdvancedSettings>(),
                ),
            ) {
                val args = it.toRoute<Destination.Settings.Advanced>()
                AdvancedServerSettingsScreen(
                    advancedSettings = args.advancedSettings,
                    onNavigateBack = { navController.navigateUp() },
                    onUpdate = { advancedSettings ->
                        navController.previousBackStackEntry
                            ?.savedStateHandle
                            ?.set(AdvancedServerSettingsKeys.AdvancedSettings, advancedSettings)
                    },
                )
            }

            composable<Destination.Settings.General> {
                GeneralSettingsScreen(
                    onNavigateBack = { navController.navigateUp() },
                )
            }

            composable<Destination.Settings.Appearance> {
                AppearanceSettingsScreen(
                    onNavigateBack = { navController.navigateUp() },
                )
            }

            composable<Destination.Settings.Network> {
                NetworkSettingsScreen(
                    onNavigateBack = { navController.navigateUp() },
                )
            }
        }
    }
}
