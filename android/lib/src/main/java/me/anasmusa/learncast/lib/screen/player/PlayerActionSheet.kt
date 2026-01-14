package me.anasmusa.learncast.lib.screen.player

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
import me.anasmusa.learncast.Strings
import me.anasmusa.learncast.data.model.DownloadState
import me.anasmusa.learncast.lib.AppTheme
import me.anasmusa.learncast.lib.component.SheetMenuButton
import me.anasmusa.learncast.lib.theme.icon.Delete
import me.anasmusa.learncast.lib.theme.icon.DoneAll
import me.anasmusa.learncast.lib.theme.icon.Download
import me.anasmusa.learncast.lib.theme.icon.RemoveDone
import me.anasmusa.learncast.lib.theme.icon.RemoveDownload
import me.anasmusa.learncast.lib.theme.icon.Star
import me.anasmusa.learncast.lib.theme.icon.StarFilled
import me.anasmusa.learncast.string

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
            onDeleteClicked = {},
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
            onDeleteClicked = {},
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
    onDeleteClicked: () -> Unit,
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
            Column(
                modifier =
                    Modifier
                        .clip(RoundedCornerShape(8.dp)),
            ) {
                SheetMenuButton(
                    icon =
                        when (downloadState) {
                            null, DownloadState.STOPPED, DownloadState.REMOVING -> Download
                            DownloadState.DOWNLOADING -> Download
                            DownloadState.COMPLETED -> RemoveDownload
                        },
                    title =
                        when (downloadState) {
                            null, DownloadState.REMOVING -> Strings.DOWNLOAD.string()
                            DownloadState.STOPPED -> Strings.RESUME_DOWNLOAD.string()
                            DownloadState.DOWNLOADING -> Strings.DOWNLOADING.string(percentDownloaded.toInt())
                            DownloadState.COMPLETED -> Strings.REMOVE_DOWNLOAD.string()
                        },
                    clip = false,
                    paddingBetween = 28.dp,
                    onClick = {
                        onDismissRequest()
                        onDownloadClicked()
                    },
                )

                if (!isSnip) {
                    SheetMenuButton(
                        icon = if (isCompleted) RemoveDone else DoneAll,
                        title = if (isCompleted) Strings.MARK_NOT_PLAYED.string() else Strings.MARK_COMPLETED.string(),
                        clip = false,
                        paddingBetween = 28.dp,
                        onClick = {
                            onDismissRequest()
                            onCompletedClicked()
                        },
                    )
                    SheetMenuButton(
                        icon = if (isFavourite) StarFilled else Star,
                        title = if (isFavourite) Strings.LESSON_IS_FAVOURITE.string() else Strings.ADD_FAVOURITE.string(),
                        clip = false,
                        paddingBetween = 28.dp,
                        onClick = {
                            onDismissRequest()
                            onFavouriteClicked()
                        },
                    )
                }
            }

            if (isSnip) {
                Spacer(modifier = Modifier.height(12.dp))

                SheetMenuButton(
                    icon = Delete,
                    title = Strings.DELETE.string(),
                    paddingBetween = 28.dp,
                    onClick = {
                        onDismissRequest()
                        onDeleteClicked()
                    },
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
