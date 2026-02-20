//
//  QueueScreen.swift
//  ios
//
//  Created by Anas Erkinjonov on 14/02/26.
//

internal import Shared
import SwiftUI
internal import UniformTypeIdentifiers

struct QueueScreen: View {
    @Environment(\.dismiss) var dismiss
    @Environment(\.env) var env: AppEnvironment

    let backgroundColors: [Color]
    let onCloseRequested: () -> Void

    @State
    private var viewModel = ObservableViewModel<QueueState, QueueIntent, QueueEvent, QueueViewModel>()

    @State private var swipingId: Int64 = -1
    @State private var selectedItem: QueueItem?
    @State private var draggedItem: QueueItem?

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

                    Text(Strings.shared.QUEUE.string())
                        .font(Typography.headlineSmall)
                        .foregroundColor(.white)
                }
                .padding(.horizontal, 12)
                .padding(.top, 8)

                // Queue List
                List {
                    if let currentPlaying = viewModel.state.currentPlaying {
                        ForEach([currentPlaying]) { item in
                            HStack(spacing: 8) {
                                QueueItemCell(
                                    queueItem: currentPlaying,
                                    paddingTop: 0,
                                    onClick: {

                                    }
                                )
                            }
                            .padding(.vertical, 4)
                            .padding(.horizontal, 8)
                            .background(
                                ZStack {
                                    // Background color
                                    RoundedRectangle(cornerRadius: 8)
                                        .fill(backgroundColors.last?.opacity(0.5) ?? Color.clear)

                                    // Progress overlay
                                    GeometryReader { geometry in
                                        let progress = CGFloat(viewModel.state.currentPositionMs) / CGFloat(currentPlaying.duration.millis())
                                        Rectangle()
                                            .fill(Color.white.opacity(0.15))
                                            .frame(width: geometry.size.width * progress)
                                    }
                                }
                            )
                            .clipShape(RoundedRectangle(cornerRadius: 8))
                            .contentShape(Rectangle())
                            .listRowBackground(Color.clear)
                            .listRowSeparator(.hidden)
                            .listRowInsets(EdgeInsets(top: 4, leading: 12, bottom: 4, trailing: 12))
                        }
                        .onDelete { indexSet in
                            viewModel.handle(intent: QueueIntentRemove(id: currentPlaying.id))
                        }
                    }

                    // Playing Next Header
                    HStack {
                        Text(Strings.shared.PLAYING_NEXT.string())
                            .font(Typography.headlineSmall)
                            .foregroundColor(.white)

                        Spacer()

                        Button {
                            viewModel.handle(intent: QueueIntentClear())
                        } label: {
                            Text(Strings.shared.CLEAR.string())
                                .font(Typography.headlineSmall)
                                .foregroundColor(.white)
                        }
                    }
                    .listRowBackground(Color.clear)
                    .listRowSeparator(.hidden)
                    .listRowInsets(EdgeInsets(top: 4, leading: 12, bottom: 4, trailing: 12))

                    let queuedItems = (viewModel.state.queuedItems as? [QueueItem]) ?? []
                    ForEach(queuedItems) { item in
                        QueueItemCell(
                            queueItem: item,
                            paddingTop: 0,
                            onClick: {
                                selectedItem = item
                            }
                        )
                        .listRowBackground(Color.clear)
                        .listRowSeparator(.hidden)
                        .listRowInsets(EdgeInsets(top: 4, leading: 12, bottom: 4, trailing: 12))
                        .contentShape(.dragPreview, Rectangle())
                    }
                    .onDelete { indexSet in
                        viewModel.handle(intent: QueueIntentRemove(id: queuedItems[indexSet.first!].id))
                    }
                    .onMove { fromSet, to in
                        viewModel.handle(intent: QueueIntentMove(from: Int32(fromSet.first!), to: Int32(to)))
                    }
                }
                .listStyle(.plain)
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
                            viewModel.handle(intent: QueueIntentTogglePlayback())
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
        .sheet(item: $selectedItem) { item in
            QueueActionSheet(
                item: item,
                onDismissRequest: {
                    selectedItem = nil
                },
                onPlay: {
                    viewModel.handle(intent: QueueIntentPlay(item: selectedItem!))
                    selectedItem = nil
                }
            )
        }
    }
}

// MARK: - Drop Delegate for Drag & Drop Reordering
struct QueueDropDelegate: DropDelegate {
    let item: QueueItem
    let items: [QueueItem]
    @Binding var draggedItem: QueueItem?
    let onMove: (Int, Int) -> Void

    func performDrop(info: DropInfo) -> Bool {
        draggedItem = nil
        return true
    }

    func dropEntered(info: DropInfo) {
        guard let draggedItem = draggedItem,
            draggedItem.id != item.id,
            let fromIndex = items.firstIndex(where: { $0.id == draggedItem.id }),
            let toIndex = items.firstIndex(where: { $0.id == item.id })
        else { return }

        onMove(fromIndex, toIndex)
    }

    func dropUpdated(info: DropInfo) -> DropProposal? {
        return DropProposal(operation: .move)
    }
}

// MARK: - Preview
private struct QueueScreenPreview: View {
    @Environment(\.env) var env: AppEnvironment

    var body: some View {
        QueueScreen(
            backgroundColors: env.playerBackgroundColors,
            onCloseRequested: {}
        )
    }
}

#Preview {
    PreviewRoot {
        QueueScreenPreview()
    }
}
