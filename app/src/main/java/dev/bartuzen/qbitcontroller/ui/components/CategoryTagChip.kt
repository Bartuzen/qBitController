package dev.bartuzen.qbitcontroller.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
private fun CategoryTagChip(
    text: String,
    backgroundColor: Color,
    selectedBackgroundColor: Color,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val background = if (!isSelected) backgroundColor else selectedBackgroundColor
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.0.dp))
            .background(background)
            .let {
                if (onClick != null) {
                    it.clickable(onClick = onClick)
                } else {
                    it
                }
            }
            .padding(top = 6.dp, bottom = 6.dp, start = 12.dp, end = 16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AnimatedVisibility(isSelected) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    tint = contentColorFor(background),
                    modifier = Modifier.size(12.dp),
                    contentDescription = null,
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = text,
                color = contentColorFor(background),
                style = MaterialTheme.typography.labelLarge,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun CategoryChip(
    category: String,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    CategoryTagChip(
        text = category,
        modifier = modifier,
        isSelected = isSelected,
        onClick = onClick,
        backgroundColor = MaterialTheme.colorScheme.primaryContainer,
        selectedBackgroundColor = MaterialTheme.colorScheme.primary,
    )
}

@Composable
fun TagChip(tag: String, modifier: Modifier = Modifier, isSelected: Boolean = false, onClick: (() -> Unit)? = null) {
    CategoryTagChip(
        text = tag,
        modifier = modifier,
        isSelected = isSelected,
        onClick = onClick,
        backgroundColor = MaterialTheme.colorScheme.tertiaryContainer,
        selectedBackgroundColor = MaterialTheme.colorScheme.tertiary,
    )
}
