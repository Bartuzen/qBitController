package dev.bartuzen.qbitcontroller.ui.settings.addeditserver

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Save
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import dev.bartuzen.qbitcontroller.R
import dev.bartuzen.qbitcontroller.ui.settings.common.EditTextPreference
import dev.bartuzen.qbitcontroller.ui.settings.common.PreferenceItem
import kotlinx.coroutines.flow.collectLatest

enum class AddEditServerResult {
    SERVER_CREATED, SERVER_EDITED, SERVER_DELETED
}

@Composable
fun AddEditServer(
    serverId: Int,
    navController: NavController,
    onComplete: (result: AddEditServerResult) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AddEditServerViewModel = hiltViewModel()
) {
    val scaffoldState = rememberScaffoldState()
    val context = LocalContext.current

    LaunchedEffect(true) {
        if (viewModel.id == -1) {
            if (serverId != -1) {
                viewModel.updateDetails(serverId)
            }
        }

        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is AddEditServerViewModel.AddEditServerEvent.ServerCreated -> {
                    navController.popBackStack()
                    onComplete(AddEditServerResult.SERVER_CREATED)
                }
                is AddEditServerViewModel.AddEditServerEvent.ServerEdited -> {
                    navController.popBackStack()
                    onComplete(AddEditServerResult.SERVER_EDITED)
                }
                is AddEditServerViewModel.AddEditServerEvent.ServerDeleted -> {
                    navController.popBackStack()
                    onComplete(AddEditServerResult.SERVER_DELETED)
                }
                is AddEditServerViewModel.AddEditServerEvent.BlankFields -> {
                    scaffoldState.snackbarHostState.showSnackbar(
                        context.getString(R.string.settings_fill_blank_fields)
                    )
                }
            }
        }
    }

    Scaffold(
        scaffoldState = scaffoldState,
        modifier = modifier.fillMaxSize(),
        topBar = { AddEditServerAppBar(navController, viewModel) }
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            listOf(
                Triple(
                    R.string.settings_torrent_name,
                    viewModel.name,
                    { text: String -> viewModel.name = text }),
                Triple(
                    R.string.settings_host,
                    viewModel.host,
                    { text: String -> viewModel.host = text }),
                Triple(
                    R.string.settings_username,
                    viewModel.username,
                    { text: String -> viewModel.username = text }),
                Triple(
                    R.string.settings_password,
                    viewModel.password,
                    { text: String -> viewModel.password = text }),
            ).forEach { (titleId, currentValue, onConfirm) ->
                item {
                    var showDialog by rememberSaveable { mutableStateOf(false) }
                    val title = stringResource(titleId)

                    if (showDialog) {
                        EditTextPreference(
                            name = title,
                            initialText = currentValue,
                            hideText = titleId == R.string.settings_password,
                            onDialogClose = {
                                showDialog = false
                            },
                            onConfirm = { value ->
                                onConfirm(value)
                            }
                        )
                    }

                    PreferenceItem(
                        name = title,
                        currentValue = currentValue,
                        onClick = { showDialog = true },
                        hideText = titleId == R.string.settings_password
                    )
                }
            }
        }
    }
}

@Composable
fun AddEditServerAppBar(
    navController: NavController,
    viewModel: AddEditServerViewModel = viewModel()
) {
    val context = LocalContext.current

    var deleteServer by remember { mutableStateOf(false) }
    var saveServer by remember { mutableStateOf(false) }

    LaunchedEffect(deleteServer) {
        if (deleteServer) {
            viewModel.deleteServer()
            deleteServer = false
        }
    }

    LaunchedEffect(saveServer) {
        if (saveServer) {
            viewModel.saveServer(context.getString(R.string.settings_default_server_name))
            saveServer = false
        }
    }

    TopAppBar(
        title = { Text(stringResource(R.string.settings_title)) },
        navigationIcon = {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = null)
            }
        },
        actions = {
            if (viewModel.id != -1) {
                IconButton(onClick = { deleteServer = true }) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(R.string.menu_add_edit_server_delete)
                    )
                }
            }
            IconButton(onClick = { saveServer = true }) {
                Icon(
                    imageVector = Icons.Default.Save,
                    contentDescription = stringResource(R.string.menu_add_edit_server_save)
                )
            }
        }
    )
}