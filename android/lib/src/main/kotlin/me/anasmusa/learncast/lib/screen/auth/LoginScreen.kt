package me.anasmusa.learncast.lib.screen.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import me.anasmusa.learncast.Strings
import me.anasmusa.learncast.core.appConfig
import me.anasmusa.learncast.core.resource.Resource.string
import me.anasmusa.learncast.lib.AppTheme
import me.anasmusa.learncast.lib.component.Loader
import me.anasmusa.learncast.lib.component.SnackBarHost
import me.anasmusa.learncast.lib.core.LocalAppEnvironment
import me.anasmusa.learncast.lib.theme.icon.Google
import me.anasmusa.learncast.ui.auth.LoginEvent
import me.anasmusa.learncast.ui.auth.LoginIntent
import me.anasmusa.learncast.ui.auth.LoginViewModel
import me.anasmusa.shared.TelegramLoginResult
import me.anasmusa.telegramloginwidget.TelegramButtonIcon
import me.anasmusa.telegramloginwidget.TelegramDefaults
import me.anasmusa.telegramloginwidget.TelegramLoginButton
import me.anasmusa.telegramloginwidget.rememberTelegramLoginState
import org.koin.compose.viewmodel.koinViewModel

@Preview
@Composable
private fun LoginScreenPreview() {
    AppTheme {
        _LoginScreen()
    }
}

@Composable
fun LoginScreen() {
    val viewModel = koinViewModel<LoginViewModel>()
    val state by viewModel.state.collectAsState()

    val snackBarState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.subscribe(this) {
            when (it) {
                is LoginEvent.ShowError -> snackBarState.showSnackbar(it.message)
            }
        }
    }

    _LoginScreen(
        isLoading = state.isLoading,
        login = {
            viewModel.handle(it)
        },
        snackBarState = snackBarState,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun _LoginScreen(
    isLoading: Boolean = false,
    login: (intent: LoginIntent) -> Unit = {},
    snackBarState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val gradientStartY = LocalWindowInfo.current.containerSize.height * (-0.5f)
    val gradientEndY = LocalWindowInfo.current.containerSize.height * 0.5f

    Scaffold(
        modifier =
            Modifier
                .background(
                    Brush.verticalGradient(
                        colors = LocalAppEnvironment.current.backgroundColors,
                        startY = gradientStartY,
                        endY = gradientEndY,
                    ),
                ),
        containerColor = Color.Transparent,
        snackbarHost = {
            SnackBarHost(snackBarState)
        },
    ) { paddingValues ->
        Column(
            modifier =
                Modifier
                    .padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .weight(0.4f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Image(
                    modifier =
                        Modifier
                            .size(200.dp),
                    painter = painterResource(appConfig.transparentLogoInt),
                    contentDescription = null,
                )

                Text(
                    modifier = Modifier,
                    text = appConfig.appName,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            Column(
                modifier =
                    Modifier
                        .padding(top = 72.dp)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .weight(0.6f),
            ) {
                Text(
                    modifier =
                        Modifier
                            .fillMaxWidth(),
                    text = Strings.LOG_IN_CONTINUE.string(),
                    textAlign = TextAlign.Center,
                )

                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 24.dp),
                ) {
                    TelegramLoginButton(
                        state =
                            rememberTelegramLoginState(
                                botId = appConfig.telegramBotId,
                                botUsername = appConfig.telegramBotUsername,
                                websiteUrl = appConfig.publicBaseUrl,
                                languageCode = appConfig.defaultLang,
                            ),
                        onResult = {
                            if (it is TelegramLoginResult.Success) {
                                login(
                                    LoginIntent.LoginWithTelegram(
                                        id = it.id,
                                        firstName = it.firstName,
                                        lastName = it.lastName,
                                        username = it.username,
                                        photoUrl = it.photoUrl,
                                        authDate = it.authDate,
                                        hash = it.hash,
                                    ),
                                )
                            }
                        },
                        left = {
                            TelegramButtonIcon(tint = TelegramDefaults.primaryColor)
                        },
                        modifier =
                            Modifier
                                .fillMaxWidth(),
                        colors =
                            TelegramDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = Color.Black,
                            ),
                    )

                    Button(
                        modifier =
                            Modifier
                                .padding(top = 8.dp)
                                .fillMaxWidth(),
                        onClick = {
                            login(LoginIntent.LoginWithGoogle)
                        },
                    ) {
                        Image(
                            modifier =
                                Modifier
                                    .padding(end = 12.dp)
                                    .size(24.dp),
                            imageVector = Google,
                            contentDescription = null,
                        )
                        Text(
                            text = Strings.CONTINUE_GOOGLE.string(),
                        )
                        Spacer(
                            modifier =
                                Modifier
                                    .width(24.dp),
                        )
                    }
                }
            }
        }

        if (isLoading) {
            Loader()
        }
    }
}
