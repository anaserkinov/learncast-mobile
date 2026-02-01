package me.anasmusa.learncast.lib.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import dev.chrisbanes.haze.HazeState

class AppEnvironment(
    val hazeState: HazeState,
    val backgroundColors: List<Color>,
    val playerBackgroundColors: List<Color>
)

val LocalAppEnvironment = staticCompositionLocalOf<AppEnvironment> { error("LocalAppEnvironment error") }

@Composable
fun ProvideAppEnvironment(
    hazeState: HazeState,
    backgroundColors: List<Color>,
    playerBackgroundColors: List<Color>,
    content: @Composable () -> Unit,
) {
    val appEnvironment =
        remember {
            AppEnvironment(
                hazeState,
                backgroundColors,
                playerBackgroundColors
            )
        }

    CompositionLocalProvider(LocalAppEnvironment provides appEnvironment) {
        content()
    }
}
