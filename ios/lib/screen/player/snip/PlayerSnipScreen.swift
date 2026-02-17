//
//  PlayerSnipScreen.swift
//  ios
//
//  Created by Anas Erkinjonov on 15/02/26.
//

internal import Shared
import SwiftUI

struct PlayerSnipScreen: View {
    @Environment(\.dismiss) var dismiss
    @Environment(\.env) var env: AppEnvironment

    let backgroundColors: [Color]
    let onCloseRequested: () -> Void

    @State
    private var viewModel = ObservableViewModel<PlayerSnipState, PlayerSnipIntent, PlayerSnipEvent, PlayerSnipViewModel>()

    @State private var selectedSnip: Snip?

    var body: some View {
        ZStack {
            VStack(spacing: 0) {
                // Header
                ZStack {
                    Button {
                        onCloseRequested()
                        dismiss()
                    } label: {
                        Image(systemName: "xmark")
                            .font(.system(size: 20, weight: .semibold))
                            .foregroundColor(.white)
                            .frame(width: 44, height: 44)
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)

                    Text(Strings.shared.SNIPS.string())
                        .font(Typography.headlineSmall)
                        .foregroundColor(.white)
                }
                .padding(.horizontal, 12)
                .padding(.top, 8)

                // Snips List
                PagingList(
                    flow: viewModel.viewModel.snips,
                    id: \.?.id
                ) { (snip: Snip?) in
                    if let snip {
                        SnipCell(snip: snip) {
                            selectedSnip = snip
                        }
                    } else {
                        EmptyView()
                    }
                }
                .padding(.top, 8)

                // Bottom Player
                if let currentPlaying = viewModel.state.currentPlaying {
                    BottomPlayer(
                        currentPlaying: currentPlaying,
                        currentPositionMs: viewModel.state.currentPositionMs,
                        playbackState: viewModel.state.playbackState,
                        backgroundColors: backgroundColors,
                        onClicked: {
                            onCloseRequested()
                            dismiss()
                        },
                        togglePlaybackState: {
                            viewModel.handle(intent: PlayerSnipIntentTogglePlayback())
                        }
                    )
                }
            }
            .background(
                LinearGradient(
                    colors: backgroundColors,
                    startPoint: UnitPoint(x: 0.5, y: -0.5),
                    endPoint: UnitPoint(x: 0.5, y: 0.4)
                )
                .ignoresSafeArea()
            )
        }
        .task {
            await viewModel.collect()
        }
        .onChange(
            of: viewModel.state.currentPlaying?.referenceId, initial: true,
            { oldValue, newValue in
                if let lessonId = newValue {
                    viewModel.handle(intent: PlayerSnipIntentLoad(lessonId: lessonId))
                }
            }
        )
        .sheet(item: $selectedSnip) { snip in
            PlayerSnipActionSheet(
                item: snip,
                onDismissRequest: {
                    selectedSnip = nil
                },
                onPlay: {
                    selectedSnip = nil
                    viewModel.handle(intent: PlayerSnipIntentPlay(item: snip))
                    onCloseRequested()
                    dismiss()
                }
            )
        }
    }
}

// MARK: - Preview
private struct PlayerSnipScreenPreview: View {

    @Environment(\.env) var env: AppEnvironment

    var body: some View {
        PlayerSnipScreen(
            backgroundColors: env.playerBackgroundColors,
            onCloseRequested: {}
        )
    }
}
#Preview {
    PreviewRoot {
        PlayerSnipScreenPreview()
    }
}
