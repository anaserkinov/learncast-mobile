package me.anasmusa.learncast.lib.core

import androidx.compose.ui.graphics.Color
import kotlin.math.max
import kotlin.math.min

fun Int.darken(amount: Float): Color {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(this, hsv)

    // increase saturation
    hsv[1] = min(1f, hsv[1] + amount)

    // optional: tiny brightness reduction (NOT black)
    hsv[2] = max(0f, hsv[2] * (1f - amount * 0.6f))

    return Color(android.graphics.Color.HSVToColor(hsv))
}

fun Int.lighten(amount: Float): Color {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(this, hsv)

    hsv[2] = min(1f, hsv[2] + hsv[2] * amount)

    return Color(android.graphics.Color.HSVToColor(hsv))
}
