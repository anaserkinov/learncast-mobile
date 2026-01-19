package me.anasmusa.learncast.lib.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import me.anasmusa.learncast.Strings
import me.anasmusa.learncast.string

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfirmationBottomSheet(
    title: String? = null,
    message: String? = null,
    positiveButtonTitle: String? = null,
    negativeButtonTitle: String? = null,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState =
        rememberModalBottomSheetState(
            skipPartiallyExpanded = true,
        )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        start = 20.dp,
                        end = 20.dp,
                        bottom = 16.dp,
                    ),
            horizontalAlignment = Alignment.Start,
        ) {
            title?.let {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                Spacer(modifier = Modifier.height(12.dp))
            }

            message?.let {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            SheetMenuWhiteButton(
                modifier = Modifier.fillMaxWidth(),
                title = positiveButtonTitle ?: Strings.YES.string(),
                icon = null,
                horizontalArrangement = Arrangement.Center,
                onClick = {
                    onConfirm()
                },
            )

            Spacer(modifier = Modifier.height(12.dp))

            SheetMenuButton(
                modifier = Modifier.fillMaxWidth(),
                title = negativeButtonTitle ?: Strings.CANCEL.string(),
                icon = null,
                horizontalArrangement = Arrangement.Center,
                onClick = onDismiss,
            )

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
