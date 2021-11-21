package dev.bartuzen.qbitcontroller.ui.settings.common

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.RadioButton
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import dev.bartuzen.qbitcontroller.R
import dev.bartuzen.qbitcontroller.data.Theme
import dev.bartuzen.qbitcontroller.model.SettingsEntry

@Composable
fun <T : Any> ListPreference(
    title: String?,
    settingsEntries: List<SettingsEntry<T>>,
    selectedEntry: T?,
    onComplete: (selected: T?) -> Unit,
    modifier: Modifier = Modifier
) {
    Dialog(
        onDismissRequest = { onComplete(null) },
    ) {
        ListPreferenceContent(
            title = title,
            settingsEntries = settingsEntries,
            selectedEntry = selectedEntry,
            onComplete = onComplete,
            modifier = modifier
        )
    }
}

@Composable
fun <T : Any> ListPreferenceContent(
    title: String?,
    settingsEntries: List<SettingsEntry<T>>,
    selectedEntry: T?,
    onComplete: (selected: T?) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth(),
        elevation = 4.dp,
        shape = RoundedCornerShape(8.dp)
    ) {
        Column {
            if (title != null) {
                Text(
                    text = title,
                    modifier = Modifier.padding(
                        start = 24.dp,
                        end = 24.dp,
                        top = 16.dp,
                        bottom = 8.dp
                    ),
                    style = TextStyle(
                        fontSize = 18.sp
                    )
                )
            }
            settingsEntries.forEach { entry ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onComplete(entry.entryValue)
                        }
                        .padding(horizontal = 8.dp)
                ) {
                    RadioButton(
                        selected = selectedEntry == entry.entryValue,
                        onClick = { }
                    )
                    Text(
                        text = entry.title,
                        modifier = Modifier.padding(end = 16.dp)
                    )
                }
            }
            Text(
                text = stringResource(R.string.settings_cancel),
                style = TextStyle(color = MaterialTheme.colors.primary),
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(end = 24.dp, top = 16.dp, bottom = 8.dp)
                    .clickable {
                        onComplete(null)
                    }
                    .padding(8.dp)
            )
        }
    }
}

@Preview(
    uiMode = UI_MODE_NIGHT_YES
)
@Composable
fun ListPreferencePreview() {
    val titles = stringArrayResource(R.array.settings_theme_entries)

    ListPreferenceContent(
        title = stringResource(R.string.settings_theme),
        settingsEntries = listOf(
            SettingsEntry(titles[0], Theme.LIGHT),
            SettingsEntry(titles[1], Theme.DARK),
            SettingsEntry(titles[2], Theme.SYSTEM_DEFAULT),
        ),
        selectedEntry = Theme.SYSTEM_DEFAULT,
        onComplete = { },
    )
}