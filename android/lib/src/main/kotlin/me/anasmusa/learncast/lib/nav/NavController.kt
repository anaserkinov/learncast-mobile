package me.anasmusa.learncast.lib.nav

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey

class NavController(
    private val backStack: NavBackStack<NavKey>,
) {
    fun navigate(screen: Screen) {
        backStack.add(screen)
    }

    fun popBack() {
        backStack.removeAt(backStack.lastIndex)
    }
}

val LocalNavController = staticCompositionLocalOf<NavController> { error("LocalNavController error") }

@Composable
fun ProvideNavController(
    navBackStack: NavBackStack<NavKey>,
    content: @Composable () -> Unit,
) {
    val navController =
        remember(navBackStack) {
            NavController(navBackStack)
        }

    CompositionLocalProvider(LocalNavController provides navController) {
        content()
    }
}
