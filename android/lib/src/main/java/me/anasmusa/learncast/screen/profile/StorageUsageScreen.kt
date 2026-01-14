package me.anasmusa.learncast.screen.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import me.anasmusa.learncast.AppTheme
import me.anasmusa.learncast.Strings
import me.anasmusa.learncast.component.Loader
import me.anasmusa.learncast.component.PrimaryButton
import me.anasmusa.learncast.core.LocalAppEnvironment
import me.anasmusa.learncast.string
import me.anasmusa.learncast.theme.icon.ArrowBackIcon
import me.anasmusa.learncast.ui.profile.StorageIntent
import me.anasmusa.learncast.ui.profile.StorageState
import me.anasmusa.learncast.ui.profile.StorageViewModel
import org.koin.compose.viewmodel.koinViewModel

@Preview
@Composable
private fun StorageUsageScreenPreview() {
    AppTheme {
        _StorageUsageScreen(
            state =
                StorageState(
                    isLoading = false,
                    cacheSize = null,
                    downloadSize = "256 MB",
                ),
            clearCache = {},
            clearDownload = {},
        )
    }
}

@Composable
fun StorageUsageScreen() {
    val viewModel = koinViewModel<StorageViewModel>()
    val state by viewModel.state.collectAsState()

    _StorageUsageScreen(
        state,
        clearCache = {
            viewModel.handle(StorageIntent.ClearCache)
        },
        clearDownload = {
            viewModel.handle(StorageIntent.ClearDownloads)
        },
    )
}

@Composable
private fun Button(
    modifier: Modifier = Modifier,
    title: Int,
    clip: Boolean = true,
    onClick: () -> Unit,
) {
    PrimaryButton(
        modifier = modifier.fillMaxWidth(),
        icon = null,
        title = title,
        clip = clip,
        padding = PaddingValues(8.dp),
        paddingBetween = 24.dp,
        horizontalArrangement = Arrangement.Center,
        onClick = onClick,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun _StorageUsageScreen(
    state: StorageState,
    clearCache: () -> Unit,
    clearDownload: () -> Unit,
) {
    val env = LocalAppEnvironment.current

    Scaffold(
        modifier =
            Modifier
                .background(
                    Brush.verticalGradient(
                        colors = LocalAppEnvironment.current.backgroundColors,
                        endY = with(LocalDensity.current) { 100.dp.toPx() },
                    ),
                ),
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                title = {
                    Text(
                        modifier = Modifier,
                        text = Strings.STORAGE_USAGE.string(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = { env.popBack() },
                    ) {
                        Icon(
                            imageVector = ArrowBackIcon,
                            contentDescription = null,
                        )
                    }
                },
            )
        },
        containerColor = Color.Transparent,
    ) {
        Column(
            modifier =
                Modifier
                    .padding(it)
                    .fillMaxSize()
                    .padding(
                        start = 16.dp,
                        end = 16.dp,
                        top = 8.dp,
                    ),
        ) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(text = Strings.CACHE.string())
                if (state.cacheSize != null) {
                    Text(text = state.cacheSize!!)
                } else {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
            Button(
                title = Strings.CLEAR_CACHE,
                onClick = clearCache,
            )

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(text = Strings.DOWNLOADS.string())
                if (state.downloadSize != null) {
                    Text(text = state.downloadSize!!)
                } else {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
            Button(
                title = Strings.CLEAR_DOWNLOAD,
                onClick = clearDownload,
            )
        }
    }

    if (state.isLoading) {
        Loader()
    }
}
