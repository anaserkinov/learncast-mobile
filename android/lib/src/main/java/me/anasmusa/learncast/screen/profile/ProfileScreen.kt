package me.anasmusa.learncast.screen.profile

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import me.anasmusa.learncast.AppTheme
import me.anasmusa.learncast.Strings
import me.anasmusa.learncast.component.Loader
import me.anasmusa.learncast.component.PrimaryButton
import me.anasmusa.learncast.core.LocalAppEnvironment
import me.anasmusa.learncast.core.appConfig
import me.anasmusa.learncast.data.model.User
import me.anasmusa.learncast.nav.Screen
import me.anasmusa.learncast.theme.icon.Logout
import me.anasmusa.learncast.theme.icon.Storage
import me.anasmusa.learncast.ui.profile.ProfileIntent
import me.anasmusa.learncast.ui.profile.ProfileState
import me.anasmusa.learncast.ui.profile.ProfileViewModel
import org.koin.compose.viewmodel.koinViewModel

@Preview
@Composable
private fun ProfileScreenPreview(){
    AppTheme {
        _ProfileScreen(
            state = ProfileState(
                isLoading = false,
                user = User(1L, "Name", "Last", null, null,"ggtgg")
            ),
            signout = {}
        )
    }
}

@Composable
fun ProfileScreen() {
    val viewModel = koinViewModel<ProfileViewModel>()
    val state by viewModel.state.collectAsState()

    _ProfileScreen(
        state,
        signout = {
            viewModel.handle(ProfileIntent.Logout)
        }
    )
}

@Composable
private fun Button(
    modifier: Modifier = Modifier,
    icon: ImageVector?,
    title: Int,
    clip: Boolean = true,
    onClick: () -> Unit
){
    PrimaryButton(
        modifier = modifier,
        icon = icon,
        title = title,
        clip = clip,
        padding = PaddingValues(16.dp),
        paddingBetween = 24.dp,
        onClick = onClick
    )
}

@Composable
private fun _ProfileScreen(
    state: ProfileState,
    signout: () -> Unit
){
    val env = LocalAppEnvironment.current

    Scaffold(
        modifier = Modifier
            .background(
                Brush.verticalGradient(
                    colors = LocalAppEnvironment.current.backgroundColors,
                    endY = with(LocalDensity.current) { 100.dp.toPx() }
                )
            ),
        containerColor = Color.Transparent,
    ){
        if (state.user != null){
            Column(
                modifier = Modifier
                    .padding(it)
                    .fillMaxSize()
                    .padding(
                        start = 16.dp,
                        end = 16.dp,
                        bottom = 88.dp + if (state.isQueueEmpty) 0.dp else 64.dp
                    )
            ) {

                Spacer(
                    modifier = Modifier
                        .height(56.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    AsyncImage(
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .size(96.dp)
                            .clip(CircleShape),
                        model = if (state.user!!.avatarPath != null)
                            state.user!!.avatarPath!!
                        else
                            appConfig.mainLogo,
                        contentScale = ContentScale.Crop,
                        contentDescription = null,
                        onError = {
                            Log.e("error", it.result.throwable.message ?: "fdfd")
                        }
                    )

                    Column() {

                        Text(
                            text = state.user!!.firstName,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Medium
                        )

                        Text(
                            text = state.user!!.email ?:
                            state.user!!.telegramUsername?.let { "@$it" } ?: "",
                            fontWeight = FontWeight.Medium
                        )

                    }

                }

                Button (
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp),
                    icon = Storage,
                    title = Strings.storage_usage
                ) {
                    env.navigate(Screen.StorageUsageScreen)
                }

                Spacer(
                    modifier = Modifier.weight(1f)
                )

                Button (
                    modifier = Modifier
                        .fillMaxWidth(),
                    icon = Logout,
                    title = Strings.signout
                ) {
                    signout()
                }
            }
        }

        if (state.isLoading)
            Loader()
    }

}