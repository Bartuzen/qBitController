package dev.bartuzen.qbitcontroller.ui.settings.network

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.bartuzen.qbitcontroller.utils.stringResource
import me.zhanghai.compose.preference.TextFieldPreference
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.pluralStringResource
import org.koin.compose.viewmodel.koinViewModel
import qbitcontroller.composeapp.generated.resources.Res
import qbitcontroller.composeapp.generated.resources.error_invalid_interval
import qbitcontroller.composeapp.generated.resources.error_invalid_interval_optional
import qbitcontroller.composeapp.generated.resources.error_required_field
import qbitcontroller.composeapp.generated.resources.settings_auto_refresh_interval
import qbitcontroller.composeapp.generated.resources.settings_auto_refresh_interval_description
import qbitcontroller.composeapp.generated.resources.settings_category_network
import qbitcontroller.composeapp.generated.resources.settings_connection_timeout
import qbitcontroller.composeapp.generated.resources.settings_connection_timeout_description
import qbitcontroller.composeapp.generated.resources.settings_disabled

@Composable
fun NetworkSettingsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NetworkSettingsViewModel = koinViewModel(),
) {
    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(Res.string.settings_category_network),
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
            )
        },
    ) { innerPadding ->
        LazyColumn(
            contentPadding = innerPadding,
            modifier = Modifier.fillMaxSize(),
        ) {
            item {
                val connectionTimeout by viewModel.connectionTimeout.flow.collectAsStateWithLifecycle()
                var error by rememberSaveable { mutableStateOf<Pair<StringResource, Array<Any>>?>(null) }
                TextFieldPreference(
                    value = connectionTimeout,
                    onValueChange = { viewModel.connectionTimeout.value = it },
                    textToValue = { text ->
                        val number = text.toIntOrNull()
                        when {
                            text.isBlank() -> {
                                error = Res.string.error_required_field to arrayOf()
                                null
                            }
                            number !in 1..3600 -> {
                                error = Res.string.error_invalid_interval to arrayOf(1, 3600)
                                null
                            }
                            else -> number
                        }
                    },
                    title = { Text(text = stringResource(Res.string.settings_connection_timeout)) },
                    summary = {
                        Text(
                            text = pluralStringResource(
                                Res.plurals.settings_connection_timeout_description,
                                connectionTimeout,
                                connectionTimeout,
                            ),
                        )
                    },
                    textField = { value, onValueChange, onOk ->
                        OutlinedTextField(
                            value = value,
                            onValueChange = {
                                if (it.text.all { it.isDigit() }) {
                                    onValueChange(it)
                                    if (it.text != value.text) {
                                        error = null
                                    }
                                }
                            },
                            isError = error != null,
                            supportingText = error?.let { { Text(stringResource(it.first, *it.second)) } },
                            keyboardActions = KeyboardActions { onOk() },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateContentSize(),
                        )
                    },
                    onDialogStateChange = { error = null },
                )
            }

            item {
                val autoRefreshInterval by viewModel.autoRefreshInterval.flow.collectAsStateWithLifecycle()
                var error by rememberSaveable { mutableStateOf<Pair<StringResource, Array<Any>>?>(null) }
                TextFieldPreference(
                    value = autoRefreshInterval,
                    onValueChange = { viewModel.autoRefreshInterval.value = it },
                    textToValue = { text ->
                        val number = text.toIntOrNull()
                        when {
                            text.isBlank() -> 0
                            number in 1..3600 -> number
                            else -> {
                                error = Res.string.error_invalid_interval_optional to arrayOf(1, 3600)
                                null
                            }
                        }
                    },
                    valueToText = { if (it == 0) "" else it.toString() },
                    title = { Text(text = stringResource(Res.string.settings_auto_refresh_interval)) },
                    summary = {
                        Text(
                            text = if (autoRefreshInterval == 0) {
                                stringResource(Res.string.settings_disabled)
                            } else {
                                pluralStringResource(
                                    Res.plurals.settings_auto_refresh_interval_description,
                                    autoRefreshInterval,
                                    autoRefreshInterval,
                                )
                            },
                        )
                    },

                    textField = { value, onValueChange, onOk ->
                        OutlinedTextField(
                            value = value,
                            onValueChange = {
                                if (it.text.all { it.isDigit() }) {
                                    onValueChange(it)
                                    if (it.text != value.text) {
                                        error = null
                                    }
                                }
                            },
                            isError = error != null,
                            supportingText = error?.let { { Text(stringResource(it.first, *it.second)) } },
                            keyboardActions = KeyboardActions { onOk() },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateContentSize(),
                        )
                    },
                    onDialogStateChange = { error = null },
                )
            }
        }
    }
}
