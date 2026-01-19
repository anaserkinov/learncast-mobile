package me.anasmusa.learncast.lib.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun SheetMenuButton(
    modifier: Modifier = Modifier,
    icon: ImageVector?,
    title: String,
    clip: Boolean = true,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    padding: PaddingValues = PaddingValues(12.dp),
    paddingBetween: Dp = 8.dp,
    onClick: () -> Unit,
) {
    PrimaryButton(
        modifier =
            modifier
                .fillMaxWidth()
                .height(56.dp),
        icon = icon,
        title = title,
        clip = clip,
        padding = padding,
        paddingBetween = paddingBetween,
        horizontalArrangement = horizontalArrangement,
        backgroundColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        onClick = onClick,
    )
}

@Composable
fun SheetMenuWhiteButton(
    modifier: Modifier = Modifier,
    icon: ImageVector?,
    title: String,
    clip: Boolean = true,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    padding: PaddingValues = PaddingValues(12.dp),
    paddingBetween: Dp = 8.dp,
    onClick: () -> Unit,
) {
    PrimaryButton(
        modifier =
            modifier
                .fillMaxWidth()
                .height(56.dp),
        icon = icon,
        title = title,
        clip = clip,
        padding = padding,
        paddingBetween = paddingBetween,
        horizontalArrangement = horizontalArrangement,
        backgroundColor = MaterialTheme.colorScheme.secondaryContainer,
        onClick = onClick,
        titleColor = MaterialTheme.colorScheme.background,
    )
}
