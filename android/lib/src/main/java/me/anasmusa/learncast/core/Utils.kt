package me.anasmusa.learncast.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

const val BOTTOM_PADDING = 144

@Composable
fun backgroundBrush(): Brush =
    Brush.verticalGradient(
        colors = LocalAppEnvironment.current.backgroundColors,
        endY = with(LocalDensity.current) { 250.dp.toPx() },
    )

@Stable
fun formatTime(seconds: Int): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60

    return if (h > 0) {
        String.format("%d:%02d:%02d", h, m, s)
    } else {
        String.format("%02d:%02d", m, s)
    }
}

@Stable
fun formatTime(mSeconds: Long): String {
    val seconds = mSeconds / 1000
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60

    return if (h > 0) {
        String.format("%d:%02d:%02d", h, m, s)
    } else {
        String.format("%02d:%02d", m, s)
    }
}
