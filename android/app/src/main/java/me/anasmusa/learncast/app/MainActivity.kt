package me.anasmusa.learncast.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import me.anasmusa.learncast.app.ui.theme.MontserratTypography
import me.anasmusa.learncast.app.ui.theme.backgroundColors
import me.anasmusa.learncast.app.ui.theme.darkScheme
import me.anasmusa.learncast.app.ui.theme.playerBackgroundColors
import me.anasmusa.learncast.lib.Activity
import me.anasmusa.learncast.lib.App

class MainActivity : Activity() {
    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialExpressiveTheme(
                colorScheme = darkScheme,
                typography = MontserratTypography(),
            ) {
                App(
                    backgroundColors = backgroundColors,
                    playerBackgroundColors = playerBackgroundColors,
                )
            }
        }
    }
}
