package dev.bartuzen.qbitcontroller.ui.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity

// https://issuetracker.google.com/issues/406497234
@Composable
fun LazyColumnItemMinHeight() {
    val height = with(LocalDensity.current) { 1.toDp() }
    Spacer(modifier = Modifier.height(height))
}
