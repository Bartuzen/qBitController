package dev.bartuzen.qbitcontroller.ui.settings

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import dev.bartuzen.qbitcontroller.utils.stringResource
import me.zhanghai.compose.preference.Preference
import qbitcontroller.composeapp.generated.resources.Res
import qbitcontroller.composeapp.generated.resources.settings_category_appearance
import qbitcontroller.composeapp.generated.resources.settings_category_general
import qbitcontroller.composeapp.generated.resources.settings_category_network
import qbitcontroller.composeapp.generated.resources.settings_category_servers
import qbitcontroller.composeapp.generated.resources.settings_title

@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToServerSettings: () -> Unit,
    onNavigateToGeneralSettings: () -> Unit,
    onNavigateToAppearanceSettings: () -> Unit,
    onNavigateToNetworkSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(Res.string.settings_title),
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
                Preference(
                    title = { Text(text = stringResource(Res.string.settings_category_servers)) },
                    icon = {
                        Icon(
                            imageVector = Icons.Filled.Dns,
                            contentDescription = null,
                        )
                    },
                    onClick = { onNavigateToServerSettings() },
                )
            }
            item {
                Preference(
                    title = { Text(text = stringResource(Res.string.settings_category_general)) },
                    icon = {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = null,
                        )
                    },
                    onClick = { onNavigateToGeneralSettings() },
                )
            }

            item {
                Preference(
                    title = { Text(text = stringResource(Res.string.settings_category_appearance)) },
                    icon = {
                        Icon(
                            imageVector = Icons.Filled.Palette,
                            contentDescription = null,
                        )
                    },
                    onClick = { onNavigateToAppearanceSettings() },
                )
            }

            item {
                Preference(
                    title = { Text(text = stringResource(Res.string.settings_category_network)) },
                    icon = {
                        Icon(
                            imageVector = Icons.Filled.Public,
                            contentDescription = null,
                        )
                    },
                    onClick = { onNavigateToNetworkSettings() },
                )
            }
        }
    }
}
