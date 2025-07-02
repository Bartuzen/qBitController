package dev.bartuzen.qbitcontroller.preferences

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp

enum class ListPreferenceType {
    AlertDialog,
    DropdownMenu,
}

@Composable
fun <T> ListPreference(
    value: T,
    onValueChange: (T) -> Unit,
    values: List<T>,
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: @Composable (() -> Unit)? = null,
    summary: @Composable (() -> Unit)? = null,
    type: ListPreferenceType = ListPreferenceType.AlertDialog,
    valueToText: (T) -> AnnotatedString = { AnnotatedString(it.toString()) },
    item: @Composable (value: T, currentValue: T, onClick: () -> Unit) -> Unit =
        ListPreferenceDefaults.item(type, valueToText),
    onSelectorStateChange: ((Boolean) -> Unit)? = null,
) {
    var openSelector by rememberSaveable { mutableStateOf(false) }
    PersistentLaunchedEffect(openSelector) {
        onSelectorStateChange?.invoke(openSelector)
    }
    val theme = LocalPreferenceTheme.current
    if (openSelector) {
        when (type) {
            ListPreferenceType.AlertDialog -> {
                PreferenceAlertDialog(
                    onDismissRequest = { openSelector = false },
                    title = title,
                    buttons = {
                        TextButton(onClick = { openSelector = false }) {
                            Text(text = theme.dialogCancelText)
                        }
                    },
                ) {
                    val lazyListState = rememberLazyListState()
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().verticalScrollIndicators(lazyListState),
                        state = lazyListState,
                    ) {
                        items(values) { itemValue ->
                            item(itemValue, value) {
                                onValueChange(itemValue)
                                openSelector = false
                            }
                        }
                    }
                }
            }

            ListPreferenceType.DropdownMenu -> {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(theme.padding.copy(vertical = 0.dp))
                ) {
                    DropdownMenu(
                        expanded = openSelector,
                        onDismissRequest = { openSelector = false },
                    ) {
                        for (itemValue in values) {
                            item(itemValue, value) {
                                onValueChange(itemValue)
                                openSelector = false
                            }
                        }
                    }
                }
            }
        }
    }
    Preference(
        title = title,
        modifier = modifier,
        enabled = enabled,
        icon = icon,
        summary = summary,
    ) {
        openSelector = true
    }
}

object ListPreferenceDefaults {
    fun <T> item(
        type: ListPreferenceType,
        valueToText: (T) -> AnnotatedString,
    ): @Composable (value: T, currentValue: T, onClick: () -> Unit) -> Unit =
        when (type) {
            ListPreferenceType.AlertDialog -> {
                { value, currentValue, onClick ->
                    DialogItem(value, currentValue, valueToText, onClick)
                }
            }

            ListPreferenceType.DropdownMenu -> {
                { value, currentValue, onClick ->
                    DropdownMenuItem(value, currentValue, valueToText, onClick)
                }
            }
        }

    @Composable
    fun <T> DialogItem(
        value: T,
        currentValue: T,
        valueToText: (T) -> AnnotatedString,
        onClick: () -> Unit,
    ) {
        val selected = value == currentValue
        Row(
            modifier =
                Modifier.fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .selectable(selected, true, Role.RadioButton, onClick)
                    .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(selected = selected, onClick = null)
            Spacer(modifier = Modifier.width(24.dp))
            Text(
                text = valueToText(value),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }

    @Composable
    fun <T> DropdownMenuItem(
        value: T,
        currentValue: T,
        valueToText: (T) -> AnnotatedString,
        onClick: () -> Unit,
    ) {
        DropdownMenuItem(
            text = { Text(text = valueToText(value)) },
            onClick = onClick,
            modifier =
                Modifier.background(
                    if (value == currentValue) MaterialTheme.colorScheme.secondaryContainer
                    else Color.Transparent
                ),
            colors = MenuDefaults.itemColors(),
        )
    }
}

