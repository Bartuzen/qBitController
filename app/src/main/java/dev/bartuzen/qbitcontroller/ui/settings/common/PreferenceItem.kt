package dev.bartuzen.qbitcontroller.ui.settings.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.bartuzen.qbitcontroller.utils.toAsterisks

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun PreferenceItem(
    name: String,
    currentValue: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    hideText: Boolean = false
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp, horizontal = 64.dp),
    ) {
        Text(name)
        AnimatedVisibility(
            visible = currentValue != null && currentValue.isNotBlank()
        ) {
            currentValue?.let { value ->
                Text(
                    text = if (!hideText) value else value.toAsterisks(),
                    style = TextStyle(
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Light
                    )
                )
            }
        }
    }
}

@Preview(
    showBackground = true
)
@Composable
fun PreferenceItemPreview() {
    PreferenceItem(
        name = "Name",
        currentValue = "Hello",
        onClick = { }
    )
}