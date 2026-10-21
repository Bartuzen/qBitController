package dev.bartuzen.qbitcontroller.ui.settings.serversettings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.bartuzen.qbitcontroller.model.ServerPreferences
import dev.bartuzen.qbitcontroller.preferences.ListPreference
import dev.bartuzen.qbitcontroller.preferences.PreferenceCategory
import dev.bartuzen.qbitcontroller.preferences.SwitchPreference
import dev.bartuzen.qbitcontroller.preferences.TextFieldPreference
import dev.bartuzen.qbitcontroller.ui.components.SwipeableSnackbarHost
import dev.bartuzen.qbitcontroller.utils.EventEffect
import dev.bartuzen.qbitcontroller.utils.formatBytes
import dev.bartuzen.qbitcontroller.utils.formatBytesPerSecond
import dev.bartuzen.qbitcontroller.utils.getErrorMessage
import dev.bartuzen.qbitcontroller.utils.stringResource
import dev.bartuzen.qbitcontroller.utils.topAppBarColors
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import qbitcontroller.composeapp.generated.resources.Res
import qbitcontroller.composeapp.generated.resources.server_settings_action_retry
import qbitcontroller.composeapp.generated.resources.server_settings_action_save
import qbitcontroller.composeapp.generated.resources.server_settings_load_failed
import qbitcontroller.composeapp.generated.resources.server_settings_save_success
import qbitcontroller.composeapp.generated.resources.server_settings_title
import qbitcontroller.composeapp.generated.resources.server_settings_unlimited

@Composable
fun ServerSettingsScreen(
    serverId: Int,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ServerSettingsViewModel = koinViewModel(parameters = { parametersOf(serverId) }),
) {
    val preferences by viewModel.preferences.collectAsStateWithLifecycle()
    val support by viewModel.support.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()

    var editedPreferences by remember { mutableStateOf<ServerPreferences?>(null) }
    LaunchedEffect(preferences) {
        if (editedPreferences == null) {
            editedPreferences = preferences
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val saveSuccessMessage = stringResource(Res.string.server_settings_save_success)

    EventEffect(viewModel.eventFlow) { event ->
        when (event) {
            is ServerSettingsViewModel.Event.Error -> {
                snackbarHostState.currentSnackbarData?.dismiss()
                scope.launch {
                    snackbarHostState.showSnackbar(getErrorMessage(event.error))
                }
            }
            ServerSettingsViewModel.Event.Saved -> {
                snackbarHostState.currentSnackbarData?.dismiss()
                scope.launch {
                    snackbarHostState.showSnackbar(saveSuccessMessage)
                }
            }
        }
    }

    val listState = rememberLazyListState()
    val currentPreferences = editedPreferences ?: preferences
    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(Res.string.server_settings_title),
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
                    val isDirty = editedPreferences != null && editedPreferences != preferences
                    IconButton(
                        onClick = { editedPreferences?.let(viewModel::savePreferences) },
                        enabled = isDirty && !isSaving && !isLoading,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Save,
                            contentDescription = stringResource(Res.string.server_settings_action_save),
                        )
                    }
                },
                colors = listState.topAppBarColors(),
            )
        },
        snackbarHost = {
            SwipeableSnackbarHost(hostState = snackbarHostState)
        },
    ) { innerPadding ->
        if (currentPreferences == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                if (isLoading) {
                    CircularProgressIndicator()
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = stringResource(Res.string.server_settings_load_failed))
                        TextButton(onClick = viewModel::loadPreferences) {
                            Text(text = stringResource(Res.string.server_settings_action_retry))
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                state = listState,
                contentPadding = innerPadding,
                modifier = Modifier.fillMaxSize(),
            ) {
                serverPreferenceGroups.forEach { group ->
                    item {
                        PreferenceCategory(
                            title = { Text(text = stringResource(group.title)) },
                        )
                    }
                    group.items.forEach { item ->
                        if (item.isAvailable(currentPreferences)) {
                            item {
                                PreferenceItem(
                                    item = item,
                                    preferences = currentPreferences,
                                    support = support,
                                    onPreferencesChange = { editedPreferences = it },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun ServerPreferenceItem.isAvailable(preferences: ServerPreferences) = isVisible(preferences) && when (this) {
    is SwitchItem -> value(preferences) != null
    is NumberItem -> value(preferences) != null
    is DecimalItem -> value(preferences) != null
    is TextItem -> value(preferences) != null
    is ChoiceItem -> value(preferences) != null
}

@Composable
private fun PreferenceItem(
    item: ServerPreferenceItem,
    preferences: ServerPreferences,
    support: ServerPreferencesSupport,
    onPreferencesChange: (ServerPreferences) -> Unit,
) {
    when (item) {
        is SwitchItem -> {
            val value = item.value(preferences) ?: return
            SwitchPreference(
                value = value,
                onValueChange = { onPreferencesChange(item.onChange(preferences, it)) },
                title = { Text(text = stringResource(item.title)) },
                summary = item.description?.let { description ->
                    { Text(text = stringResource(description)) }
                },
            )
        }
        is NumberItem -> {
            val value = item.value(preferences) ?: return
            TextFieldPreference(
                value = value,
                onValueChange = { onPreferencesChange(item.onChange(preferences, it)) },
                title = { Text(text = stringResource(item.title)) },
                summary = {
                    val description = item.description?.let { stringResource(it) }
                    val formattedValue = formatNumber(value, item.format)
                    Text(
                        text = listOfNotNull(description, formattedValue).joinToString(" · "),
                    )
                },
                textToValue = { text -> text.toLongOrNull()?.takeIf { it >= 0 } },
                valueToText = { it.toString() },
                textField = { textFieldValue, onValueChange, onOk ->
                    NumberTextField(textFieldValue, onValueChange, onOk)
                },
            )
        }
        is TextItem -> {
            val value = item.value(preferences) ?: return
            TextFieldPreference(
                value = value,
                onValueChange = { onPreferencesChange(item.onChange(preferences, it)) },
                title = { Text(text = stringResource(item.title)) },
                summary = item.description?.let { description ->
                    { Text(text = stringResource(description)) }
                },
                textToValue = { it },
            )
        }
        is DecimalItem -> {
            val value = item.value(preferences) ?: return
            TextFieldPreference(
                value = value,
                onValueChange = { onPreferencesChange(item.onChange(preferences, it)) },
                title = { Text(text = stringResource(item.title)) },
                summary = item.description?.let { description ->
                    { Text(text = stringResource(description)) }
                },
                textToValue = { text -> text.toDoubleOrNull()?.takeIf { it >= 0 } },
                valueToText = { it.toString() },
                textField = { textFieldValue, onValueChange, onOk ->
                    DecimalTextField(textFieldValue, onValueChange, onOk)
                },
            )
        }
        is ChoiceItem -> {
            val value = item.value(preferences) ?: return
            val supportedOptions = item.options.filter { option -> item.isOptionSupported(option.value, support) }
            val labels = supportedOptions.associate { option ->
                option.value to stringResource(option.label)
            }
            ListPreference(
                value = value,
                onValueChange = { onPreferencesChange(item.onChange(preferences, it)) },
                values = supportedOptions.map { it.value },
                title = { Text(text = stringResource(item.title)) },
                summary = {
                    val description = item.description?.let { stringResource(it) }
                    val currentLabel = item.options.find { it.value == value }
                        ?.let { stringResource(it.label) }
                        ?: value.toString()
                    Text(
                        text = listOfNotNull(description, currentLabel).joinToString(" · "),
                    )
                },
                valueToText = { optionValue -> AnnotatedString(labels[optionValue] ?: optionValue.toString()) },
            )
        }
    }
}

@Composable
private fun NumberTextField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    onOk: () -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { newValue ->
            if (newValue.text.all { it.isDigit() }) {
                onValueChange(newValue)
            }
        },
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number,
            imeAction = ImeAction.Done,
        ),
        keyboardActions = KeyboardActions { onOk() },
        singleLine = true,
    )
}

@Composable
private fun DecimalTextField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    onOk: () -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { newValue ->
            if (newValue.text.all { it.isDigit() || it == '.' }) {
                onValueChange(newValue)
            }
        },
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Decimal,
            imeAction = ImeAction.Done,
        ),
        keyboardActions = KeyboardActions { onOk() },
        singleLine = true,
    )
}

@Composable
private fun formatNumber(value: Long, format: NumberFormat): String = when (format) {
    NumberFormat.Plain -> value.toString()
    NumberFormat.Speed -> if (value <= 0) {
        stringResource(Res.string.server_settings_unlimited)
    } else {
        formatBytesPerSecond(value)
    }
    NumberFormat.Bytes -> if (value <= 0) {
        stringResource(Res.string.server_settings_unlimited)
    } else {
        formatBytes(value)
    }
    NumberFormat.Seconds -> "$value s"
    NumberFormat.Minutes -> "$value min"
    NumberFormat.MiB -> "$value MiB"
}
