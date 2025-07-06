package dev.bartuzen.qbitcontroller.ui.torrentlist

import androidx.compose.animation.EnterTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import dev.bartuzen.qbitcontroller.data.ServerManager
import dev.bartuzen.qbitcontroller.model.ServerConfig
import dev.bartuzen.qbitcontroller.ui.addtorrent.AddTorrentKeys
import dev.bartuzen.qbitcontroller.ui.addtorrent.AddTorrentScreen
import dev.bartuzen.qbitcontroller.ui.components.PlatformNavHost
import dev.bartuzen.qbitcontroller.ui.main.DeepLinkDestination
import dev.bartuzen.qbitcontroller.ui.main.Destination
import dev.bartuzen.qbitcontroller.ui.settings.addeditserver.AddEditServerKeys
import dev.bartuzen.qbitcontroller.ui.settings.addeditserver.AddEditServerResult
import dev.bartuzen.qbitcontroller.ui.settings.addeditserver.AddEditServerScreen
import dev.bartuzen.qbitcontroller.ui.settings.addeditserver.advanced.AdvancedServerSettingsKeys
import dev.bartuzen.qbitcontroller.ui.settings.addeditserver.advanced.AdvancedServerSettingsScreen
import dev.bartuzen.qbitcontroller.ui.torrent.TorrentKeys
import dev.bartuzen.qbitcontroller.ui.torrent.TorrentScreen
import dev.bartuzen.qbitcontroller.utils.DefaultTransitions
import dev.bartuzen.qbitcontroller.utils.getSerializable
import dev.bartuzen.qbitcontroller.utils.jsonSaver
import dev.bartuzen.qbitcontroller.utils.serializableNavType
import dev.bartuzen.qbitcontroller.utils.setSerializable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import org.koin.compose.koinInject
import kotlin.reflect.typeOf

@Composable
fun TorrentsNavHost(
    currentServer: ServerConfig?,
    navigateToStartFlow: Flow<Unit>,
    torrentsDeepLinkFlow: Flow<DeepLinkDestination>,
    onSelectServer: (serverId: Int) -> Unit,
    onShowNotificationPermission: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val navController = rememberNavController()

    // Using currentServer directly causes bugs beyond human comprehension
    val serverManager = koinInject<ServerManager>()
    var currentServerLocal by rememberSaveable(stateSaver = jsonSaver()) {
        mutableStateOf(serverManager.serversFlow.value.firstOrNull())
    }
    LaunchedEffect(currentServer) {
        currentServerLocal = currentServer
    }

    LaunchedEffect(navigateToStartFlow) {
        navigateToStartFlow.collectLatest {
            navController.popBackStack<Destination.TorrentList>(inclusive = false)
        }
    }

    LaunchedEffect(torrentsDeepLinkFlow) {
        torrentsDeepLinkFlow.collect { destination ->
            when (destination) {
                is DeepLinkDestination.TorrentList -> {
                    navController.popBackStack<Destination.TorrentList>(inclusive = false)
                }
                is DeepLinkDestination.Torrent -> {
                    navController.navigate(
                        Destination.Torrent(
                            serverId = destination.serverId,
                            torrentHash = destination.torrentHash,
                            torrentName = destination.torrentName,
                        ),
                    ) {
                        popUpTo<Destination.TorrentList>()
                    }
                }
                is DeepLinkDestination.AddTorrent -> {
                    navController.navigate(
                        Destination.AddTorrent(
                            torrentUrl = destination.torrentUrl,
                            torrentFileUris = destination.torrentFileUris,
                        ),
                    ) {
                        popUpTo<Destination.TorrentList>()
                    }
                }
                else -> {}
            }
        }
    }

    PlatformNavHost(
        navController = navController,
        startDestination = Destination.TorrentList,
        enterTransition = DefaultTransitions.Enter,
        exitTransition = DefaultTransitions.Exit,
        popEnterTransition = DefaultTransitions.PopEnter,
        popExitTransition = DefaultTransitions.PopExit,
        modifier = modifier,
    ) {
        composable<Destination.TorrentList>(
            enterTransition = { EnterTransition.None },
            exitTransition = DefaultTransitions.Exit,
            popEnterTransition = DefaultTransitions.PopEnter,
            popExitTransition = DefaultTransitions.PopExit,
        ) {
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

            TorrentListScreen(
                currentServer = currentServerLocal,
                addTorrentFlow = addTorrentChannel.receiveAsFlow(),
                deleteTorrentFlow = deleteTorrentChannel.receiveAsFlow(),
                onSelectServer = onSelectServer,
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

                    onSelectServer(serverId)

                    navController.navigateUp()
                },
            )
        }

        composable<Destination.Settings.AddEditServer> {
            val args = it.toRoute<Destination.Settings.AddEditServer>()

            val advancedSettingsChannel = remember { Channel<ServerConfig.AdvancedSettings>() }
            val advancedSettings = it.savedStateHandle.getSerializable<ServerConfig.AdvancedSettings?>(
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
                        onShowNotificationPermission()
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
                        ?.setSerializable(AdvancedServerSettingsKeys.AdvancedSettings, advancedSettings)
                },
            )
        }
    }
}
