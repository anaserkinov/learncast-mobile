package me.anasmusa.learncast.theme.icon

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Sidebar: ImageVector
    get() {
        if (_Sidebar != null) return _Sidebar!!

        _Sidebar = ImageVector.Builder(
            name = "Sidebar",
            defaultWidth = 12.dp,
            defaultHeight = 24.dp,
            viewportWidth = 480f,
            viewportHeight = 960f
        ).apply {

            path(fill = SolidColor(Color.Black)) {
                moveTo(160f, 260f)
                quadTo(120f, 260f, 120f, 300f)
                quadTo(120f, 340f, 160f, 340f)
                lineTo(340f, 340f)
                lineTo(340f, 260f)
                close()
            }

            path(fill = SolidColor(Color.Black)) {
                moveTo(160f, 440f)
                quadTo(120f, 440f, 120f, 480f)
                quadTo(120f, 520f, 160f, 520f)
                lineTo(340f, 520f)
                lineTo(340f, 440f)
                close()
            }

            path(fill = SolidColor(Color.Black)) {
                moveTo(160f, 620f)
                quadTo(120f, 620f, 120f, 660f)
                quadTo(120f, 700f, 160f, 700f)
                lineTo(340f, 700f)
                lineTo(340f, 620f)
                close()
            }

        }.build()

        return _Sidebar!!
    }

@Suppress("ObjectPropertyName")
private var _Sidebar: ImageVector? = null
