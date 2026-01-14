package me.anasmusa.learncast.screen.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import me.anasmusa.learncast.core.STATE_LOADING
import me.anasmusa.learncast.core.STATE_PAUSED
import me.anasmusa.learncast.core.appConfig
import me.anasmusa.learncast.core.normalizeUrl
import me.anasmusa.learncast.data.model.QueueItem
import me.anasmusa.learncast.theme.icon.Pause
import me.anasmusa.learncast.theme.icon.PlayArrowIcon

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun BottomPlayer(
    currentPlaying: QueueItem,
    currentPositionMs: Long,
    playbackState: Int,
    backgroundColors: List<Color>,
    onClicked: () -> Unit,
    togglePlaybackState: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .height(64.dp)
                .fillMaxWidth()
                .clickable(onClick = onClicked)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, backgroundColors.last()),
                        endY = with(LocalDensity.current) { 10.dp.toPx() },
                    ),
                ).clip(RoundedCornerShape(6.dp)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            modifier =
                Modifier
                    .padding(start = 8.dp, top = 4.dp, bottom = 4.dp)
                    .width(55.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(6)),
            model =
                if (currentPlaying.coverImagePath != null) {
                    currentPlaying.coverImagePath!!.normalizeUrl()
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
                    .fillMaxHeight()
                    .padding(start = 16.dp, top = 4.dp, end = 12.dp, bottom = 4.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = currentPlaying.title,
                color = Color.White,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                lineHeight = 16.sp,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                modifier =
                    Modifier
                        .alpha(0.7f),
                text = currentPlaying.subTitle,
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                lineHeight = 16.sp,
                overflow = TextOverflow.Ellipsis,
            )
            LinearProgressIndicator(
                modifier =
                    Modifier
                        .padding(top = 6.dp)
                        .fillMaxWidth()
                        .height(3.dp),
                progress = { currentPositionMs.toFloat() / currentPlaying.duration.inWholeMilliseconds },
                color = Color.White,
                trackColor = Color.White.copy(alpha = 0.5f),
            )
        }

        Box(
            modifier =
                Modifier
                    .padding(end = 12.dp)
                    .size(56.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (playbackState == STATE_LOADING) {
                CircularWavyProgressIndicator(
                    modifier =
                        Modifier
                            .size(28.dp),
                    stroke =
                        Stroke(
                            width = with(LocalDensity.current) { 3.dp.toPx() },
                            cap = StrokeCap.Round,
                        ),
                )
            } else {
                IconButton(
                    modifier =
                        Modifier
                            .fillMaxSize(),
                    onClick = togglePlaybackState,
                ) {
                    Icon(
                        modifier =
                            Modifier
                                .size(28.dp),
                        imageVector =
                            if (playbackState == STATE_PAUSED) {
                                PlayArrowIcon
                            } else {
                                Pause
                            },
                        contentDescription = null,
                    )
                }
            }
        }
    }
}
