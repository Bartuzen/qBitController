package dev.bartuzen.qbitcontroller.ui.settings.addeditserver

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.bartuzen.qbitcontroller.model.ServerConfig
import dev.bartuzen.qbitcontroller.model.ServerConfig.AdvancedSettings
import dev.bartuzen.qbitcontroller.ui.components.ActionMenuItem
import dev.bartuzen.qbitcontroller.ui.components.AppBarActions
import dev.bartuzen.qbitcontroller.ui.components.SwipeableSnackbarHost
import dev.bartuzen.qbitcontroller.utils.EventEffect
import dev.bartuzen.qbitcontroller.utils.fillWidthOfParent
import dev.bartuzen.qbitcontroller.utils.getErrorMessage
import dev.bartuzen.qbitcontroller.utils.getString
import dev.bartuzen.qbitcontroller.utils.jsonSaver
import dev.bartuzen.qbitcontroller.utils.stringResource
import dev.bartuzen.qbitcontroller.utils.stringResourceSaver
import dev.bartuzen.qbitcontroller.utils.topAppBarColors
import io.ktor.http.parseUrl
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import qbitcontroller.composeapp.generated.resources.Res
import qbitcontroller.composeapp.generated.resources.error_required_field
import qbitcontroller.composeapp.generated.resources.settings_server_action_advanced_settings
import qbitcontroller.composeapp.generated.resources.settings_server_action_delete
import qbitcontroller.composeapp.generated.resources.settings_server_action_save
import qbitcontroller.composeapp.generated.resources.settings_server_connection_success
import qbitcontroller.composeapp.generated.resources.settings_server_credentials
import qbitcontroller.composeapp.generated.resources.settings_server_invalid_url
import qbitcontroller.composeapp.generated.resources.settings_server_name
import qbitcontroller.composeapp.generated.resources.settings_server_password
import qbitcontroller.composeapp.generated.resources.settings_server_server_info
import qbitcontroller.composeapp.generated.resources.settings_server_test_configuration
import qbitcontroller.composeapp.generated.resources.settings_server_title_add
import qbitcontroller.composeapp.generated.resources.settings_server_title_edit
import qbitcontroller.composeapp.generated.resources.settings_server_url
import qbitcontroller.composeapp.generated.resources.settings_server_url_examples
import qbitcontroller.composeapp.generated.resources.settings_server_username

object AddEditServerKeys {
    const val Result = "addEditServer.result"
}

@Composable
fun AddEditServerScreen(
    serverId: Int?,
    advancedSettingsFlow: Flow<AdvancedSettings>,
    onNavigateBack: (result: AddEditServerResult?) -> Unit,
    onNavigateToAdvancedSettings: (advancedSettings: AdvancedSettings) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AddEditServerViewModel = koinViewModel(parameters = { parametersOf(serverId) }),
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val isTesting by viewModel.isTesting.collectAsStateWithLifecycle()

    val serverConfig = viewModel.serverConfig
    var name by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(serverConfig?.name ?: "", TextRange(Int.MAX_VALUE)))
    }
    var url by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(serverConfig?.url ?: "", TextRange(Int.MAX_VALUE)))
    }
    var username by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(serverConfig?.username ?: "", TextRange(Int.MAX_VALUE)))
    }
    var password by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(serverConfig?.password ?: "", TextRange(Int.MAX_VALUE)))
    }

    var urlError by rememberSaveable(
        stateSaver = stringResourceSaver(Res.string.error_required_field, Res.string.settings_server_invalid_url),
    ) { mutableStateOf(null) }

    var advancedSettings by rememberSaveable(stateSaver = jsonSaver()) {
        mutableStateOf(serverConfig?.advanced ?: AdvancedSettings())
    }

    fun validateAndGetServerConfig(): ServerConfig? {
        var isValid = true

        val urlWithProtocol = if (!url.text.contains("://")) "http://${url.text}" else url.text
        urlError = if (url.text.isBlank()) {
            isValid = false
            Res.string.error_required_field
        } else if (parseUrl(urlWithProtocol) == null || !isPlatformUrlValid(urlWithProtocol)) {
            isValid = false
            Res.string.settings_server_invalid_url
        } else {
            null
        }

        if (!isValid) {
            return null
        }

        return ServerConfig(
            id = serverId ?: -1,
            name = name.text.ifEmpty { null },
            url = url.text,
            username = username.text.ifEmpty { null },
            password = password.text.ifEmpty { null },
            advanced = advancedSettings,
        )
    }

    EventEffect(viewModel.eventFlow) { event ->
        when (event) {
            is AddEditServerViewModel.Event.TestFailure -> {
                snackbarHostState.currentSnackbarData?.dismiss()
                scope.launch {
                    snackbarHostState.showSnackbar(getErrorMessage(event.error))
                }
            }
            AddEditServerViewModel.Event.TestSuccess -> {
                snackbarHostState.currentSnackbarData?.dismiss()
                scope.launch {
                    snackbarHostState.showSnackbar(getString(Res.string.settings_server_connection_success))
                }
            }
        }
    }

    LaunchedEffect(advancedSettingsFlow) {
        advancedSettingsFlow.collectLatest {
            advancedSettings = it
        }
    }

    val softwareKeyboardController = LocalSoftwareKeyboardController.current
    val scrollState = rememberScrollState()
    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (serverId == null) {
                            stringResource(Res.string.settings_server_title_add)
                        } else {
                            stringResource(Res.string.settings_server_title_edit)
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            softwareKeyboardController?.hide()
                            onNavigateBack(null)
                        },
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                        )
                    }
                },
                actions = {
                    val actionMenuItems = listOfNotNull(
                        ActionMenuItem(
                            title = stringResource(Res.string.settings_server_action_advanced_settings),
                            icon = Icons.Filled.Settings,
                            onClick = { onNavigateToAdvancedSettings(advancedSettings) },
                            showAsAction = true,
                        ),
                        if (serverId != null) {
                            ActionMenuItem(
                                title = stringResource(Res.string.settings_server_action_delete),
                                icon = Icons.Filled.Delete,
                                onClick = {
                                    viewModel.removeServer(serverId)
                                    onNavigateBack(AddEditServerResult.Delete)
                                },
                                showAsAction = true,
                            )
                        } else {
                            null
                        },
                        ActionMenuItem(
                            title = stringResource(Res.string.settings_server_action_save),
                            icon = Icons.Filled.Save,
                            onClick = {
                                val serverConfig = validateAndGetServerConfig() ?: return@ActionMenuItem
                                softwareKeyboardController?.hide()
                                if (serverConfig.id == -1) {
                                    viewModel.addServer(serverConfig)
                                    onNavigateBack(AddEditServerResult.Add)
                                } else {
                                    viewModel.editServer(serverConfig)
                                    onNavigateBack(AddEditServerResult.Edit)
                                }
                            },
                            showAsAction = true,
                        ),
                    )

                    AppBarActions(items = actionMenuItems)
                },
                colors = scrollState.topAppBarColors(),
            )
        },
        snackbarHost = {
            SwipeableSnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom)),
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .imePadding(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = stringResource(Res.string.settings_server_server_info),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(vertical = 4.dp),
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = {
                        Text(
                            text = stringResource(Res.string.settings_server_name),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Computer,
                            contentDescription = null,
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Next,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )

                OutlinedTextField(
                    value = url,
                    onValueChange = {
                        if (it.text != url.text) {
                            urlError = null
                        }
                        url = it
                    },
                    label = {
                        Text(
                            text = stringResource(Res.string.settings_server_url),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Link,
                            contentDescription = null,
                        )
                    },
                    supportingText = {
                        val examples = """
                            
                            • 192.168.1.20:8080
                            • https://example.com
                            • https://example.com/qbittorrent
                        """.trimIndent()

                        Text(
                            text = urlError?.let { stringResource(it) }
                                ?: stringResource(Res.string.settings_server_url_examples, examples),
                        )
                    },
                    isError = urlError != null,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Next,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize(),
                )

                HorizontalDivider(
                    modifier = Modifier
                        .padding(vertical = 8.dp)
                        .fillWidthOfParent(32.dp),
                )

                Text(
                    text = stringResource(Res.string.settings_server_credentials),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(vertical = 4.dp),
                )

                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = {
                        Text(
                            text = stringResource(Res.string.settings_server_username),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = null,
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Next,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics {
                            contentType = ContentType.Username
                        },
                )

                var showPassword by rememberSaveable { mutableStateOf(false) }
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = {
                        Text(
                            text = stringResource(Res.string.settings_server_password),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = null,
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                    ),
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(
                                imageVector = if (showPassword) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                contentDescription = null,
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics {
                            contentType = ContentType.Password
                        },
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = {
                        val serverConfig = validateAndGetServerConfig() ?: return@OutlinedButton
                        viewModel.testConnection(serverConfig)
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        imageVector = Icons.Filled.NetworkCheck,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp),
                    )
                    Text(
                        text = stringResource(Res.string.settings_server_test_configuration),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }

                Spacer(
                    modifier = Modifier.windowInsetsBottomHeight(WindowInsets.safeDrawing),
                )
            }

            AnimatedVisibility(
                visible = isTesting,
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

enum class AddEditServerResult {
    Add,
    Edit,
    Delete,
}

expect fun isPlatformUrlValid(url: String): Boolean
