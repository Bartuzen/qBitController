package dev.bartuzen.qbitcontroller.ui.settings.general

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.bartuzen.qbitcontroller.WindowsDefaultTorrentHandlersRegistrationResult
import dev.bartuzen.qbitcontroller.areWindowsDefaultTorrentHandlersRegistered
import dev.bartuzen.qbitcontroller.preferences.Preference
import dev.bartuzen.qbitcontroller.registerWindowsDefaultTorrentHandlers
import dev.bartuzen.qbitcontroller.utils.Platform
import dev.bartuzen.qbitcontroller.utils.currentPlatform
import dev.bartuzen.qbitcontroller.utils.stringResource
import kotlinx.coroutines.delay
import qbitcontroller.composeapp.generated.resources.Res
import qbitcontroller.composeapp.generated.resources.settings_magnet_links
import qbitcontroller.composeapp.generated.resources.settings_magnet_links_desc
import qbitcontroller.composeapp.generated.resources.settings_set_as_default

@Composable
actual fun DefaultMagnetHandlerPreference() {
    if (currentPlatform !is Platform.Desktop.Windows) return

    var isDefault by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            isDefault = areWindowsDefaultTorrentHandlersRegistered()
            delay(2_000)
        }
    }

    Preference(
        title = { Text(text = stringResource(Res.string.settings_magnet_links)) },
        summary = { Text(text = stringResource(Res.string.settings_magnet_links_desc)) },
        widgetContainer = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(
                    onClick = {
                        isDefault = when (registerWindowsDefaultTorrentHandlers()) {
                            WindowsDefaultTorrentHandlersRegistrationResult.Registered -> true
                            WindowsDefaultTorrentHandlersRegistrationResult.OpenedDefaultAppsSettings,
                            WindowsDefaultTorrentHandlersRegistrationResult.Unavailable,
                            -> areWindowsDefaultTorrentHandlersRegistered()
                        }
                    },
                    enabled = !isDefault,
                ) {
                    Text(text = stringResource(Res.string.settings_set_as_default))
                }

                if (isDefault) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF2E7D32),
                    )
                }
            }
        },
    )
}
