package me.anasmusa.learncast.lib.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import me.anasmusa.learncast.Strings
import me.anasmusa.learncast.lib.theme.icon.SearchIcon
import me.anasmusa.learncast.Resource.string

@Composable
fun RowScope.SearchButton(
    searchQuery: String?,
    onQueryChanged: (value: String?) -> Unit,
    leftContent: (@Composable RowScope.(weight: Float) -> Unit)? = null,
) {
    val focusRequester = remember { FocusRequester() }

    if (leftContent != null) {
        val topicButtonWeight by animateFloatAsState(
            if (searchQuery != null) 0f else 1f,
            animationSpec = tween(300),
        )
        if (topicButtonWeight != 0f) {
            leftContent.invoke(this, topicButtonWeight)
        }
    }

    if (searchQuery != null) {
        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }
        SearchInput(
            modifier = Modifier.weight(1f),
            text = searchQuery,
            focusRequester = focusRequester,
            onTextChange = onQueryChanged,
        )
    } else {
        PrimaryButton(
            modifier = Modifier.weight(1f),
            icon = SearchIcon,
            titleKey = Strings.SEARCH,
        ) {
            onQueryChanged("")
        }
    }
    AnimatedContent(
        targetState = searchQuery != null,
        transitionSpec = {
            slideInHorizontally(
                initialOffsetX = { it },
            ) togetherWith
                slideOutHorizontally(
                    targetOffsetX = { it },
                )
        },
        contentAlignment = Alignment.Center,
    ) {
        if (it) {
            TextButton(
                modifier =
                    Modifier
                        .padding(start = 8.dp),
                onClick = { onQueryChanged(null) },
            ) {
                Text(text = Strings.CANCEL.string())
            }
        }
    }
}

@Composable
fun SearchInput(
    modifier: Modifier,
    text: String,
    focusRequester: FocusRequester,
    onTextChange: (text: String) -> Unit,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.tertiaryContainer, RoundedCornerShape(8.dp))
                .padding(12.dp),
    ) {
        Icon(
            imageVector = SearchIcon,
            contentDescription = null,
        )

        val colors = TextFieldDefaults.colors()
        val textStyle = MaterialTheme.typography.titleMedium
        val interactionSource = remember { MutableInteractionSource() }
        val textColor = colors.textColor(enabled = true, isError = false, focused = interactionSource.collectIsFocusedAsState().value)
        val mergedTextStyle = textStyle.merge(TextStyle(color = textColor))
        BasicTextField(
            modifier =
                Modifier
                    .padding(start = 8.dp)
                    .focusRequester(focusRequester),
            value = text,
            onValueChange = onTextChange,
            singleLine = true,
            textStyle = mergedTextStyle,
            cursorBrush = SolidColor(colors.cursorColor(false)),
            decorationBox = { innerTextField ->
                if (text.isEmpty()) {
                    Text(
                        modifier =
                            Modifier
                                .alpha(0.5f),
                        text = Strings.SEARCH.string(),
                        maxLines = 1,
                        style = mergedTextStyle,
                    )
                }
                innerTextField()
            },
        )
    }
}
