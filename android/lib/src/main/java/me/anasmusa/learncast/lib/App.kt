package me.anasmusa.learncast.lib

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.simplestarts.app.ui.theme.icons.PersonIcon
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.launch
import me.anasmusa.learncast.Resource
import me.anasmusa.learncast.Strings
import me.anasmusa.learncast.core.AppConfig
import me.anasmusa.learncast.lib.core.ProvideAppEnvironment
import me.anasmusa.learncast.lib.nav.Screen
import me.anasmusa.learncast.lib.nav.entryProvider
import me.anasmusa.learncast.lib.screen.player.PlayerScreen
import me.anasmusa.learncast.lib.theme.MontserratTypography
import me.anasmusa.learncast.lib.theme.backgroundColors
import me.anasmusa.learncast.lib.theme.darkScheme
import me.anasmusa.learncast.lib.theme.icon.CutIcon
import me.anasmusa.learncast.lib.theme.icon.HomeIcon
import me.anasmusa.learncast.lib.theme.playerBackgroundColors
import me.anasmusa.learncast.parseStringsXml
import me.anasmusa.learncast.string
import me.anasmusa.learncast.ui.AppEvent
import me.anasmusa.learncast.ui.AppIntent
import me.anasmusa.learncast.ui.AppViewModel
import org.koin.compose.viewmodel.koinViewModel
import java.io.BufferedReader
import java.io.File
import kotlin.math.min

@Composable
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

        val backStack = rememberNavBackStack(Screen.Entrance)
        val hazeState = rememberHazeState()

        var selectedPage by rememberSaveable { mutableIntStateOf(0) }

        LaunchedEffect(viewModel) {
            launch {
                viewModel.subscribe {
                    when (it) {
                        is AppEvent.ShowLoginScreen -> {
                            backStack.clear()
                            backStack.add(Screen.Login)
                        }

                        is AppEvent.ShowHomeScreen -> {
                            selectedPage = 0
                            backStack.clear()
                            backStack.add(Screen.Home)

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

        if (state.isLoggedIn == true) {
            val activity = LocalActivity.current
            LaunchedEffect(selectedPage) {
                backStack.removeAll { it != Screen.Home && it != Screen.Snips && it != Screen.Profile }
                val screen =
                    when (selectedPage) {
                        0 -> Screen.Home
                        1 -> Screen.Snips
                        else -> Screen.Profile
                    }
                val index = backStack.indexOf(screen)
                backStack.add(
                    if (index != -1) {
                        backStack.removeAt(index)
                    } else {
                        screen
                    },
                )
            }
            BackHandler(
                enabled = backStack.size <= 3 && backStack.all { it == Screen.Home || it == Screen.Snips || it == Screen.Profile },
            ) {
                if (selectedPage != 0) {
                    selectedPage = 0
                } else {
                    activity?.finish()
                }
            }
        }

        ProvideAppEnvironment(
            backStack = backStack,
            hazeState = hazeState,
            backgroundColors = backgroundColors,
            playerBackgroundColors = playerBackgroundColors,
            isNightMode = false,
            changeNightMode = {},
        ) {
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
                                        ).fillMaxWidth()
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
                                    val icon: ImageVector
                                    val name: Int

                                    when (it) {
                                        0 -> {
                                            icon = HomeIcon
                                            name = Strings.HOME
                                        }

                                        1 -> {
                                            icon = CutIcon
                                            name = Strings.SNIPS
                                        }

                                        else -> {
                                            icon = PersonIcon
                                            name = Strings.PROFILE
                                        }
                                    }

                                    NavigationBarItem(
                                        selected = selectedPage == it,
                                        icon = {
                                            Icon(
                                                icon,
                                                contentDescription = null,
                                            )
                                        },
                                        label = { Text(name.string()) },
                                        onClick = {
                                            selectedPage = it
                                        },
                                    )
                                }
                            }
                        }
                    }
                },
            ) { _ ->
                val animationSpec = tween<IntOffset>(300)
                NavDisplay(
                    modifier =
                        Modifier
                            .hazeSource(state = hazeState),
                    entryDecorators =
                        listOf(
                            rememberSaveableStateHolderNavEntryDecorator(),
                            rememberViewModelStoreNavEntryDecorator(),
                        ),
                    backStack = backStack,
                    onBack = { backStack.removeLastOrNull() },
                    entryProvider = entryProvider(),
                    transitionSpec = {
                        slideInHorizontally(
                            initialOffsetX = { it },
                            animationSpec = animationSpec,
                        ) togetherWith
                            slideOutHorizontally(
                                targetOffsetX = { (-it * 0.2f).toInt() },
                                animationSpec = animationSpec,
                            )
                    },
                    popTransitionSpec = {
                        slideInHorizontally(
                            initialOffsetX = { (-it * 0.2f).toInt() },
                            animationSpec = animationSpec,
                        ) togetherWith
                            slideOutHorizontally(
                                targetOffsetX = { it },
                                animationSpec = animationSpec,
                            )
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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AppTheme(content: @Composable () -> Unit) {
    AppConfig.update(
        appName = "LearnCast",
        mainLogo = R.drawable.logo,
        loginLogo = R.drawable.logo_transparent,
        apiBaseUrl = "http://localhost:3000",
        publicBaseUrl = "http://localhost:3000",
        telegramBotId = 8292515516L,
        googleClientId = "",
    )
    val file =
        File("/Users/anas/AndroidStudioProjects/LearnCast/android/lib/src/main/assets/values/strings.xml")
    Resource.setStrings(
        "en",
        parseStringsXml(
            file.bufferedReader().use(BufferedReader::readText),
        ),
    )

    ProvideAppEnvironment(
        backStack = rememberNavBackStack(),
        hazeState = rememberHazeState(),
        backgroundColors = backgroundColors,
        playerBackgroundColors = playerBackgroundColors,
        isNightMode = false,
        changeNightMode = {},
    ) {
        CompositionLocalProvider(
            LocalContentColor provides Color.White,
        ) {
            MaterialExpressiveTheme(
                colorScheme = darkScheme,
                typography = MontserratTypography(),
                content = content,
            )
        }
    }
}
