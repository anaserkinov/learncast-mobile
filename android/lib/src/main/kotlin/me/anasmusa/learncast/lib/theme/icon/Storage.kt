package me.anasmusa.learncast.lib.theme.icon

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Storage: ImageVector
    get() {
        if (_Storage != null) {
            return _Storage!!
        }
        _Storage =
            ImageVector
                .Builder(
                    name = "Storage",
                    defaultWidth = 24.dp,
                    defaultHeight = 24.dp,
                    viewportWidth = 960f,
                    viewportHeight = 960f,
                ).apply {
                    path(fill = SolidColor(Color.Black)) {
                        moveTo(200f, 800f)
                        quadToRelative(-33f, 0f, -56.5f, -23.5f)
                        reflectiveQuadTo(120f, 720f)
                        quadToRelative(0f, -33f, 23.5f, -56.5f)
                        reflectiveQuadTo(200f, 640f)
                        horizontalLineToRelative(560f)
                        quadToRelative(33f, 0f, 56.5f, 23.5f)
                        reflectiveQuadTo(840f, 720f)
                        quadToRelative(0f, 33f, -23.5f, 56.5f)
                        reflectiveQuadTo(760f, 800f)
                        lineTo(200f, 800f)
                        close()
                        moveTo(200f, 320f)
                        quadToRelative(-33f, 0f, -56.5f, -23.5f)
                        reflectiveQuadTo(120f, 240f)
                        quadToRelative(0f, -33f, 23.5f, -56.5f)
                        reflectiveQuadTo(200f, 160f)
                        horizontalLineToRelative(560f)
                        quadToRelative(33f, 0f, 56.5f, 23.5f)
                        reflectiveQuadTo(840f, 240f)
                        quadToRelative(0f, 33f, -23.5f, 56.5f)
                        reflectiveQuadTo(760f, 320f)
                        lineTo(200f, 320f)
                        close()
                        moveTo(200f, 560f)
                        quadToRelative(-33f, 0f, -56.5f, -23.5f)
                        reflectiveQuadTo(120f, 480f)
                        quadToRelative(0f, -33f, 23.5f, -56.5f)
                        reflectiveQuadTo(200f, 400f)
                        horizontalLineToRelative(560f)
                        quadToRelative(33f, 0f, 56.5f, 23.5f)
                        reflectiveQuadTo(840f, 480f)
                        quadToRelative(0f, 33f, -23.5f, 56.5f)
                        reflectiveQuadTo(760f, 560f)
                        lineTo(200f, 560f)
                        close()
                        moveTo(240f, 280f)
                        quadToRelative(17f, 0f, 28.5f, -11.5f)
                        reflectiveQuadTo(280f, 240f)
                        quadToRelative(0f, -17f, -11.5f, -28.5f)
                        reflectiveQuadTo(240f, 200f)
                        quadToRelative(-17f, 0f, -28.5f, 11.5f)
                        reflectiveQuadTo(200f, 240f)
                        quadToRelative(0f, 17f, 11.5f, 28.5f)
                        reflectiveQuadTo(240f, 280f)
                        close()
                        moveTo(240f, 520f)
                        quadToRelative(17f, 0f, 28.5f, -11.5f)
                        reflectiveQuadTo(280f, 480f)
                        quadToRelative(0f, -17f, -11.5f, -28.5f)
                        reflectiveQuadTo(240f, 440f)
                        quadToRelative(-17f, 0f, -28.5f, 11.5f)
                        reflectiveQuadTo(200f, 480f)
                        quadToRelative(0f, 17f, 11.5f, 28.5f)
                        reflectiveQuadTo(240f, 520f)
                        close()
                        moveTo(240f, 760f)
                        quadToRelative(17f, 0f, 28.5f, -11.5f)
                        reflectiveQuadTo(280f, 720f)
                        quadToRelative(0f, -17f, -11.5f, -28.5f)
                        reflectiveQuadTo(240f, 680f)
                        quadToRelative(-17f, 0f, -28.5f, 11.5f)
                        reflectiveQuadTo(200f, 720f)
                        quadToRelative(0f, 17f, 11.5f, 28.5f)
                        reflectiveQuadTo(240f, 760f)
                        close()
                    }
                }.build()

        return _Storage!!
    }

@Suppress("ObjectPropertyName")
private var _Storage: ImageVector? = null
