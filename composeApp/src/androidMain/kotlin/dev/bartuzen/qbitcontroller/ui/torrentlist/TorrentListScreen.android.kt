package dev.bartuzen.qbitcontroller.ui.torrentlist

import android.graphics.drawable.AdaptiveIconDrawable
import android.os.Build
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toBitmap
import dev.bartuzen.qbitcontroller.R

@Composable
actual fun AppIcon(modifier: Modifier) {
    Image(
        painter = adaptiveIconPainterResource(R.mipmap.ic_launcher_round),
        contentDescription = null,
    )
}

@Composable
private fun adaptiveIconPainterResource(@DrawableRes id: Int): Painter {
    val context = LocalContext.current
    val res = context.resources
    val theme = context.theme

    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val adaptiveIcon = remember(id) {
            ResourcesCompat.getDrawable(res, id, theme) as? AdaptiveIconDrawable
        }
        if (adaptiveIcon != null) {
            remember(id) {
                BitmapPainter(adaptiveIcon.toBitmap().asImageBitmap())
            }
        } else {
            painterResource(id)
        }
    } else {
        painterResource(id)
    }
}
