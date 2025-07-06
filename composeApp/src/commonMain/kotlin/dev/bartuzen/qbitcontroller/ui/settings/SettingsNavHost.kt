package dev.bartuzen.qbitcontroller.ui.settings

import androidx.compose.animation.EnterTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import dev.bartuzen.qbitcontroller.model.ServerConfig
import dev.bartuzen.qbitcontroller.ui.components.PlatformNavHost
import dev.bartuzen.qbitcontroller.ui.main.DeepLinkDestination
import dev.bartuzen.qbitcontroller.ui.main.Destination
import dev.bartuzen.qbitcontroller.ui.settings.addeditserver.AddEditServerKeys
import dev.bartuzen.qbitcontroller.ui.settings.addeditserver.AddEditServerResult
import dev.bartuzen.qbitcontroller.ui.settings.addeditserver.AddEditServerScreen
import dev.bartuzen.qbitcontroller.ui.settings.addeditserver.advanced.AdvancedServerSettingsKeys
import dev.bartuzen.qbitcontroller.ui.settings.addeditserver.advanced.AdvancedServerSettingsScreen
import dev.bartuzen.qbitcontroller.ui.settings.appearance.AppearanceSettingsScreen
import dev.bartuzen.qbitcontroller.ui.settings.general.GeneralSettingsScreen
import dev.bartuzen.qbitcontroller.ui.settings.network.NetworkSettingsScreen
import dev.bartuzen.qbitcontroller.ui.settings.servers.ServersScreen
import dev.bartuzen.qbitcontroller.utils.DefaultTransitions
import dev.bartuzen.qbitcontroller.utils.getSerializable
import dev.bartuzen.qbitcontroller.utils.serializableNavType
import dev.bartuzen.qbitcontroller.utils.setSerializable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlin.reflect.typeOf

@Composable
fun SettingsNavHost(
    navigateToStartFlow: Flow<Unit>,
    settingsDeepLinkFlow: Flow<DeepLinkDestination>,
    onShowNotificationPermission: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val navController = rememberNavController()
    LaunchedEffect(settingsDeepLinkFlow) {
        settingsDeepLinkFlow.collect { destination ->
            when (destination) {
                DeepLinkDestination.Settings -> {
                    navController.popBackStack<Destination.Settings.Main>(inclusive = false)
                }
                else -> {}
            }
        }
    }

    LaunchedEffect(navigateToStartFlow) {
        navigateToStartFlow.collectLatest {
            navController.popBackStack<Destination.Settings.Main>(inclusive = false)
        }
    }

    PlatformNavHost(
        navController = navController,
        startDestination = Destination.Settings.Main,
        enterTransition = DefaultTransitions.Enter,
        exitTransition = DefaultTransitions.Exit,
        popEnterTransition = DefaultTransitions.PopEnter,
        popExitTransition = DefaultTransitions.PopExit,
        modifier = modifier,
    ) {
        composable<Destination.Settings.Main>(
            enterTransition = { EnterTransition.None },
            exitTransition = DefaultTransitions.Exit,
            popEnterTransition = DefaultTransitions.PopEnter,
            popExitTransition = DefaultTransitions.PopExit,
        ) {
            SettingsScreen(
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
