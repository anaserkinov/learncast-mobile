package me.anasmusa.learncast.lib.component.cell

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import me.anasmusa.learncast.core.appConfig
import me.anasmusa.learncast.core.normalizeUrl
import me.anasmusa.learncast.data.model.QueueItem

@Composable
fun RowScope.QueueItemCell(
    queueItem: QueueItem,
) {
    AsyncImage(
        modifier =
            Modifier
                .padding(end = 8.dp)
                .size(80.dp)
                .clip(RoundedCornerShape(8.dp)),
        model =
            if (queueItem.coverImagePath != null) {
                queueItem.coverImagePath!!.normalizeUrl()
            } else {
                appConfig.mainLogo
            },
        contentScale = ContentScale.Crop,
        contentDescription = null,
    )

    Column(
        modifier =
            Modifier
                .weight(1f)
                .height(80.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
//                Icon(
//                    modifier = Modifier
//                        .padding(end = 2.dp)
//                        .size(16.dp)
//                        .alpha(0.7f),
//                    imageVector = CutIcon,
//                    contentDescription = null
//                )
            Text(
                modifier =
                    Modifier
                        .alpha(0.7f),
                text = queueItem.subTitle,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Text(
            text = queueItem.title,
            maxLines = 2,
            style = MaterialTheme.typography.titleMedium,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 17.sp,
        )
//        Text(
//            modifier = Modifier
//                .alpha(0.7f),
//            text = "${queueItem.createdAt?.dayMonth()} · ${queueItem.audioDuration}",
//            style = MaterialTheme.typography.bodyMedium
//        )
    }
}
