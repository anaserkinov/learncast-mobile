package me.anasmusa.learncast.screen.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import me.anasmusa.learncast.AppTheme
import me.anasmusa.learncast.Strings
import me.anasmusa.learncast.component.Loader
import me.anasmusa.learncast.component.SnackBarHost
import me.anasmusa.learncast.core.LocalAppEnvironment
import me.anasmusa.learncast.core.appConfig
import me.anasmusa.learncast.string
import me.anasmusa.learncast.theme.icon.Google
import me.anasmusa.learncast.theme.icon.Telegram
import me.anasmusa.learncast.ui.auth.LoginEvent
import me.anasmusa.learncast.ui.auth.LoginIntent
import me.anasmusa.learncast.ui.auth.LoginViewModel
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
        viewModel.subscribe {
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
    var shoTelegramLogin by remember { mutableStateOf(false) }
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
    ) {
        Column(
            modifier =
                Modifier
                    .padding(it),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .weight(0.3f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Image(
                    modifier =
                        Modifier
                            .size(200.dp),
                    painter = painterResource(appConfig.loginLogo),
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
                        .weight(0.5f),
            ) {
                Text(
                    modifier =
                        Modifier
                            .fillMaxWidth(),
                    text = Strings.SIGN_IN_CONTINUE.string(),
                    textAlign = TextAlign.Center,
                )

                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 24.dp),
                ) {
                    Button(
                        modifier =
                            Modifier
                                .fillMaxWidth(),
                        onClick = {
                            shoTelegramLogin = true
                        },
                    ) {
                        Image(
                            modifier =
                                Modifier
                                    .padding(end = 12.dp)
                                    .size(28.dp),
                            imageVector = Telegram,
                            contentDescription = null,
                        )
                        Text(
                            text = Strings.CONTINUE_TELEGRAM.string(),
                        )
                    }

                    Button(
                        modifier =
                            Modifier
                                .padding(top = 12.dp)
                                .fillMaxWidth(),
                        onClick = {
                            login(LoginIntent.LoginWithGoogle)
                        },
                    ) {
                        Image(
                            modifier =
                                Modifier
                                    .padding(end = 12.dp)
                                    .size(28.dp),
                            imageVector = Google,
                            contentDescription = null,
                        )
                        Text(
                            text = Strings.CONTINUE_GOOGLE.string(),
                        )
                    }
                }
            }
        }

        if (shoTelegramLogin) {
            val sheetState =
                rememberModalBottomSheetState(
                    skipPartiallyExpanded = true,
                )
            ModalBottomSheet(
                sheetState = sheetState,
                onDismissRequest = { shoTelegramLogin = false },
                containerColor = Color.White,
            ) {
                TelegramLoginScreen(
                    onGetResult = {
                        shoTelegramLogin = false
                        login(LoginIntent.LoginWithTelegram(it))
                    },
                )
            }
        }

        if (isLoading) {
            Loader()
        }
    }
}
