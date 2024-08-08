package dev.bartuzen.qbitcontroller.ui.icons

import androidx.compose.material.icons.Icons
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

@Suppress("UnusedReceiverParameter")
val Icons.Outlined.Priority: ImageVector
    get() {
        if (icon != null) {
            return icon!!
        }
        icon = ImageVector.Builder(
            name = "priority",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 40f,
            viewportHeight = 40f,
        ).apply {
            path(
                fill = SolidColor(Color.Black),
                fillAlpha = 1f,
                stroke = null,
                strokeAlpha = 1f,
                strokeLineWidth = 1.0f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter,
                strokeLineMiter = 1f,
                pathFillType = PathFillType.NonZero,
            ) {
                moveTo(14.792f, 34.75f)
                quadToRelative(-3.959f, 0f, -6.75f, -2.792f)
                quadToRelative(-2.792f, -2.791f, -2.792f, -6.75f)
                verticalLineTo(14.792f)
                quadToRelative(0f, -3.959f, 2.792f, -6.75f)
                quadToRelative(2.791f, -2.792f, 6.75f, -2.792f)
                horizontalLineToRelative(10.416f)
                quadToRelative(3.959f, 0f, 6.75f, 2.792f)
                quadToRelative(2.792f, 2.791f, 2.792f, 6.75f)
                verticalLineToRelative(10.416f)
                quadToRelative(0f, 3.959f, -2.792f, 6.75f)
                quadToRelative(-2.791f, 2.792f, -6.75f, 2.792f)
                close()
                moveToRelative(3.541f, -8.375f)
                lineToRelative(9.792f, -9.792f)
                lineToRelative(-1.833f, -1.875f)
                lineToRelative(-7.959f, 7.917f)
                lineToRelative(-3.916f, -3.875f)
                lineToRelative(-1.875f, 1.875f)
                close()
                moveToRelative(-3.541f, 5.75f)
                horizontalLineToRelative(10.416f)
                quadToRelative(2.875f, 0f, 4.896f, -2.042f)
                quadToRelative(2.021f, -2.041f, 2.021f, -4.875f)
                verticalLineTo(14.792f)
                quadToRelative(0f, -2.875f, -2.042f, -4.896f)
                quadToRelative(-2.041f, -2.021f, -4.875f, -2.021f)
                horizontalLineTo(14.792f)
                quadToRelative(-2.875f, 0f, -4.896f, 2.042f)
                quadToRelative(-2.021f, 2.041f, -2.021f, 4.875f)
                verticalLineToRelative(10.416f)
                quadToRelative(0f, 2.875f, 2.042f, 4.896f)
                quadToRelative(2.041f, 2.021f, 4.875f, 2.021f)
                close()
                moveTo(20f, 20f)
                close()
            }
        }.build()
        return icon!!
    }

private var icon: ImageVector? = null
