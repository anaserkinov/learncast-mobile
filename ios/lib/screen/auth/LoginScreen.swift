//
//  LoginScreen.swift
//  ios
//
//  Created by Anas Erkinjonov on 31/01/26.
//

internal import Shared
import SwiftUI
internal import TelegramLoginData
internal import TelegramLoginWidget

struct LoginScreen: View {
    @Environment(\.env) var env: AppEnvironment

    @State private var viewModel = ObservableViewModel<
        LoginState, LoginIntent, LoginEvent, LoginViewModel
    >()

    @State private var snackbarMessage: String?
    @State private var showSnackbar = false

    private var state: LoginState {
        viewModel.state
    }

    @State private var telegramState = TelegramLoginState(
        botId: 8_538_344_134,
        botUsername: "learncast_bot",
        websiteUrl: "https://learncast.anasmusa.me",
        languageCode: AppConfigKt.appConfig.preferredLang
    )

    var body: some View {
        ZStack {
            GeometryReader { geo in
                VStack(spacing: 0) {
                    VStack(spacing: 16) {
                        Image(appConfig.transparentLogoString)
                            .resizable()
                            .scaledToFit()
                            .frame(width: 200, height: 200)

                        Text(appConfig.appName)
                            .font(.title2)
                            .fontWeight(.semibold)
                            .multilineTextAlignment(.center)
                    }
                    .frame(height: geo.size.height * 0.4)

                    VStack(spacing: 24) {
                        Text(Strings.shared.LOG_IN_CONTINUE.string())
                            .frame(maxWidth: .infinity)
                            .multilineTextAlignment(.center)

                        VStack(spacing: 12) {
                            TelegramLoginButton(
                                state: telegramState,
                                onResult: { result in
                                    if let data = result as? TelegramLoginResultSuccess {
                                        viewModel.handle(
                                            intent: LoginIntentLoginWithTelegram(
                                                id: data.id, firstName: data.firstName, lastName: data.lastName, username: data.username, photoUrl: data.photoUrl, authDate: data.authDate,
                                                hash: data.hash_)
                                        )
                                    }
                                }
                            ) { state in
                                HStack {
                                    TelegramButtonIcon()
                                        .foregroundStyle(TelegramDefaults.primaryColor)
                                    TelegramButtonText(state: state)
                                        .foregroundStyle(.black)
                                    TelegramButtontUserPhotoBox(
                                        state: state, preservesSpace: false,
                                        progress: {
                                            TelegramButtonCircularProgress(tint: .black)
                                        })
                                }
                                .frame(maxWidth: .infinity)
                                .padding(.horizontal, 8)
                            }
                            .font(Typography.labelLarge)
                            .tint(.white)
                            .buttonStyle(.glassProminent)

                            Button {
                                viewModel.handle(intent: LoginIntentLoginWithGoogle())
                            } label: {
                                HStack {
                                    Image("Google")
                                        .resizable()
                                        .frame(width: 24, height: 24)

                                    Text(Strings.shared.CONTINUE_GOOGLE.string())
                                        .font(Typography.labelLarge)
                                }
                                .foregroundStyle(Colors.onPrimary)
                                .frame(maxWidth: .infinity)
                            }
                            .buttonStyle(.glassProminent)
                        }
                    }
                    .padding(.horizontal, 16)
                    .padding(.top, 72)
                    .frame(height: geo.size.height * 0.6, alignment: .top)
                }
                .padding(.top)
            }

            if state.isLoading {
                LoaderView()
            }
        }
        .background(env.backgroundGradient())
        .snackbar(
            isPresented: $showSnackbar,
            message: snackbarMessage ?? ""
        )
        .task {
            await viewModel.collect()
        }
        .task {
            for await event in viewModel.events {
                switch event {
                case let showError as LoginEvent.ShowError:
                    snackbarMessage = showError.message
                    showSnackbar = true
                default:
                    break
                }
            }
        }
    }
}

#Preview {
    PreviewRoot {
        LoginScreen()
    }
}
