package me.anasmusa.learncast.core

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import me.anasmusa.learncast.nav.Screen
import dev.chrisbanes.haze.HazeState

class AppEnvironment(
    val backStack: NavBackStack<NavKey>,
    val hazeState: HazeState,
    val backgroundColors: List<Color>,
    val playerBackgroundColors: List<Color>,
    val isNightMode: Boolean,
    val changeNightMode: () -> Unit
){
    fun navigate(screen: Screen){
        backStack.add(screen)
    }
    fun popBack(){
        backStack.removeAt(backStack.lastIndex)
    }
}

val LocalAppEnvironment = staticCompositionLocalOf <AppEnvironment> { error("LocalAppEnvironment error") }

@Composable
fun ProvideAppEnvironment(
    backStack: NavBackStack<NavKey>,
    hazeState: HazeState,
    backgroundColors: List<Color>,
    playerBackgroundColors: List<Color>,
    isNightMode: Boolean,
    changeNightMode: () -> Unit,
    content: @Composable () -> Unit
) {
    val appEnvironment = remember {
        AppEnvironment(
            backStack,
            hazeState,
            backgroundColors,
            playerBackgroundColors,
            isNightMode,
            changeNightMode
        )
    }

    CompositionLocalProvider(LocalAppEnvironment provides appEnvironment) {
        content()
    }
}
