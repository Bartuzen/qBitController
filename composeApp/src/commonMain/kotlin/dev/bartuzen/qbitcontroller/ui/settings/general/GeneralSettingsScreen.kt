package dev.bartuzen.qbitcontroller.ui.settings.general

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.bartuzen.qbitcontroller.data.TrafficStats
import dev.bartuzen.qbitcontroller.generated.BuildConfig
import dev.bartuzen.qbitcontroller.preferences.ListPreference
import dev.bartuzen.qbitcontroller.preferences.ListPreferenceType
import dev.bartuzen.qbitcontroller.preferences.SwitchPreference
import dev.bartuzen.qbitcontroller.preferences.TextFieldPreference
import dev.bartuzen.qbitcontroller.utils.Platform
import dev.bartuzen.qbitcontroller.utils.areNotificationsEnabled
import dev.bartuzen.qbitcontroller.utils.currentPlatform
import dev.bartuzen.qbitcontroller.utils.stringResource
import dev.bartuzen.qbitcontroller.utils.topAppBarColors
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.pluralStringResource
import org.koin.compose.viewmodel.koinViewModel
import qbitcontroller.composeapp.generated.resources.Res
import qbitcontroller.composeapp.generated.resources.error_invalid_interval_optional
import qbitcontroller.composeapp.generated.resources.settings_category_general
import qbitcontroller.composeapp.generated.resources.settings_check_updates
import qbitcontroller.composeapp.generated.resources.settings_disabled
import qbitcontroller.composeapp.generated.resources.settings_enable_torrent_swipe_actions
import qbitcontroller.composeapp.generated.resources.settings_hide_server_urls
import qbitcontroller.composeapp.generated.resources.settings_hide_server_urls_desc
import qbitcontroller.composeapp.generated.resources.settings_notification_check_interval
import qbitcontroller.composeapp.generated.resources.settings_notification_check_interval_description
import qbitcontroller.composeapp.generated.resources.settings_traffic_stats_in_list
import qbitcontroller.composeapp.generated.resources.settings_traffic_stats_in_list_complete
import qbitcontroller.composeapp.generated.resources.settings_traffic_stats_in_list_none
import qbitcontroller.composeapp.generated.resources.settings_traffic_stats_in_list_session
import qbitcontroller.composeapp.generated.resources.settings_traffic_stats_in_list_total

@Composable
fun GeneralSettingsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GeneralSettingsViewModel = koinViewModel(),
) {
    val listState = rememberLazyListState()
    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(Res.string.settings_category_general),
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
                colors = listState.topAppBarColors(),
            )
        },
    ) { innerPadding ->
        val areNotificationsEnabled = areNotificationsEnabled()
        LazyColumn(
            state = listState,
            contentPadding = innerPadding,
            modifier = Modifier.fillMaxSize(),
        ) {
            item {
                val hideServerUrls by viewModel.hideServerUrls.flow.collectAsStateWithLifecycle()
                SwitchPreference(
                    value = hideServerUrls,
                    onValueChange = { viewModel.hideServerUrls.value = it },
                    title = { Text(text = stringResource(Res.string.settings_hide_server_urls)) },
                    summary = { Text(text = stringResource(Res.string.settings_hide_server_urls_desc)) },
                )
            }

            if (areNotificationsEnabled != null) {
                item {
                    val notificationCheckInterval by viewModel.notificationCheckInterval.flow.collectAsStateWithLifecycle()
                    var error by rememberSaveable { mutableStateOf<Pair<StringResource, Array<Any>>?>(null) }
                    TextFieldPreference(
                        value = notificationCheckInterval,
                        onValueChange = { viewModel.notificationCheckInterval.value = it },
                        textToValue = { text ->
                            val number = text.toIntOrNull()
                            when {
                                text.isBlank() -> 0
                                number in 15..(24 * 60) -> number
                                else -> {
                                    error = Res.string.error_invalid_interval_optional to arrayOf(15, 24 * 60, 0)
                                    null
                                }
                            }
                        },
                        valueToText = { if (it == 0) "" else it.toString() },
                        title = { Text(text = stringResource(Res.string.settings_notification_check_interval)) },
                        summary = {
                            Text(
                                text = if (notificationCheckInterval == 0) {
                                    stringResource(Res.string.settings_disabled)
                                } else {
                                    pluralStringResource(
                                        Res.plurals.settings_notification_check_interval_description,
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
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .animateContentSize(),
                            )
                        },
                        onDialogStateChange = { error = null },
                        enabled = areNotificationsEnabled,
                    )
                }
            }

            item {
                val swipeActionsEnabled by viewModel.areTorrentSwipeActionsEnabled.flow.collectAsStateWithLifecycle()
                SwitchPreference(
                    value = swipeActionsEnabled,
                    onValueChange = { viewModel.areTorrentSwipeActionsEnabled.value = it },
                    title = { Text(text = stringResource(Res.string.settings_enable_torrent_swipe_actions)) },
                )
            }

            item {
                val trafficStatsInList by viewModel.trafficStatsInList.flow.collectAsStateWithLifecycle()
                val trafficNoneString = stringResource(Res.string.settings_traffic_stats_in_list_none)
                val trafficTotalString = stringResource(Res.string.settings_traffic_stats_in_list_total)
                val trafficSessionString = stringResource(Res.string.settings_traffic_stats_in_list_session)
                val trafficCompleteString = stringResource(Res.string.settings_traffic_stats_in_list_complete)

                ListPreference(
                    value = trafficStatsInList,
                    onValueChange = { viewModel.trafficStatsInList.value = it },
                    values = listOf(
                        TrafficStats.NONE,
                        TrafficStats.TOTAL,
                        TrafficStats.SESSION,
                        TrafficStats.COMPLETE,
                    ),
                    title = { Text(text = stringResource(Res.string.settings_traffic_stats_in_list)) },
                    valueToText = { trafficStatsInList ->
                        AnnotatedString(
                            when (trafficStatsInList) {
                                TrafficStats.NONE -> trafficNoneString
                                TrafficStats.TOTAL -> trafficTotalString
                                TrafficStats.SESSION -> trafficSessionString
                                TrafficStats.COMPLETE -> trafficCompleteString
                            },
                        )
                    },
                    summary = {
                        Text(
                            text = when (trafficStatsInList) {
                                TrafficStats.NONE -> trafficNoneString
                                TrafficStats.TOTAL -> trafficTotalString
                                TrafficStats.SESSION -> trafficSessionString
                                TrafficStats.COMPLETE -> trafficCompleteString
                            },
                        )
                    },
                    type = ListPreferenceType.DropdownMenu,
                )
            }

            if (BuildConfig.EnableUpdateChecker && currentPlatform is Platform.Desktop) {
                item {
                    val checkUpdates by viewModel.checkUpdates.flow.collectAsStateWithLifecycle()
                    SwitchPreference(
                        value = checkUpdates,
                        onValueChange = { checked -> viewModel.checkUpdates.value = checked },
                        title = { Text(text = stringResource(Res.string.settings_check_updates)) },
                    )
                }
            }
        }
    }
}
