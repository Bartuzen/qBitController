package dev.bartuzen.qbitcontroller.preferences

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp

@Composable
fun <T> TextFieldPreference(
    value: T,
    onValueChange: (T) -> Unit,
    title: @Composable () -> Unit,
    textToValue: (String) -> T?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: @Composable (() -> Unit)? = null,
    summary: @Composable (() -> Unit)? = null,
    valueToText: (T) -> String = { it.toString() },
    textField:
    @Composable
        (value: TextFieldValue, onValueChange: (TextFieldValue) -> Unit, onOk: () -> Unit) -> Unit =
        TextFieldPreferenceDefaults.TextField,
    onDialogStateChange: ((Boolean) -> Unit)? = null,
) {
    var openDialog by rememberSaveable { mutableStateOf(false) }
    PersistentLaunchedEffect(openDialog) {
        onDialogStateChange?.invoke(openDialog)
    }
    Preference(
        title = title,
        modifier = modifier,
        enabled = enabled,
        icon = icon,
        summary = summary,
    ) {
        openDialog = true
    }
    if (openDialog) {
        var dialogText by
        rememberSaveable(stateSaver = TextFieldValue.Saver) {
            val text = valueToText(value)
            mutableStateOf(TextFieldValue(text, TextRange(text.length)))
        }
        val onOk = {
            val dialogValue = textToValue(dialogText.text)
            if (dialogValue != null) {
                onValueChange(dialogValue)
                openDialog = false
            }
        }
        PreferenceAlertDialog(
            onDismissRequest = { openDialog = false },
            title = title,
            buttons = {
                val theme = LocalPreferenceTheme.current
                TextButton(onClick = { openDialog = false }) {
                    Text(text = theme.dialogCancelText)
                }
                if (theme.useTextButtonForDialogConfirmation) {
                    TextButton(onClick = onOk) { Text(text = theme.dialogOkText) }
                } else {
                    Button(onClick = onOk) { Text(text = theme.dialogOkText) }
                }
            },
        ) {
            val focusRequester = remember { FocusRequester() }
            Box(
                modifier =
                    Modifier.fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .focusRequester(focusRequester)
            ) {
                textField(dialogText, { dialogText = it }, onOk)
            }
            LaunchedEffect(focusRequester) { focusRequester.requestFocus() }
        }
    }
}

object TextFieldPreferenceDefaults {
    val TextField:
            @Composable
                (value: TextFieldValue, onValueChange: (TextFieldValue) -> Unit, onOk: () -> Unit) -> Unit =
        { value, onValueChange, onOk ->
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                keyboardActions = KeyboardActions { onOk() },
                singleLine = true,
            )
        }
}

