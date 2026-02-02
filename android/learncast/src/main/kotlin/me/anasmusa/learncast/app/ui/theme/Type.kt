package me.anasmusa.learncast.app.ui.theme

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import me.anasmusa.learncast.lib.R

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun MontserratTypography() =
    Typography().run {
        val fontFamily =
            FontFamily(
                Font(R.font.montserrat_regular, weight = FontWeight.Normal),
                Font(R.font.montserrat_medium, weight = FontWeight.Medium),
                Font(R.font.montserrat_bold, weight = FontWeight.Bold),
            )

        copy(
            displayLarge = displayLarge.copy(fontFamily = fontFamily),
            displayMedium = displayMedium.copy(fontFamily = fontFamily),
            displaySmall = displaySmall.copy(fontFamily = fontFamily),
            headlineLarge = headlineLarge.copy(fontFamily = fontFamily),
            headlineMedium = headlineMedium.copy(fontFamily = fontFamily),
            headlineSmall = headlineSmall.copy(fontFamily = fontFamily),
            titleLarge = titleLarge.copy(fontFamily = fontFamily),
            titleMedium = titleMedium.copy(fontFamily = fontFamily),
            titleSmall = titleSmall.copy(fontFamily = fontFamily),
            bodyLarge = bodyLarge.copy(fontFamily = fontFamily),
            bodyMedium = bodyMedium.copy(fontFamily = fontFamily),
            bodySmall = bodySmall.copy(fontFamily = fontFamily),
            labelLarge = labelLarge.copy(fontFamily = fontFamily),
            labelMedium = labelMedium.copy(fontFamily = fontFamily),
            labelSmall = labelSmall.copy(fontFamily = fontFamily),
            displayLargeEmphasized = displayLargeEmphasized.copy(fontFamily = fontFamily),
            displayMediumEmphasized = displayMediumEmphasized.copy(fontFamily = fontFamily),
            displaySmallEmphasized = displaySmallEmphasized.copy(fontFamily = fontFamily),
            headlineLargeEmphasized = headlineLargeEmphasized.copy(fontFamily = fontFamily),
            headlineMediumEmphasized = headlineMediumEmphasized.copy(fontFamily = fontFamily),
            headlineSmallEmphasized = headlineSmallEmphasized.copy(fontFamily = fontFamily),
            titleLargeEmphasized = titleLargeEmphasized.copy(fontFamily = fontFamily),
            titleMediumEmphasized = titleMediumEmphasized.copy(fontFamily = fontFamily),
            titleSmallEmphasized = titleSmallEmphasized.copy(fontFamily = fontFamily),
            bodyLargeEmphasized = bodyLargeEmphasized.copy(fontFamily = fontFamily),
            bodyMediumEmphasized = bodyMediumEmphasized.copy(fontFamily = fontFamily),
            bodySmallEmphasized = bodySmallEmphasized.copy(fontFamily = fontFamily),
            labelLargeEmphasized = labelLargeEmphasized.copy(fontFamily = fontFamily),
            labelMediumEmphasized = labelMediumEmphasized.copy(fontFamily = fontFamily),
            labelSmallEmphasized = labelSmallEmphasized.copy(fontFamily = fontFamily),
        )
    }
