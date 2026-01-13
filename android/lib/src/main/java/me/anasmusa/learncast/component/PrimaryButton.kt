package me.anasmusa.learncast.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import me.anasmusa.learncast.string

@Composable
fun PrimaryButton(
    modifier: Modifier = Modifier,
    icon: ImageVector?,
    title: Int,
    clip: Boolean = true,
    padding: PaddingValues = PaddingValues(12.dp),
    paddingBetween: Dp = 8.dp,
    backgroundColor: Color = MaterialTheme.colorScheme.tertiaryContainer,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    onClick: () -> Unit
) {
    PrimaryButton(
        modifier,
        icon,
        title.string(),
        clip,
        padding,
        paddingBetween,
        backgroundColor,
        horizontalArrangement,
        onClick
    )
}

@Composable
fun PrimaryButton(
    modifier: Modifier = Modifier,
    icon: ImageVector?,
    title: String,
    clip: Boolean = true,
    padding: PaddingValues = PaddingValues(12.dp),
    paddingBetween: Dp = 8.dp,
    backgroundColor: Color = MaterialTheme.colorScheme.tertiaryContainer,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .let {
                if (clip) it.clip(RoundedCornerShape(8.dp))
                else it
            }
            .clickable(onClick = onClick)
            .background(backgroundColor)
            .padding(padding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = horizontalArrangement
    ) {

        if (icon != null)
            Icon(
                modifier = Modifier
                    .padding(end = paddingBetween),
                imageVector = icon,
                contentDescription = null
            )

        Text(
            text = title,
            maxLines = 1,
            style = MaterialTheme.typography.titleMedium
        )

    }
}