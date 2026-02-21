//
//  SnipEditScreen.swift
//  ios
//
//  Created by Anas Erkinjonov on 14/02/26.
//

import Combine
internal import Shared
import SwiftUI

struct SnipEditScreen: View {
    @Environment(\.env) var env: AppEnvironment

    let clientSnipId: String
    let queueItemId: Int64
    let startAt: Int64
    let endAt: Int64
    let duration: Int64
    let audioPath: String
    let noteInitial: String?
    let colors: [Color]
    let onDismissRequest: (Bool) -> Void

    @State
    private var viewModel = ObservableViewModel<SnipEditState, SnipEdiIntent, SnipEditEvent, SnipEditViewModel>()

    @State private var note: String
    @State private var rangeSelectorState: TimeRangeSelectorState

    // Debounce publishers for range changes
    @State private var startDebouncePublisher = PassthroughSubject<Int, Never>()
    @State private var endDebouncePublisher = PassthroughSubject<Int, Never>()
    @State private var cancellables = Set<AnyCancellable>()

    init(
        clientSnipId: String,
        queueItemId: Int64,
        startAt: Int64,
        endAt: Int64,
        duration: Int64,
        audioPath: String,
        note: String?,
        colors: [Color],
        onDismissRequest: @escaping (Bool) -> Void
    ) {
        self.clientSnipId = clientSnipId
        self.queueItemId = queueItemId
        self.startAt = startAt
        self.endAt = endAt
        self.duration = duration
        self.audioPath = audioPath
        self.noteInitial = note
        self.colors = colors
        self.onDismissRequest = onDismissRequest

        _note = State(initialValue: note ?? "")
        _rangeSelectorState = State(
            initialValue: TimeRangeSelectorState(
                initialStart: Int(startAt / 1000),
                initialEnd: Int(endAt / 1000),
                total: Int(duration / 1000)
            )
        )
    }

    var body: some View {
        ZStack {
            VStack(spacing: 0) {
                // Header with duration and close button
                ZStack {
                    HStack {
                        Button {
                            onDismissRequest(false)
                        } label: {
                            Image(systemName: "xmark")
                                .font(.system(size: 20, weight: .semibold))
                                .foregroundStyle(.white)
                                .frame(width: 44, height: 44)
                        }

                        Spacer()
                    }

                    Text((rangeSelectorState.end - rangeSelectorState.start).formatTime())
                        .font(Typography.bodyLarge)
                        .foregroundColor(.white)
                }
                .padding(.horizontal, 12)
                .padding(.top, 12)

                // Time Range Selector
                TimeRangeSelector(
                    state: rangeSelectorState,
                    color: Color.white.opacity(0.3),
                    currentPosition: viewModel.state.playbackState == STATE_PLAYING
                        ? Int(viewModel.state.currentPositionMs / 1000)
                        : -1
                )
                .padding(.top, 16)
                .padding(.horizontal, 12)

                // Note TextField
                TextField(
                    Strings.shared.WRITE_NOTE.string(),
                    text: $note,
                    axis: .vertical
                )
                .textFieldStyle(.plain)
                .padding(16)
                .background(
                    RoundedRectangle(cornerRadius: 12)
                        .fill(lerpColor(colors.first!, colors.last!, 0.8))
                )
                .foregroundColor(.white)
                .onChange(of: note) { oldValue, newValue in
                    if newValue.count > 128 {
                        note = String(newValue.prefix(128))
                    }
                }
                .padding(.top, 16)
                .padding(.horizontal, 12)

                // Action Buttons
                HStack(spacing: 16) {
                    // Play/Stop Button
                    if viewModel.state.playbackState == STATE_LOADING {
                        ProgressView()
                            .progressViewStyle(CircularProgressViewStyle(tint: .white))
                            .padding(.leading, 20)
                    } else {
                        Button {
                            if viewModel.state.playbackState == STATE_PLAYING {
                                viewModel.handle(intent: SnipEdiIntentStop())
                            } else {
                                viewModel.handle(
                                    intent: SnipEdiIntentStart(
                                        from: Int32(rangeSelectorState.start),
                                        to: Int32(rangeSelectorState.end)
                                    )
                                )
                            }
                        } label: {
                            HStack(spacing: 8) {
                                Image(systemName: viewModel.state.playbackState == STATE_PLAYING ? "stop.fill" : "play.fill")

                                Text(
                                    viewModel.state.playbackState == STATE_PLAYING
                                        ? Strings.shared.STOP.string()
                                        : Strings.shared.PLAY.string()
                                )
                            }
                            .font(Typography.titleMedium)
                            .foregroundColor(Colors.onSecondaryContainer)
                            .padding(.horizontal, 16)
                            .padding(.vertical, 12)
                        }
                        .buttonStyle(.glassProminent)
                    }

                    Spacer()

                    // Save Button
                    if viewModel.state.isLoading {
                        ProgressView()
                            .progressViewStyle(CircularProgressViewStyle(tint: .white))
                            .padding(.trailing, 20)
                    } else {
                        Button {
                            viewModel.handle(
                                intent: SnipEdiIntentSave(
                                    clientSnipId: clientSnipId,
                                    queueItemId: queueItemId,
                                    start: Int32(rangeSelectorState.start),
                                    end: Int32(rangeSelectorState.end),
                                    note: note
                                )
                            )
                        } label: {
                            Text(Strings.shared.SAVE.string())
                                .font(Typography.titleMedium)
                                .foregroundColor(Colors.onSecondaryContainer)
                                .padding(.horizontal, 24)
                                .padding(.vertical, 12)
                        }
                        .buttonStyle(.glassProminent)
                    }
                }
                .padding(.horizontal, 12)
                .padding(.top, 16)
            }
        }
        .presentationDragIndicator(.hidden)
        .presentationDetents([.height(calculateHeight())])
        .presentationBackground(
            LinearGradient(
                colors: colors,
                startPoint: UnitPoint(x: 0.5, y: -0.4),
                endPoint: UnitPoint(x: 0.5, y: 0.2)
            )
        )
        .task {
            await viewModel.collect()
        }
        .onAppear {
            viewModel.handle(
                intent: SnipEdiIntentInit(
                    clientSnipId: clientSnipId,
                    audioPath: audioPath,
                    startPosition: startAt
                )
            )

            setupDebouncers()
        }
        .task {
            for await event in viewModel.events {
                switch event {
                case is SnipEditEventFinish:
                    onDismissRequest(true)

                case let loadedEvent as SnipEditEventOnSnipLoaded:
                    if let loadedNote = loadedEvent.note {
                        note = loadedNote
                    }

                case is SnipEditEventShowError:
                    // Handle error if needed
                    break

                default:
                    break
                }
            }
        }
        .onChange(of: rangeSelectorState.start) { _, newValue in
            startDebouncePublisher.send(newValue)
        }
        .onChange(of: rangeSelectorState.end) { _, newValue in
            endDebouncePublisher.send(newValue)
        }
    }

    // MARK: - Helper Methods

    private func setupDebouncers() {
        // Debounce start changes
        startDebouncePublisher
            .debounce(for: .seconds(1), scheduler: DispatchQueue.main)
            .sink { value in
                viewModel.handle(
                    intent: SnipEdiIntentStart(
                        from: Int32(value),
                        to: Int32(value + 5)
                    )
                )
            }
            .store(in: &cancellables)

        // Debounce end changes
        endDebouncePublisher
            .debounce(for: .seconds(1), scheduler: DispatchQueue.main)
            .sink { value in
                viewModel.handle(
                    intent: SnipEdiIntentStart(
                        from: Int32(value - 5),
                        to: Int32(value)
                    )
                )
            }
            .store(in: &cancellables)
    }

    private func calculateHeight() -> CGFloat {
        return 12 + 60 + 16 + 120 + 16 + 52 + 16 + 58  // padding + title + padding + range selector + padding + input + padding + buttons
    }

}

// MARK: - Preview
private struct SnipEditScreenPreview: View {
    @Environment(\.env) var env: AppEnvironment

    var body: some View {
        Color.clear
            .sheet(isPresented: .constant(true)) {
                SnipEditScreen(
                    clientSnipId: "",
                    queueItemId: 0,
                    startAt: 0,
                    endAt: 11000,
                    duration: 3_600_000,
                    audioPath: "",
                    note: "Sample note",
                    colors: env.playerBackgroundColors,
                    onDismissRequest: { _ in }
                )
            }
    }
}

#Preview {
    PreviewRoot {
        SnipEditScreenPreview()
    }
}
