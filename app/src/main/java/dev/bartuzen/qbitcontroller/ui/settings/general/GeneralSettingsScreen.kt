package dev.bartuzen.qbitcontroller.ui.settings.general

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.bartuzen.qbitcontroller.R
import me.zhanghai.compose.preference.SwitchPreference
import me.zhanghai.compose.preference.TextFieldPreference
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun GeneralSettingsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GeneralSettingsViewModel = koinViewModel(),
) {
    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings_category_general),
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
                val context = LocalContext.current
                val notificationCheckInterval by viewModel.notificationCheckInterval.flow.collectAsStateWithLifecycle()
                var error by rememberSaveable { mutableStateOf<Pair<Int, Array<Any>>?>(null) }
                TextFieldPreference(
                    value = notificationCheckInterval,
                    onValueChange = { viewModel.notificationCheckInterval.value = it },
                    textToValue = { text ->
                        val number = text.toIntOrNull()
                        when {
                            text.isBlank() -> 0
                            number in 15..(24 * 60) -> number
                            else -> {
                                error = R.string.error_invalid_interval_optional to arrayOf(15, 24 * 60, 0)
                                null
                            }
                        }
                    },
                    valueToText = { if (it == 0) "" else it.toString() },
                    title = { Text(text = stringResource(R.string.settings_notification_check_interval)) },
                    summary = {
                        Text(
                            text = if (notificationCheckInterval == 0) {
                                stringResource(R.string.settings_disabled)
                            } else {
                                pluralStringResource(
                                    R.plurals.settings_notification_check_interval_description,
                                    notificationCheckInterval,
                                    notificationCheckInterval,
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
                            modifier = Modifier.fillMaxWidth(),
                        )
                    },
                    onDialogStateChange = { error = null },
                    enabled = NotificationManagerCompat.from(context).areNotificationsEnabled(),
                )
            }

            item {
                val swipeActionsEnabled by viewModel.areTorrentSwipeActionsEnabled.flow.collectAsStateWithLifecycle()
                SwitchPreference(
                    value = swipeActionsEnabled,
                    onValueChange = { viewModel.areTorrentSwipeActionsEnabled.value = it },
                    title = { Text(text = stringResource(R.string.settings_enable_torrent_swipe_actions)) },
                )
            }
        }
    }
}
