package me.anasmusa.learncast.theme.icon

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Telegram: ImageVector
    get() {
        if (_Telegram != null) {
            return _Telegram!!
        }
        _Telegram = ImageVector.Builder(
            name = "Telegram",
            defaultWidth = 100.dp,
            defaultHeight = 100.dp,
            viewportWidth = 100f,
            viewportHeight = 100f
        ).apply {
            path(fill = SolidColor(Color(0xFF1B92D1))) {
                moveTo(91.52f, 9.73f)
                curveTo(78.33f, 15.19f, 21.76f, 38.62f, 6.13f, 45.01f)
                curveTo(-4.35f, 49.1f, 1.78f, 52.94f, 1.78f, 52.94f)
                curveTo(1.78f, 52.94f, 10.73f, 56f, 18.4f, 58.3f)
                curveTo(26.07f, 60.61f, 30.16f, 58.05f, 30.16f, 58.05f)
                lineTo(66.21f, 33.76f)
                curveTo(78.99f, 25.07f, 75.92f, 32.23f, 72.86f, 35.3f)
                curveTo(66.21f, 41.94f, 55.22f, 52.42f, 46.01f, 60.86f)
                curveTo(41.92f, 64.44f, 43.97f, 67.51f, 45.76f, 69.04f)
                curveTo(52.4f, 74.67f, 70.56f, 86.17f, 71.58f, 86.94f)
                curveTo(76.98f, 90.76f, 87.6f, 96.26f, 89.22f, 84.64f)
                lineTo(95.61f, 44.5f)
                curveTo(97.65f, 30.95f, 99.7f, 18.42f, 99.95f, 14.84f)
                curveTo(100.72f, 6.15f, 91.52f, 9.73f, 91.52f, 9.73f)
                close()
            }
        }.build()

        return _Telegram!!
    }

@Suppress("ObjectPropertyName")
private var _Telegram: ImageVector? = null
