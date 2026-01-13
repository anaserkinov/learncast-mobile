package me.anasmusa.learncast.theme.icon

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Google: ImageVector
    get() {
        if (_Google != null) {
            return _Google!!
        }
        _Google = ImageVector.Builder(
            name = "Google",
            defaultWidth = 800.dp,
            defaultHeight = 800.dp,
            viewportWidth = 32f,
            viewportHeight = 32f
        ).apply {
            path(fill = SolidColor(Color(0xFF4285F4))) {
                moveTo(30.001f, 16.311f)
                curveTo(30.001f, 15.16f, 29.906f, 14.32f, 29.7f, 13.449f)
                horizontalLineTo(16.287f)
                verticalLineTo(18.644f)
                horizontalLineTo(24.16f)
                curveTo(24.001f, 19.935f, 23.144f, 21.88f, 21.239f, 23.186f)
                lineTo(21.213f, 23.36f)
                lineTo(25.454f, 26.58f)
                lineTo(25.747f, 26.609f)
                curveTo(28.446f, 24.167f, 30.001f, 20.573f, 30.001f, 16.311f)
                close()
            }
            path(fill = SolidColor(Color(0xFF34A853))) {
                moveTo(16.286f, 30f)
                curveTo(20.143f, 30f, 23.381f, 28.755f, 25.747f, 26.609f)
                lineTo(21.239f, 23.186f)
                curveTo(20.032f, 24.011f, 18.413f, 24.586f, 16.286f, 24.586f)
                curveTo(12.509f, 24.586f, 9.302f, 22.144f, 8.159f, 18.769f)
                lineTo(7.992f, 18.782f)
                lineTo(3.582f, 22.127f)
                lineTo(3.524f, 22.284f)
                curveTo(5.874f, 26.857f, 10.699f, 30f, 16.286f, 30f)
                close()
            }
            path(fill = SolidColor(Color(0xFFFBBC05))) {
                moveTo(8.16f, 18.769f)
                curveTo(7.858f, 17.898f, 7.684f, 16.965f, 7.684f, 16f)
                curveTo(7.684f, 15.036f, 7.858f, 14.102f, 8.144f, 13.231f)
                lineTo(8.136f, 13.046f)
                lineTo(3.671f, 9.647f)
                lineTo(3.525f, 9.716f)
                curveTo(2.557f, 11.613f, 2.001f, 13.745f, 2.001f, 16f)
                curveTo(2.001f, 18.256f, 2.557f, 20.387f, 3.525f, 22.285f)
                lineTo(8.16f, 18.769f)
                close()
            }
            path(fill = SolidColor(Color(0xFFEB4335))) {
                moveTo(16.286f, 7.413f)
                curveTo(18.969f, 7.413f, 20.778f, 8.549f, 21.81f, 9.498f)
                lineTo(25.842f, 5.64f)
                curveTo(23.366f, 3.384f, 20.143f, 2f, 16.286f, 2f)
                curveTo(10.699f, 2f, 5.874f, 5.142f, 3.524f, 9.715f)
                lineTo(8.143f, 13.231f)
                curveTo(9.302f, 9.856f, 12.509f, 7.413f, 16.286f, 7.413f)
                close()
            }
        }.build()

        return _Google!!
    }

@Suppress("ObjectPropertyName")
private var _Google: ImageVector? = null
