package dev.bartuzen.qbitcontroller.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import dev.bartuzen.qbitcontroller.utils.dropdownMenuHeight

data class ActionMenuItem(
    val title: String?,
    val onClick: () -> Unit,
    val showAsAction: Boolean,
    val icon: ImageVector?,
    val isEnabled: Boolean = true,
    val isVisible: Boolean = true,
    val isHidden: Boolean = false,
    val trailingIcon: ImageVector? = null,
    val dropdownMenu: (@Composable () -> Unit)? = null,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppBarActions(
    items: List<ActionMenuItem>,
    showOverflow: Boolean,
    onOverflowVisibilityChange: (visible: Boolean) -> Unit,
    canFocus: Boolean = true,
) {
    LaunchedEffect(canFocus, showOverflow) {
        if (!canFocus && showOverflow) {
            onOverflowVisibilityChange(false)
        }
    }

    val maxActionItems = rememberMaxActionButtons()
    val filteredItems = items.filter { it.isVisible }

    val (actionList, overflowList) = remember(filteredItems, maxActionItems) {
        val hasOverflow = filteredItems.find { !it.showAsAction } != null || filteredItems.size > maxActionItems

        if (hasOverflow) {
            val firstOverflowItemIndex =
                (filteredItems.indexOfFirst { !it.showAsAction }.takeIf { it != -1 } ?: (maxActionItems - 1))
                    .coerceAtMost(maxActionItems - 1)

            filteredItems.slice(0..<firstOverflowItemIndex) to
                filteredItems.slice(firstOverflowItemIndex..filteredItems.lastIndex)
        } else {
            filteredItems to emptyList()
        }
    }

    actionList.forEach { item ->
        if (!item.isHidden) {
            val content = @Composable {
                IconButton(
                    onClick = item.onClick,
                    enabled = item.isEnabled,
                    modifier = Modifier.focusProperties {
                        this.canFocus = canFocus
                    },
                ) {
                    if (item.icon != null) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.title,
                        )
                    }
                }
                item.dropdownMenu?.invoke()
            }

            if (item.title != null) {
                TooltipBox(
                    positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider((-80).dp),
                    tooltip = {
                        PlainTooltip {
                            Text(item.title)
                        }
                    },
                    state = rememberTooltipState(),
                    focusable = false,
                ) {
                    content()
                }
            } else {
                content()
            }
        } else {
            Spacer(modifier = Modifier)
        }
    }

    if (overflowList.isNotEmpty()) {
        Box {
            IconButton(
                onClick = {
                    if (canFocus && !showOverflow) {
                        onOverflowVisibilityChange(true)
                    }
                },
                modifier = Modifier
                    .focusProperties {
                        this.canFocus = canFocus
                    },
            ) {
                Icon(
                    imageVector = Icons.Filled.MoreVert,
                    contentDescription = null,
                )
            }

            val overflowScrollState = rememberScrollState()
            LaunchedEffect(showOverflow) {
                if (showOverflow) {
                    overflowScrollState.scrollTo(0)
                }
            }

            DropdownMenu(
                onDismissRequest = {
                    onOverflowVisibilityChange(false)
                },
                expanded = showOverflow,
                scrollState = overflowScrollState,
                modifier = Modifier.dropdownMenuHeight(),
            ) {
                overflowList.forEach { item ->
                    DropdownMenuItem(
                        text = {
                            Text(item.title ?: "")
                        },
                        leadingIcon = if (item.icon != null) {
                            {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = null,
                                )
                            }
                        } else {
                            null
                        },
                        trailingIcon = if (item.trailingIcon != null) {
                            {
                                Icon(
                                    imageVector = item.trailingIcon,
                                    contentDescription = null,
                                )
                            }
                        } else {
                            null
                        },
                        onClick = {
                            item.onClick()
                            onOverflowVisibilityChange(false)
                        },
                        enabled = item.isEnabled,
                        modifier = Modifier.focusProperties {
                            this.canFocus = canFocus
                        },
                    )
                }
            }

            overflowList.forEach { item ->
                item.dropdownMenu?.invoke()
            }
        }
    }
}

@Composable
fun AppBarActions(items: List<ActionMenuItem>, canFocus: Boolean = true) {
    var showOverflow by rememberSaveable { mutableStateOf(false) }
    AppBarActions(
        items = items,
        showOverflow = showOverflow,
        onOverflowVisibilityChange = { showOverflow = it },
        canFocus = canFocus,
    )
}

@Composable
private fun rememberMaxActionButtons(): Int {
    val config = LocalConfiguration.current

    val width = config.screenWidthDp
    val height = config.screenHeightDp
    val smallest = config.smallestScreenWidthDp

    return remember(width, height, smallest) {
        when {
            smallest > 600 || width > 960 && height > 720 || width > 720 && height > 960 -> 5
            width >= 500 || width > 480 && height > 640 -> 4
            width >= 360 -> 3
            else -> 2
        }
    }
}
