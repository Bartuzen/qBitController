package dev.bartuzen.qbitcontroller.ui.settings.common

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun PreferenceCategory(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title,
        style = TextStyle(
            color = MaterialTheme.colors.secondary
        ),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 64.dp, vertical = 8.dp)
    )
}

@Preview(
    showBackground = true
)
@Composable
fun PreferenceCategoryPreview() {
    PreferenceCategory("Hello")
}