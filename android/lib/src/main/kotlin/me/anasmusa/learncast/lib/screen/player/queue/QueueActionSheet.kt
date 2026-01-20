package me.anasmusa.learncast.lib.screen.player.queue

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import me.anasmusa.learncast.Strings
import me.anasmusa.learncast.core.appConfig
import me.anasmusa.learncast.core.normalizeUrl
import me.anasmusa.learncast.data.model.QueueItem
import me.anasmusa.learncast.data.model.getSampleQueueItem
import me.anasmusa.learncast.lib.AppTheme
import me.anasmusa.learncast.lib.component.SheetMenuButton
import me.anasmusa.learncast.lib.theme.icon.PlayArrowIcon
import me.anasmusa.learncast.Resource.string

@Preview
@Composable
private fun QueueActionSheetPreview() {
    AppTheme {
        QueueActionSheet(
            item = getSampleQueueItem(),
            onDismissRequest = {},
            onPlay = {},
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueActionSheet(
    item: QueueItem,
    onDismissRequest: () -> Unit,
    onPlay: () -> Unit,
) {
    ModalBottomSheet(
        modifier =
            Modifier
                .fillMaxWidth(),
        sheetState =
            rememberModalBottomSheetState(
                skipPartiallyExpanded = true,
            ),
        onDismissRequest = onDismissRequest,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 12.dp,
                    ),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AsyncImage(
                    modifier =
                        Modifier
                            .padding(end = 8.dp)
                            .size(64.dp)
                            .clip(RoundedCornerShape(8)),
                    model =
                        if (item.coverImagePath != null) {
                            item.coverImagePath!!.normalizeUrl()
                        } else {
                            appConfig.mainLogo
                        },
                    contentScale = ContentScale.Crop,
                    contentDescription = null,
                )
                Text(
                    text = item.title,
                    maxLines = 2,
                    style = MaterialTheme.typography.titleMedium,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 17.sp,
                )
            }
            Spacer(
                modifier = Modifier.height(12.dp),
            )

            SheetMenuButton(
                icon = PlayArrowIcon,
                title = Strings.PLAY_NOW.string(),
                paddingBetween = 28.dp,
                onClick = onPlay,
            )

            Spacer(
                modifier = Modifier.height(12.dp),
            )
        }
    }
}
