//
//  LoginScreen.swift
//  ios
//
//  Created by Anas Erkinjonov on 31/01/26.
//

internal import Shared
import SwiftUI
internal import TelegramLogin

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
                                config: TelegramLoginConfig(
                                    clientId: appConfig.telegramBotClientId,
                                    redirectURI: appConfig.publicBaseUrl
                                )
                            ) { result in
                                if case .success(let data) = result {
                                    viewModel.handle(
                                        intent: LoginIntentLoginWithTelegram(idToken: data.idToken)
                                    )
                                }
                            } content: {
                                HStack {
                                    TelegramButtonIcon()
                                        .foregroundStyle(TelegramDefaults.primaryColor)
                                    Text(Strings.shared.CONTINUE_TELEGRAM.string())
                                        .foregroundStyle(.black)
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
