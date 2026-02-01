//
//  App.swift
//  ios
//
//  Created by Anas Erkinjonov on 23/01/26.
//


import SwiftUI
import Shared


public struct AppView: View {
    
    @State
    private var viewModel = ObservableViewModel<AppState, AppIntent, AppEvent, AppViewModel>()

    @State
    private var env = AppEnvironment()
    @State
    private var selectedTab: Screen = .Entrance
    
    public init() {}
    
    public var body: some View {
        _AppView(
            state: viewModel.state,
            env: env,
            selectedTab: $selectedTab
        )
        .task {
            viewModel.handle(intent: AppIntentLoad())
            await viewModel.collect()
        }
        .onChange(of: viewModel.event, initial: false, { _, event in
            guard let event = event else { return }
            switch event {
            case is AppEvent.ShowLoginScreen:
                selectedTab = .Login
            case is AppEvent.ShowHomeScreen:
                selectedTab = .Home
            default:
                break
            }
        })
    }
}

private struct _AppView: View {
    
    var state: AppState
    var env: AppEnvironment
    
    @State
    private var homeNavController = NavController()
    @State
    private var snipsNavController = NavController()
    @State
    private var profileNavController = NavController()
    
    
    @Binding
    var selectedTab: Screen
    
    @State
    private var stringsLoaded = false
    
    @ViewBuilder
    private func navStack(
        navController: Binding<NavController>,
        root: @escaping() -> some View
    ) -> some View {
        NavigationStack(path: navController.backStack) {
            root()
                .navigationDestination(for: Screen.self) { screen in
                    getView(screen: screen)
                }
        }
    }
    
    public var body: some View {
        ZStack {
            switch selectedTab {
            case .Entrance: EmptyView()
            case .Login: LoginScreen()
            default :
                TabView(selection: $selectedTab) {
                    navStack(navController: $homeNavController){
                        HomeScreen()
                    }
                    .tag(Screen.Home)
                    .tabItem {
                        Image(systemName: "house")
                        Text(Strings.shared.HOME.string())
                    }
                    
                    navStack(navController: $snipsNavController){
                        SnipListScreen()
                    }
                    .tag(Screen.Snips)
                    .tabItem {
                        Image(systemName: "scissors")
                        Text(Strings.shared.SNIPS.string())
                    }
                    
                    navStack(navController: $profileNavController){
                        ProfileScreen()
                    }
                    .tag(Screen.Profile)
                    .tabItem {
                        Image(systemName: "person")
                        Text(Strings.shared.PROFILE.string())
                    }
                }
                .navigationBarBackButtonHidden()
            }
        }
        .font(Typography.BodyMedium)
        .preferredColorScheme(.dark)
        .onAppear {
            if !stringsLoaded {
                Resource.shared.setLocale(locale: "uz", onLoad: {
                    stringsLoaded = true
                })
            }
        }
    }
}


struct PreviewRoot<Content: View>: View {
    let content: Content
    @State
    private var env = AppEnvironment()
    
    init(@ViewBuilder content: () -> Content) {
        PreviewSetup.setup()
        self.content = content()
    }
    
    var body: some View {
        content
            .environment(\.env, env)
            .font(Typography.BodyMedium)
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
            telegramBotId: 8538344134,
            googleClientId: "preview-google-client-id"
        )
        
        Resource.shared.setLocale(locale: "uz", onLoad: {})
    }
}

#Preview {
    PreviewRoot {
        _AppView(
            state: AppState(isLoggedIn: true),
            env: AppEnvironment(),
            selectedTab: .constant(.Home)
        )
    }
}
