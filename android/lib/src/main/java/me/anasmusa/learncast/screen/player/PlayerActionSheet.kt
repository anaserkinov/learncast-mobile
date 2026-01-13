package me.anasmusa.learncast.screen.player

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import me.anasmusa.learncast.AppTheme
import me.anasmusa.learncast.Strings
import me.anasmusa.learncast.component.SheetMenuButton
import me.anasmusa.learncast.data.model.DownloadState
import me.anasmusa.learncast.string
import me.anasmusa.learncast.theme.icon.Delete
import me.anasmusa.learncast.theme.icon.DoneAll
import me.anasmusa.learncast.theme.icon.Download
import me.anasmusa.learncast.theme.icon.RemoveDone
import me.anasmusa.learncast.theme.icon.RemoveDownload
import me.anasmusa.learncast.theme.icon.Star
import me.anasmusa.learncast.theme.icon.StarFilled

@Preview
@Composable
private fun LessonPlayerActionSheetPreview() {
    AppTheme {
        PlayerActionSheet(
            isSnip = false,
            downloadState = DownloadState.STOPPED,
            percentDownloaded = 0f,
            isCompleted = false,
            isFavourite = true,
            onDismissRequest = {},
            onDownloadClicked = {},
            onCompletedClicked = {},
            onFavouriteClicked = {},
            onDeleteClicked = {}
        )
    }
}

@Preview
@Composable
private fun SnipPlayerActionSheetPreview() {
    AppTheme {
        PlayerActionSheet(
            isSnip = true,
            downloadState = DownloadState.STOPPED,
            percentDownloaded = 0f,
            isCompleted = false,
            isFavourite = true,
            onDismissRequest = {},
            onDownloadClicked = {},
            onCompletedClicked = {},
            onFavouriteClicked = {},
            onDeleteClicked = {}
        )
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerActionSheet(
    isSnip: Boolean,
    downloadState: DownloadState?,
    percentDownloaded: Float,
    isCompleted: Boolean,
    isFavourite: Boolean,
    onDismissRequest: () -> Unit,
    onDownloadClicked: () -> Unit,
    onCompletedClicked: () -> Unit,
    onFavouriteClicked: () -> Unit,
    onDeleteClicked: () -> Unit
) {
    ModalBottomSheet(
        modifier = Modifier
            .fillMaxWidth(),
        sheetState = rememberModalBottomSheetState(
            skipPartiallyExpanded = true
        ),
        onDismissRequest = onDismissRequest
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 12.dp
                )
        ) {

            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
            ) {

                SheetMenuButton(
                    icon = when(downloadState){
                        null, DownloadState.STOPPED, DownloadState.REMOVING -> Download
                        DownloadState.DOWNLOADING -> Download
                        DownloadState.COMPLETED -> RemoveDownload
                    },
                    title = when(downloadState){
                        null, DownloadState.REMOVING -> Strings.download.string()
                        DownloadState.STOPPED -> Strings.resume_download.string()
                        DownloadState.DOWNLOADING -> Strings.downloading.string(percentDownloaded.toInt())
                        DownloadState.COMPLETED -> Strings.remove_download.string()
                    },
                    clip = false,
                    paddingBetween = 28.dp,
                    onClick = {
                        onDismissRequest()
                        onDownloadClicked()
                    }
                )

                if (!isSnip){
                    SheetMenuButton(
                        icon = if (isCompleted) RemoveDone else DoneAll,
                        title = if (isCompleted) Strings.mark_not_played.string() else Strings.mark_completed.string(),
                        clip = false,
                        paddingBetween = 28.dp,
                        onClick = {
                            onDismissRequest()
                            onCompletedClicked()
                        }
                    )
                    SheetMenuButton(
                        icon = if (isFavourite) StarFilled else Star,
                        title = if (isFavourite) Strings.lesson_is_favourite.string() else Strings.add_favourite.string(),
                        clip = false,
                        paddingBetween = 28.dp,
                        onClick = {
                            onDismissRequest()
                            onFavouriteClicked()
                        }
                    )
                }
            }

            if (isSnip){
                Spacer(modifier = Modifier.height(12.dp))

                SheetMenuButton(
                    icon = Delete,
                    title = Strings.delete.string(),
                    paddingBetween = 28.dp,
                    onClick = {
                        onDismissRequest()
                        onDeleteClicked()
                    }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}