package dev.bartuzen.qbitcontroller.ui.settings.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import dev.bartuzen.qbitcontroller.R

@Composable
fun EditTextPreference(
    name: String,
    modifier: Modifier = Modifier,
    initialText: String? = null,
    hideText: Boolean = false,
    onDialogClose: () -> Unit,
    onConfirm: ((text: String) -> Unit)? = null,
    onCancel: ((text: String) -> Unit)? = null
) {
    Dialog(
        onDismissRequest = onDialogClose,
    ) {
        EditTextPreferenceContent(
            name,
            modifier,
            initialText,
            hideText,
            onDialogClose,
            onConfirm,
            onCancel
        )
    }
}

@Composable
fun EditTextPreferenceContent(
    name: String,
    modifier: Modifier = Modifier,
    initialText: String? = null,
    hideText: Boolean = false,
    onDialogClose: () -> Unit,
    onConfirm: ((text: String) -> Unit)? = null,
    onCancel: ((text: String) -> Unit)? = null
) {
    Surface(
        modifier = modifier
            .fillMaxWidth(),
        elevation = 4.dp,
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 8.dp)
        ) {
            var textFieldValue by remember {
                mutableStateOf(
                    TextFieldValue(
                        initialText ?: "", TextRange(initialText?.length ?: 0)
                    )
                )
            }
            var passwordVisibility by rememberSaveable { mutableStateOf(false) }
            val focusRequester = remember { FocusRequester() }

            Text(
                text = name,
                modifier = Modifier.padding(bottom = 8.dp),
                style = TextStyle(
                    fontSize = 18.sp
                )
            )
            TextField(
                value = textFieldValue,
                onValueChange = { textFieldValue = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .focusRequester(focusRequester),
                visualTransformation = if (!hideText || passwordVisibility) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    if (hideText) {
                        val image = if (passwordVisibility) {
                            Icons.Default.Visibility
                        } else {
                            Icons.Default.VisibilityOff
                        }

                        IconButton(onClick = { passwordVisibility = !passwordVisibility }) {
                            Icon(imageVector = image, contentDescription = null)
                        }
                    }
                }
            )

            LaunchedEffect(true) {
                focusRequester.requestFocus()
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = stringResource(R.string.settings_cancel),
                    style = TextStyle(color = MaterialTheme.colors.primary),
                    modifier = Modifier
                        .clickable {
                            onCancel?.invoke(textFieldValue.text)
                            onDialogClose()
                        }
                        .padding(8.dp),
                )
                Text(
                    text = stringResource(R.string.settings_ok),
                    style = TextStyle(color = MaterialTheme.colors.primary),
                    modifier = Modifier
                        .clickable {
                            onConfirm?.invoke(textFieldValue.text)
                            onDialogClose()
                        }
                        .padding(8.dp),
                )
            }
        }
    }
}

@Preview
@Composable
fun EditTextPreferencePreview() {
    EditTextPreferenceContent(
        name = "Name",
        initialText = "Server",
        onDialogClose = { }
    )
}