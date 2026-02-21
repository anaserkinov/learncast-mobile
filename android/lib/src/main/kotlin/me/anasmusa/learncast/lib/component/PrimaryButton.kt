package me.anasmusa.learncast.lib.component

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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import me.anasmusa.learncast.core.resource.Resource.string
import me.anasmusa.learncast.Strings
import me.anasmusa.learncast.lib.AppTheme
import me.anasmusa.learncast.lib.theme.icon.SearchIcon

@Preview
@Composable
private fun PrimaryButtonPreview(){
    AppTheme {
        PrimaryButton(
            titleKey = Strings.SEARCH,
            icon = SearchIcon
        ) { }
    }
}

@Composable
fun PrimaryButton(
    modifier: Modifier = Modifier,
    titleKey: String,
    icon: ImageVector?,
    clip: Boolean = true,
    padding: PaddingValues = PaddingValues(12.dp),
    spacing: Dp = 8.dp,
    titleColor: Color = MaterialTheme.colorScheme.onTertiaryContainer,
    backgroundColor: Color = MaterialTheme.colorScheme.tertiaryContainer,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    onClick: () -> Unit,
) {
    PrimaryButton(
        modifier,
        icon,
        titleKey.string(),
        clip,
        padding,
        spacing,
        titleColor,
        backgroundColor,
        horizontalArrangement,
        onClick,
    )
}

@Composable
fun PrimaryButton(
    modifier: Modifier = Modifier,
    icon: ImageVector?,
    title: String,
    clip: Boolean = true,
    padding: PaddingValues = PaddingValues(12.dp),
    spacing: Dp = 8.dp,
    titleColor: Color = MaterialTheme.colorScheme.onTertiaryContainer,
    backgroundColor: Color = MaterialTheme.colorScheme.tertiaryContainer,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            modifier
                .let {
                    if (clip) {
                        it.clip(RoundedCornerShape(8.dp))
                    } else {
                        it
                    }
                }.clickable(onClick = onClick)
                .background(backgroundColor)
                .padding(padding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = horizontalArrangement,
    ) {
        if (icon != null) {
            Icon(
                modifier =
                    Modifier
                        .padding(end = spacing),
                imageVector = icon,
                contentDescription = null,
            )
        }

        Text(
            text = title,
            maxLines = 1,
            style = MaterialTheme.typography.titleMedium,
            color = titleColor
        )
    }
}
