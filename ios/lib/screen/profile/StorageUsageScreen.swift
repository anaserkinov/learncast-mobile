//
//  StorageUsageScreen.swift
//  ios
//
//  Created by Anas Erkinjonov on 16/02/26.
//

internal import Shared
import SwiftUI

struct StorageUsageScreen: View {
    @Environment(\.env) var env: AppEnvironment

    @State
    private var viewModel = ObservableViewModel<StorageState, StorageIntent, StorageEvent, StorageViewModel>()

    @State private var showClearCacheConfirm = false
    @State private var showClearDownloadsConfirm = false

    var body: some View {
        ZStack {
            VStack(spacing: 0) {
                // Cache Section
                HStack {
                    Text(Strings.shared.CACHE.string())
                        .font(Typography.bodyLarge)
                        .foregroundColor(.primary)

                    Spacer()

                    if let cacheSize = viewModel.state.cacheSize {
                        Text(cacheSize)
                            .font(Typography.bodyLarge)
                            .foregroundColor(.primary)
                    } else {
                        ProgressView()
                            .progressViewStyle(CircularProgressViewStyle())
                            .frame(width: 24, height: 24)
                    }
                }
                .padding(.vertical, 8)

                StorageButton(title: Strings.shared.CLEAR_CACHE.string()) {
                    showClearCacheConfirm = true
                }

                // Downloads Section
                HStack {
                    Text(Strings.shared.DOWNLOADS.string())
                        .font(Typography.bodyLarge)
                        .foregroundColor(.primary)

                    Spacer()

                    if let downloadSize = viewModel.state.downloadSize {
                        Text(downloadSize)
                            .font(Typography.bodyLarge)
                            .foregroundColor(.primary)
                    } else {
                        ProgressView()
                            .progressViewStyle(CircularProgressViewStyle())
                            .frame(width: 24, height: 24)
                    }
                }
                .padding(.top, 24)
                .padding(.bottom, 8)

                StorageButton(title: Strings.shared.CLEAR_DOWNLOAD.string()) {
                    showClearDownloadsConfirm = true
                }

                Spacer()
            }
            .padding(.horizontal, 16)
            .background(env.backgroundGradient())
            .navigationTitle(Strings.shared.STORAGE_USAGE.string())
            .navigationBarTitleDisplayMode(.large)

            if viewModel.state.isLoading {
                LoaderView()
            }
        }
        .task {
            await viewModel.collect()
        }
        .sheet(isPresented: $showClearCacheConfirm) {
            ConfirmationBottomSheet(
                title: Strings.shared.CLEAR_CACHE.string() + "?",
                message: Strings.shared.CLEAR_CACHE_CONFIRM_MESSAGE.string(),
                positiveButtonTitle: Strings.shared.CLEAR.string(),
                onConfirm: {
                    showClearCacheConfirm = false
                    viewModel.handle(intent: StorageIntentClearCache())
                },
                onDismiss: {
                    showClearCacheConfirm = false
                }
            )
        }
        .sheet(isPresented: $showClearDownloadsConfirm) {
            ConfirmationBottomSheet(
                title: Strings.shared.REMOVE_DOWNLOADS.string() + "?",
                message: Strings.shared.CLEAR_DOWNLOADS_CONFIRM_MESSAGE.string(),
                positiveButtonTitle: Strings.shared.CLEAR.string(),
                onConfirm: {
                    showClearDownloadsConfirm = false
                    viewModel.handle(intent: StorageIntentClearDownloads())
                },
                onDismiss: {
                    showClearDownloadsConfirm = false
                }
            )
        }
    }
}

// MARK: - Storage Button Component
struct StorageButton: View {
    let title: String
    let onClick: () -> Void

    var body: some View {
        PrimaryButton(
            title: title,
            padding: EdgeInsets(top: 8, leading: 8, bottom: 8, trailing: 8),
            spacing: 24,
            horizontalAlignment: .center,
            onClick: onClick,
        )
    }
}

// MARK: - Preview
#Preview {
    PreviewRoot {
        NavigationStack {
            StorageUsageScreen()
        }
    }
}
