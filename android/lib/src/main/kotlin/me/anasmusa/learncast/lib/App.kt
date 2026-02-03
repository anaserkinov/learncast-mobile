package me.anasmusa.learncast.lib

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import me.anasmusa.learncast.lib.theme.icon.PersonIcon
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.anasmusa.learncast.Resource
import me.anasmusa.learncast.Resource.string
import me.anasmusa.learncast.Strings
import me.anasmusa.learncast.core.AppConfig
import me.anasmusa.learncast.lib.core.ProvideAppEnvironment
import me.anasmusa.learncast.lib.nav.ProvideNavController
import me.anasmusa.learncast.lib.nav.Screen
import me.anasmusa.learncast.lib.nav.entryProvider
import me.anasmusa.learncast.lib.screen.auth.LoginScreen
import me.anasmusa.learncast.lib.screen.player.PlayerScreen
import me.anasmusa.learncast.lib.theme.MontserratTypography
import me.anasmusa.learncast.lib.theme.darkScheme
import me.anasmusa.learncast.lib.theme.icon.CutIcon
import me.anasmusa.learncast.lib.theme.icon.HomeIcon
import me.anasmusa.learncast.parseStringsXml
import me.anasmusa.learncast.ui.AppEvent
import me.anasmusa.learncast.ui.AppIntent
import me.anasmusa.learncast.ui.AppViewModel
import org.koin.compose.viewmodel.koinViewModel
import java.io.BufferedReader
import kotlin.math.min

@Composable
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
fun App(
    backgroundColors: List<Color>,
    playerBackgroundColors: List<Color>,
) {
    val permissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission(),
        ) { granted -> }

    val activity = LocalActivity.current
    val density = LocalDensity.current
    val expandedPx = LocalWindowInfo.current.containerSize.height
    val windowBottomInset = NavigationBarDefaults.windowInsets.getBottom(density)

    val anchors =
        remember(windowBottomInset) {
            val collapsedPx = with(density) { (80 + 64).dp.toPx() }
            DraggableAnchors {
                "expanded" at 0f
                "collapsed" at (expandedPx - collapsedPx - windowBottomInset)
            }
        }
    val draggableState =
        remember(anchors) {
            AnchoredDraggableState(
                initialValue = "collapsed",
                anchors = anchors,
            )
        }

    var stringsLoaded by remember { mutableStateOf(Resource.isLoaded) }

    Resource.setLocale("uz") {
        stringsLoaded = true
    }

    if (stringsLoaded) {
        val viewModel = koinViewModel<AppViewModel>()
        val state by viewModel.state.collectAsState()

        val hazeState = rememberHazeState()

        var selectedPage by rememberSaveable(
            saver = Saver(
                save = { it.value.toPosition() },
                restore = { mutableStateOf(it.toScreen()) }
            )
        ) { mutableStateOf<Screen>(Screen.Entrance) }

        LaunchedEffect(viewModel) {
            launch {
                viewModel.subscribe(this) {
                    when (it) {
                        is AppEvent.ShowLoginScreen -> {
                            selectedPage = Screen.Login
                        }

                        is AppEvent.ShowHomeScreen -> {
                            selectedPage = Screen.Home

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && activity != null) {
                                if (ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS) ==
                                    PackageManager.PERMISSION_GRANTED
                                ) {
                                    // FCM SDK (and your app) can post notifications.
                                } else if (shouldShowRequestPermissionRationale(activity, Manifest.permission.POST_NOTIFICATIONS)) {
                                    // TODO: display an educational UI explaining to the user the features that will be enabled
                                    //       by them granting the POST_NOTIFICATION permission. This UI should provide the user
                                    //       "OK" and "No thanks" buttons. If the user selects "OK," directly request the permission.
                                    //       If the user selects "No thanks," allow the user to continue without notifications.
                                } else {
                                    // Directly ask for the permission
                                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            }
                        }
                    }
                }
            }
            viewModel.handle(AppIntent.Load)
        }

        ProvideAppEnvironment(
            hazeState = hazeState,
            backgroundColors = backgroundColors,
            playerBackgroundColors = playerBackgroundColors
        ) {
            CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onBackground) {
                when (selectedPage) {
                    Screen.Entrance -> {}
                    Screen.Login -> LoginScreen()
                    else -> {
                        val homeBackStack = rememberNavBackStack(Screen.Home)
                        val snipsBackStack = rememberNavBackStack(Screen.Snips)
                        val profileBackStack = rememberNavBackStack(Screen.Profile)

                        Scaffold(
                            bottomBar = {
                                if (state.isLoggedIn == true) {
                                    Box(
                                        modifier =
                                            Modifier
                                                .fillMaxSize(),
                                    ) {
                                        PlayerScreen(
                                            modifier = Modifier,
                                            draggableState = draggableState,
                                        )
                                        NavigationBar(
                                            modifier =
                                                Modifier
                                                    .align(Alignment.BottomCenter)
                                                    .offset(
                                                        y =
                                                            min(
                                                                draggableState.anchors.maxPosition() - draggableState.offset,
                                                                80f + windowBottomInset,
                                                            ).dp,
                                                    )
                                                    .fillMaxWidth()
                                                    .hazeEffect(
                                                        state = hazeState,
                                                        style =
                                                            HazeStyle(
                                                                tint =
                                                                    HazeTint(
                                                                        color =
                                                                            backgroundColors
                                                                                .last()
                                                                                .copy(alpha = 0.5f),
                                                                    ),
                                                            ),
                                                    ),
                                            containerColor = Color.Transparent,
                                        ) {
                                            repeat(3) {
                                                val navKey: Screen
                                                val icon: ImageVector
                                                val nameKey: String

                                                when (it) {
                                                    0 -> {
                                                        navKey = Screen.Home
                                                        icon = HomeIcon
                                                        nameKey = Strings.HOME
                                                    }

                                                    1 -> {
                                                        navKey = Screen.Snips
                                                        icon = CutIcon
                                                        nameKey = Strings.SNIPS
                                                    }

                                                    else -> {
                                                        navKey = Screen.Profile
                                                        icon = PersonIcon
                                                        nameKey = Strings.PROFILE
                                                    }
                                                }

                                                NavigationBarItem(
                                                    selected = selectedPage == navKey,
                                                    icon = {
                                                        Icon(
                                                            icon,
                                                            contentDescription = null,
                                                        )
                                                    },
                                                    label = { Text(nameKey.string()) },
                                                    onClick = {
                                                        selectedPage = it.toScreen()
                                                    },
                                                )
                                            }
                                        }
                                    }
                                }
                            },
                        ) { _ ->
                            val decorators = listOf(
                                rememberSaveableStateHolderNavEntryDecorator<NavKey>(),
                                rememberViewModelStoreNavEntryDecorator()
                            )
                            val animationSpec = tween<IntOffset>(300)

                            val backStack = when (selectedPage) {
                                Screen.Home -> homeBackStack
                                Screen.Snips -> snipsBackStack
                                else -> profileBackStack
                            }
                            var withAnimation by remember(selectedPage) { mutableStateOf(false) }
                            LaunchedEffect(withAnimation) {
                                if (!withAnimation){
                                    delay(350)
                                    withAnimation = true
                                }
                            }
                            ProvideNavController(backStack) {
                                NavDisplay(
                                    modifier =
                                        Modifier
                                            .hazeSource(state = hazeState),
                                    entryDecorators = decorators,
                                    backStack = backStack,
                                    onBack = { backStack.removeLastOrNull() },
                                    entryProvider = entryProvider(),
                                    transitionSpec = {
                                        if (withAnimation)
                                            slideInHorizontally(
                                                initialOffsetX = { it },
                                                animationSpec = animationSpec,
                                            ) togetherWith
                                                slideOutHorizontally(
                                                    targetOffsetX = { (-it * 0.2f).toInt() },
                                                    animationSpec = animationSpec,
                                                )
                                        else EnterTransition.None togetherWith ExitTransition.None
                                    },
                                    popTransitionSpec = {
                                        if (withAnimation)
                                            slideInHorizontally(
                                                initialOffsetX = { (-it * 0.2f).toInt() },
                                                animationSpec = animationSpec,
                                            ) togetherWith
                                                slideOutHorizontally(
                                                    targetOffsetX = { it },
                                                    animationSpec = animationSpec,
                                                )
                                        else EnterTransition.None togetherWith ExitTransition.None
                                    },
                                    predictivePopTransitionSpec = {
                                        slideInHorizontally(
                                            initialOffsetX = { (-it * 0.2f).toInt() },
                                            animationSpec = animationSpec,
                                        ) togetherWith
                                            slideOutHorizontally(targetOffsetX = { it })
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun Screen.toPosition() = when (this) {
    Screen.Home -> 0
    Screen.Snips -> 1
    Screen.Profile -> 2
    else -> -1
}

private fun Int.toScreen() = when (this) {
    0 -> Screen.Home
    1 -> Screen.Snips
    2 -> Screen.Profile
    else -> Screen.Login
}


@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AppTheme(content: @Composable () -> Unit) {
    AppConfig.update(
        appName = "LearnCast",
        mainLogo = R.drawable.logo,
        transparentLogo = R.drawable.logo_transparent,
        apiBaseUrl = "http://localhost:3000",
        publicBaseUrl = "http://localhost:3000",
        telegramBotId = 8292515516L,
        googleClientId = "",
    )
    val assets = LocalContext.current.assets
    Resource.setStrings(
        "en",
        parseStringsXml(
            assets.open("strings.xml").bufferedReader().use(BufferedReader::readText),
        ),
    )

    ProvideAppEnvironment(
        hazeState = rememberHazeState(),
        backgroundColors = listOf(
            Color(0.094f, 0.122f, 0.2f, 1.0f),
            Color(0.055f, 0.071f, 0.122f, 1.0f),
        ),
        playerBackgroundColors = listOf(
            Color(0.224f, 0.282f, 0.42f, 1.0f),
            Color(0.075f, 0.094f, 0.157f, 1.0f),
        )
    ) {
        CompositionLocalProvider(
            LocalContentColor provides Color.White,
        ) {
            val homeBackStack = rememberNavBackStack(Screen.Home)

            ProvideNavController(homeBackStack) {
                MaterialExpressiveTheme(
                    colorScheme = darkScheme,
                    typography = MontserratTypography(),
                    content = content,
                )
            }
        }
    }
}
