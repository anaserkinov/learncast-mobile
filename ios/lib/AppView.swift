//
//  App.swift
//  ios
//
//  Created by Anas Erkinjonov on 23/01/26.
//

internal import Shared
import SwiftUI

public struct AppView: View {

    @State
    private var viewModel = ObservableViewModel<AppState, AppIntent, AppEvent, AppViewModel>()
    @State
    private var playerViewModel = ObservableViewModel<PlayerState, PlayerIntent, PlayerEvent, PlayerViewModel>()

    @State
    private var env = AppEnvironment()
    @State
    private var selectedTab: Screen = .entrance

    @State
    private var homeNavController = NavController()
    @State
    private var snipsNavController = NavController()
    @State
    private var profileNavController = NavController()

    @State
    private var stringsLoaded = false

    @Namespace private var animation

    @ViewBuilder
    private func navStack(
        navController: Binding<NavController>,
        root: @escaping () -> some View
    ) -> some View {
        NavigationStack(path: navController.backStack) {
            root()
                .navigationDestination(for: Screen.self) { screen in
                    return getView(screen: screen)
                }
        }
    }

    public init() {}

    public var body: some View {
        ZStack(alignment: .bottom) {
            switch selectedTab {
            case .entrance: EmptyView()
            case .login: LoginScreen()
            default:
                TabView(selection: $selectedTab) {
                    navStack(navController: $homeNavController) {
                        HomeScreen()
                    }
                    .environment(\.navController, homeNavController)
                    .tag(Screen.home)
                    .tabItem {
                        Image(systemName: "house")
                        Text(Strings.shared.HOME.string())
                    }

                    navStack(navController: $snipsNavController) {
                        SnipListScreen()
                    }
                    .environment(\.navController, snipsNavController)
                    .tag(Screen.snips)
                    .tabItem {
                        Image(systemName: "scissors")
                        Text(Strings.shared.SNIPS.string())
                    }

                    navStack(navController: $profileNavController) {
                        ProfileScreen()
                    }
                    .environment(\.navController, profileNavController)
                    .tag(Screen.profile)
                    .tabItem {
                        Image(systemName: "person")
                        Text(Strings.shared.PROFILE.string())
                    }
                }

                PlayerScreen(viewModel: playerViewModel)
            }
        }
        .environment(\.env, env)
        .environment(\.navigationAnimation, animation)
        .scrollDismissesKeyboard(.interactively)
        .font(Typography.bodyMedium)
        .preferredColorScheme(.dark)
        .onAppear {
            if !stringsLoaded {
                Resource.shared.setLocale(
                    locale: "uz",
                    onLoad: {
                        stringsLoaded = true
                    })
            }
        }
        .task {
            viewModel.handle(intent: AppIntentLoad())
            await viewModel.collect()
        }
        .task {
            for await event in viewModel.events {
                switch event {
                case is AppEvent.ShowLoginScreen:
                    selectedTab = .login
                case is AppEvent.ShowHomeScreen:
                    selectedTab = .home
                default:
                    break
                }
            }
        }
    }
}

struct PreviewRoot<Content: View>: View {
    let content: Content
    @State
    private var env = AppEnvironment()
    @Namespace private var animation

    init(@ViewBuilder content: () -> Content) {
        PreviewSetup.setup()
        self.content = content()
    }

    var body: some View {
        NavigationStack(root: {
            content
        })
        .environment(\.env, env)
        .environment(\.navigationAnimation, animation)
        .font(Typography.bodyMedium)
        .preferredColorScheme(.dark)
    }
}

enum PreviewSetup {

    private static var isInitialized = false

    static func setup() {
        guard !isInitialized else { return }
        isInitialized = true

        AppConfig.companion.update(
            appName: "LearnCast",
            mainLogo: "MainLogo",
            transparentLogo: "TransparentLogo",
            apiBaseUrl: "https://api.anasmusa.me/learncast/",
            publicBaseUrl: "https://learncast.anasmusa.me",
            telegramBotId: 8_538_344_134,
            googleClientId: "preview-google-client-id"
        )

        Resource.shared.setLocale(locale: "uz", onLoad: {})
    }
}

#Preview {
    PreviewRoot {
        AppView()
    }
}
