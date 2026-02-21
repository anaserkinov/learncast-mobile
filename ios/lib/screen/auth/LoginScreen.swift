//
//  LoginScreen.swift
//  ios
//
//  Created by Anas Erkinjonov on 31/01/26.
//

internal import Shared
import SwiftUI

struct LoginScreen: View {

    @State private var viewModel = ObservableViewModel<
        LoginState, LoginIntent, LoginEvent, LoginViewModel
    >()

    @State private var snackbarMessage: String?
    @State private var showSnackbar = false

    var body: some View {
        _LoginScreen(
            state: viewModel.state,
            handle: { intent in
                viewModel.handle(intent: intent)
            }
        )
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

private struct _LoginScreen: View {
    @Environment(\.env) var env: AppEnvironment

    var state: LoginState
    var handle: (Shared.LoginIntent) -> Void

    @State private var showTelegramLogin = false

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
                        Text(Strings.shared.SIGN_IN_CONTINUE.string())
                            .frame(maxWidth: .infinity)
                            .multilineTextAlignment(.center)

                        VStack(spacing: 12) {
                            Button {
                                showTelegramLogin = true
                            } label: {
                                HStack(spacing: 12) {
                                    Image("Telegram")
                                        .resizable()
                                        .frame(width: 28, height: 28)

                                    Text(Strings.shared.CONTINUE_TELEGRAM.string())
                                        .font(Typography.labelLarge)
                                }
                                .foregroundStyle(Colors.onPrimary)
                                .frame(maxWidth: .infinity)
                            }
                            .buttonStyle(.borderedProminent)

                            Button {
                                handle(LoginIntentLoginWithGoogle())
                            } label: {
                                HStack(spacing: 12) {
                                    Image("Google")
                                        .resizable()
                                        .frame(width: 28, height: 28)

                                    Text(Strings.shared.CONTINUE_GOOGLE.string())
                                        .font(Typography.labelLarge)
                                }
                                .foregroundStyle(Colors.onPrimary)
                                .frame(maxWidth: .infinity)
                            }
                            .buttonStyle(.borderedProminent)
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
        .sheet(isPresented: $showTelegramLogin) {
            TelegramLoginScreen(
                onGetResult: { result in
                    showTelegramLogin = false
                    handle(LoginIntentLoginWithTelegram(hash: result))
                },
                onCancel: {
                    showTelegramLogin = false
                }
            )
            .presentationDetents([.fraction(0.75)])
            .presentationDragIndicator(.visible)
        }
    }
}

#Preview {
    PreviewRoot {
        _LoginScreen(
            state: LoginState(isLoading: false),
            handle: { _ in }
        )
    }
}
