package dev.bartuzen.qbitcontroller.ui.settings

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dagger.hilt.android.AndroidEntryPoint
import dev.bartuzen.qbitcontroller.R
import dev.bartuzen.qbitcontroller.data.Theme
import dev.bartuzen.qbitcontroller.model.SettingsEntry
import dev.bartuzen.qbitcontroller.ui.settings.addeditserver.AddEditServer
import dev.bartuzen.qbitcontroller.ui.settings.addeditserver.AddEditServerResult
import dev.bartuzen.qbitcontroller.ui.settings.common.ListPreference
import dev.bartuzen.qbitcontroller.ui.settings.common.PreferenceCategory
import dev.bartuzen.qbitcontroller.ui.settings.common.PreferenceItem
import dev.bartuzen.qbitcontroller.ui.theme.AppTheme
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val viewModel: SettingsViewModel = hiltViewModel()
            val theme by viewModel.themeFlow.collectAsState(Theme.SYSTEM_DEFAULT)
            val isDarkTheme = when (theme) {
                Theme.LIGHT -> false
                Theme.DARK -> true
                Theme.SYSTEM_DEFAULT -> isSystemInDarkTheme()
            }

            AppTheme(isDarkTheme) {
                val navController = rememberNavController()
                val scaffoldState = rememberScaffoldState()
                val coroutineScope = rememberCoroutineScope()

                NavHost(navController = navController, startDestination = "settings") {
                    composable("settings") {
                        MainSettings(
                            activity = this@SettingsActivity,
                            navController = navController,
                            scaffoldState = scaffoldState,
                            modifier = Modifier.fillMaxSize(),
                            viewModel = viewModel
                        )
                    }
                    composable(
                        "addeditserver?serverId={serverId}",
                        arguments = listOf(navArgument("serverId") { defaultValue = -1 })
                    ) { backStackEntry ->
                        backStackEntry.arguments?.getInt("serverId")?.let { serverId ->
                            AddEditServer(
                                serverId = serverId,
                                navController = navController,
                                onComplete = { result ->
                                    coroutineScope.launch {
                                        when (result) {
                                            AddEditServerResult.SERVER_CREATED -> {
                                                scaffoldState.snackbarHostState.showSnackbar(
                                                    getString(R.string.settings_server_add_success)
                                                )
                                            }
                                            AddEditServerResult.SERVER_EDITED -> {
                                                scaffoldState.snackbarHostState.showSnackbar(
                                                    getString(R.string.settings_server_edit_success)
                                                )
                                            }
                                            AddEditServerResult.SERVER_DELETED -> {
                                                scaffoldState.snackbarHostState.showSnackbar(
                                                    getString(R.string.settings_server_remove_success)
                                                )
                                            }
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MainSettings(
    activity: Activity,
    navController: NavController,
    modifier: Modifier = Modifier,
    scaffoldState: ScaffoldState = rememberScaffoldState(),
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val servers by viewModel.serversFlow.collectAsState(sortedMapOf())

    Scaffold(
        modifier = modifier.fillMaxSize(),
        scaffoldState = scaffoldState
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            val theme by viewModel.themeFlow.collectAsState(null)

            var showDialog by rememberSaveable { mutableStateOf(false) }
            val themeTitles = stringArrayResource(R.array.settings_theme_entries)

            if (showDialog) {
                ListPreference(
                    title = stringResource(R.string.settings_theme),
                    settingsEntries = listOf(
                        SettingsEntry(themeTitles[0], Theme.LIGHT),
                        SettingsEntry(themeTitles[1], Theme.DARK),
                        SettingsEntry(themeTitles[2], Theme.SYSTEM_DEFAULT),
                    ),
                    selectedEntry = theme,
                    onComplete = { selectedTheme ->
                        if (selectedTheme != null) {
                            viewModel.setTheme(selectedTheme)
                        }
                        showDialog = false
                    }
                )
            }

            SettingsTopAppBar(
                activity = activity,
                modifier = Modifier.fillMaxWidth()
            )
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                item {
                    PreferenceCategory(title = stringResource(R.string.settings_servers))
                }
                servers.forEach { (_, serverConfig) ->
                    item {
                        PreferenceItem(
                            name = serverConfig.name,
                            currentValue = serverConfig.host,
                            onClick = {
                                navController.navigate("addeditserver?serverId=${serverConfig.id}")
                            }
                        )
                    }
                }
                item {
                    PreferenceItem(
                        name = stringResource(R.string.settings_add_new_server),
                        currentValue = null,
                        onClick = {
                            navController.navigate("addeditserver")
                        }
                    )
                }
                item {
                    Divider(modifier = Modifier.fillMaxWidth())
                }
                item {
                    PreferenceCategory(title = stringResource(R.string.settings_other))
                }
                item {
                    PreferenceItem(
                        name = stringResource(R.string.settings_theme),
                        currentValue = theme?.ordinal?.let { themeTitles[it] },
                        onClick = {
                            showDialog = true
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsTopAppBar(
    activity: Activity,
    modifier: Modifier = Modifier
) {
    TopAppBar(
        modifier = modifier,
        title = {
            Text(stringResource(R.string.settings_title))
        },
        navigationIcon = {
            IconButton(onClick = { activity.finish() }) {
                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = null)
            }
        }
    )
}

@Preview
@Composable
fun SettingsPreview() {
    MainSettings(
        ComponentActivity(),
        rememberNavController()
    )
}