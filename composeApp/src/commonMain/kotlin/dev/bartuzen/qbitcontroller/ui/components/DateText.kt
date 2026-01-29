package dev.bartuzen.qbitcontroller.ui.components

import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.bartuzen.qbitcontroller.data.SettingsManager
import dev.bartuzen.qbitcontroller.utils.formatDate
import dev.bartuzen.qbitcontroller.utils.formatRelativeDate
import org.koin.compose.koinInject
import kotlin.time.Instant

@Composable
fun DateText(date: Instant, tooltipText: @Composable (date: String) -> Unit, content: @Composable (date: String) -> Unit) {
    val settingsManager = koinInject<SettingsManager>()
    val relativeTimestamp by settingsManager.showRelativeTimestamps.flow.collectAsStateWithLifecycle()

    DateText(
        date = date,
        showRelativeTimestamp = relativeTimestamp,
        tooltipText = tooltipText,
        content = content,
    )
}

@Composable
fun DateText(
    date: Instant,
    showRelativeTimestamp: Boolean,
    tooltipText: @Composable (date: String) -> Unit,
    content: @Composable (date: String) -> Unit,
) {
    if (showRelativeTimestamp) {
        TooltipBox(
            positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
            tooltip = {
                PlainTooltip {
                    tooltipText(date.formatDate())
                }
            },
            state = rememberTooltipState(isPersistent = true),
            focusable = false,
        ) {
            content(date.formatRelativeDate())
        }
    } else {
        content(date.formatDate())
    }
}
