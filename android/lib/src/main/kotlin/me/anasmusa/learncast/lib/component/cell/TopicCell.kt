package me.anasmusa.learncast.lib.component.cell

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import me.anasmusa.learncast.Strings
import me.anasmusa.learncast.core.appConfig
import me.anasmusa.learncast.core.monthYear
import me.anasmusa.learncast.core.normalizeUrl
import me.anasmusa.learncast.data.model.Topic
import me.anasmusa.learncast.Resource.quantityString

@Composable
fun TopicCell(
    topic: Topic,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .padding(top = 8.dp)
                .fillMaxSize()
                .clip(RoundedCornerShape(4.dp))
                .clickable(onClick = onClick)
                .padding(4.dp),
    ) {
        AsyncImage(
            modifier =
                Modifier
                    .padding(end = 8.dp)
                    .size(80.dp)
                    .clip(RoundedCornerShape(8.dp)),
            model =
                if (topic.coverImagePath != null) {
                    topic.coverImagePath!!.normalizeUrl()
                } else {
                    appConfig.mainLogo
                },
            contentScale = ContentScale.Crop,
            contentDescription = null,
        )

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(80.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                modifier =
                    Modifier
                        .alpha(0.7f),
                text = topic.authorName,
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = topic.title,
                maxLines = 2,
                style = MaterialTheme.typography.titleMedium,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 17.sp,
            )
            Text(
                modifier =
                    Modifier
                        .alpha(0.7f),
                text = "${topic.createdAt.monthYear()} · ${Strings.LESSON.quantityString(topic.lessonCount)}",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
