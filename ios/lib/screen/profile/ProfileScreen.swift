//
//  HomeScreen.swift
//  ios
//
//  Created by Anas Erkinjonov on 30/01/26.
//

internal import Kingfisher
internal import Shared
import SwiftUI

struct ProfileScreen: View {
    @Environment(\.env) var env: AppEnvironment
    @Environment(\.navController) var navController: NavController

    @State
    private var viewModel = ObservableViewModel<ProfileState, ProfileIntent, ProfileEvent, ProfileViewModel>()

    @State
    private var showLogoutConfirm = false

    var body: some View {
        ZStack {
            if let user = viewModel.state.user {
                VStack(spacing: 0) {
                    Spacer()
                        .frame(height: 56)

                    // User Info Section
                    HStack(alignment: .center, spacing: 12) {
                        // Avatar
                        KFImage(URL(string: user.avatarPath ?? ""))
                            .placeholder {
                                Image("MainLogo")
                                    .resizable()
                                    .aspectRatio(contentMode: .fit)
                            }
                            .resizable()
                            .clipShape(Circle())
                            .frame(width: 96, height: 96)

                        // User Details
                        VStack(alignment: .leading, spacing: 4) {
                            Text(user.firstName)
                                .font(Typography.headlineMedium)
                                .fontWeight(.medium)

                            Text(user.email ?? user.telegramUsername.map { "@\($0)" } ?? "")
                                .font(Typography.bodyMedium)
                                .fontWeight(.medium)
                        }

                        Spacer()
                    }
                    .padding(.horizontal, 16)

                    // Storage Usage Button
                    ProfileButton(
                        icon: "internaldrive",
                        title: Strings.shared.STORAGE_USAGE.string()
                    ) {
                        navController.navigate(screen: .storageUsage)
                    }
                    .padding(.horizontal, 16)
                    .padding(.top, 24)

                    Spacer()

                    // Sign Out Button
                    ProfileButton(
                        icon: "rectangle.portrait.and.arrow.right",
                        title: Strings.shared.SIGNOUT.string()
                    ) {
                        showLogoutConfirm = true
                    }
                    .padding(.horizontal, 16)
                    .padding(.bottom, viewModel.state.isQueueEmpty ? 8 : 80)
                }
                .background(env.backgroundGradient())
            }

            if viewModel.state.isLoading {
                LoaderView()
            }
        }
        .sheet(isPresented: $showLogoutConfirm) {
            ConfirmationBottomSheet(
                title: Strings.shared.SIGNOUT.string() + "?",
                message: Strings.shared.SIGN_OUT_CONFIRM_MESSAGE.string(),
                positiveButtonTitle: Strings.shared.SIGNOUT.string(),
                onConfirm: {
                    showLogoutConfirm = false
                    viewModel.handle(intent: ProfileIntentLogout())
                },
                onDismiss: {
                    showLogoutConfirm = false
                }
            )
        }
        .task {
            await viewModel.collect()
        }
    }
}

private struct ProfileButton: View {
    let icon: String
    let title: String
    var clip: Bool = true
    let onClick: () -> Void

    var body: some View {
        PrimaryButton(
            title: title,
            icon: icon,
            clip: clip,
            padding: EdgeInsets(top: 16, leading: 16, bottom: 16, trailing: 16),
            spacing: 24,
            backgroundColor: Colors.surfaceContainerLowest,
            horizontalAlignment: .leading,
            height: 56,
            onClick: onClick
        )
    }
}

#Preview {
    PreviewRoot {
        ProfileScreen()
    }
}
