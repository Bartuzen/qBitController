package dev.bartuzen.qbitcontroller.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp

data class ActionMenuItem(
    val title: String?,
    val onClick: () -> Unit,
    val showAsAction: Boolean,
    val icon: ImageVector?,
    val isEnabled: Boolean = true,
    val isVisible: Boolean = true,
    val isHidden: Boolean = false,
    val trailingIcon: (@Composable () -> Unit)? = null,
    val dropdownMenu: (@Composable () -> Unit)? = null,
)

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

    val maxActionItems = getMaxActionButtons()
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
                                    tint = MaterialTheme.colorScheme.secondary,
                                )
                            }
                        } else {
                            null
                        },
                        trailingIcon = item.trailingIcon,
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
private fun getMaxActionButtons(): Int {
    val density = LocalDensity.current
    val width = with(density) { LocalWindowInfo.current.containerSize.width.toDp() }

    return if (width < 600.dp) 3 else 5
}
