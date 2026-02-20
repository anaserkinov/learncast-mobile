//
//  PlayerScreen.swift
//  ios
//
//  Created by Anas Erkinjonov on 03/02/26.
//

internal import Kingfisher
internal import Shared
import SwiftUI

struct PlayerScreen: View {
    @Environment(\.env) private var env

    var viewModel: ObservableViewModel<PlayerState, PlayerIntent, PlayerEvent, PlayerViewModel>

    private var state: PlayerState {
        return viewModel.state
    }

    // Drag state
    @State private var isExpanded = false
    @State private var yOffset: CGFloat = -1

    // UI state
    @State private var backgroundColors: [Color] = []
    @State private var collapsedColor: Color = .clear
    @State private var showActionSheet = false
    @State private var showSnipEditScreen = false
    @State private var showQueueScreen = false
    @State private var showSnipScreen = false

    var body: some View {
        GeometryReader { geometry in
            let screenHeight = geometry.size.height + geometry.safeAreaInsets.top + geometry.safeAreaInsets.bottom
            let collapsedOffset = screenHeight - PlayerConstants.miniPlayerBottomPadding - PlayerConstants.collapsedHeight
            let ratio = yOffset / collapsedOffset
            Group {
                if let currentPlaying = state.currentPlaying {
                    ZStack(alignment: .top) {
                        LinearGradient(
                            colors: backgroundColors.isEmpty ? env.playerBackgroundColors : backgroundColors,
                            startPoint: UnitPoint(x: 0.5, y: -0.5),
                            endPoint: UnitPoint(x: 0.5, y: 0.4)
                        )

                        if ratio < 0.8 {
                            expandedPlayerContent(
                                currentPlaying: currentPlaying,
                                screenWidth: geometry.size.width,
                                ratio: ratio,
                                collapsedOffset: collapsedOffset
                            )
                            .padding(.top, geometry.safeAreaInsets.top)
                            .padding(.bottom, geometry.safeAreaInsets.bottom)
                        }

                        if ratio >= 0.8 {
                            collapsedPlayerContent(currentPlaying: currentPlaying)
                                .opacity(1 - (1 - ratio) / 0.2)
                                .transition(.move(edge: .top).combined(with: .opacity))
                                .frame(maxHeight: PlayerConstants.collapsedHeight, alignment: .bottom)
                        }

                        // Queue Screen Overlay
                        if showQueueScreen {
                            QueueScreen(
                                backgroundColors: backgroundColors,
                                onCloseRequested: {
                                    withAnimation {
                                        showQueueScreen = false
                                    }
                                }
                            )
                            .frame(maxWidth: .infinity, maxHeight: .infinity)
                            .transition(.move(edge: .bottom))
                            .padding(.top, geometry.safeAreaInsets.top)
                            .padding(.bottom, geometry.safeAreaInsets.bottom)
                            .zIndex(1)
                        }

                        // Snip Screen Overlay
                        if showSnipScreen {
                            PlayerSnipScreen(
                                backgroundColors: backgroundColors,
                                onCloseRequested: { showSnipScreen = false }
                            )
                            .frame(maxWidth: .infinity, maxHeight: .infinity)
                            .transition(.move(edge: .bottom))
                            .padding(.top, geometry.safeAreaInsets.top)
                            .padding(.bottom, geometry.safeAreaInsets.bottom)
                            .zIndex(1)
                        }
                    }
                    .frame(height: yOffset == -1 ? 0 : screenHeight - (screenHeight - PlayerConstants.collapsedHeight) * ratio)
                    .clipShape(RoundedRectangle(cornerRadius: 6 * ratio))
                    .padding(.horizontal, 6 * ratio)
                    .applyIf(
                        yOffset != -1,
                        transform: { view in
                            view.offset(x: 0, y: yOffset)
                        }
                    )
                    .gesture(
                        DragGesture()
                            .onChanged { value in
                                guard !showQueueScreen else { return }
                                yOffset = max(
                                    0,
                                    min(
                                        collapsedOffset,
                                        isExpanded ? value.translation.height : (collapsedOffset + value.translation.height)
                                    )
                                )
                            }
                            .onEnded { value in
                                guard !showQueueScreen else { return }
                                let currentAnchor = isExpanded ? 0 : collapsedOffset
                                let predictedEnd = currentAnchor + value.predictedEndTranslation.height

                                if predictedEnd <= screenHeight / 2 {
                                    yOffset = 0
                                    isExpanded = true
                                } else {
                                    yOffset = collapsedOffset
                                    isExpanded = false
                                }
                            }
                    )
                    .animation(.spring(response: 0.35, dampingFraction: 0.8), value: yOffset)
                    .animation(.spring(response: 0.35, dampingFraction: 0.8), value: showQueueScreen)
                    .animation(.spring(response: 0.35, dampingFraction: 0.8), value: showSnipScreen)
                    .ignoresSafeArea()
                    .task(
                        id: currentPlaying.coverImagePath,
                        {
                            await loadBackgroundColors(coverImagePath: currentPlaying.coverImagePath)
                        }
                    )
                    .onChange(of: isExpanded, initial: false) { oldValue, newValue in
                        if newValue && state.snipCount == -1 && state.currentPlaying?.referenceType == ReferenceType.lesson {
                            viewModel.handle(intent: PlayerIntentLoadSnipCount())
                        }
                    }
                    .onChange(of: state, initial: false) { oldValue, newValue in
                        if isExpanded && newValue.snipCount == -1 && newValue.currentPlaying?.referenceType == ReferenceType.lesson {
                            viewModel.handle(intent: PlayerIntentLoadSnipCount())
                        }
                    }
                    .sheet(isPresented: $showActionSheet) {
                        if let currentPlaying = state.currentPlaying {
                            PlayerActionSheet(
                                isSnip: currentPlaying.referenceType == .snip,
                                downloadState: currentPlaying.downloadState,
                                percentDownloaded: currentPlaying.percentDownloaded,
                                isCompleted: currentPlaying.status == .completed,
                                isFavourite: currentPlaying.isFavourite,
                                onDismissRequest: { showActionSheet = false },
                                onDownloadClicked: { viewModel.handle(intent: PlayerIntentDownload()) },
                                onRemoveDownloadClicked: { viewModel.handle(intent: PlayerIntentRemoveDownload()) },
                                onCompletedClicked: { viewModel.handle(intent: PlayerIntentToggleCompletedState()) },
                                onFavouriteClicked: { viewModel.handle(intent: PlayerIntentToggleFavourite()) },
                                onDeleteClicked: { viewModel.handle(intent: PlayerIntentDeleteSnip()) }
                            )
                        }
                    }
                    .sheet(isPresented: $showSnipEditScreen) {
                        SnipEditScreen(
                            clientSnipId: currentPlaying.referenceUuid,
                            queueItemId: currentPlaying.id,
                            startAt: (currentPlaying.startMs?.int64Value ?? 0) + state.currentPositionMs,
                            endAt: currentPlaying.endMs != nil
                                ? currentPlaying.endMs!.int64Value
                                : min(
                                    state.currentPositionMs + 5 * 60 * 1000,
                                    currentPlaying.audioDuration
                                ),
                            duration: currentPlaying.audioDuration.millis(),
                            audioPath: currentPlaying.audioPath,
                            note: nil,
                            colors: backgroundColors
                        ) { updated in
                            showSnipEditScreen = false
                            if updated {
                                viewModel.handle(intent: PlayerIntentRefresh())
                            }
                        }
                    }
                }
            }
            .onAppear {
                yOffset = collapsedOffset
            }
        }
        .task {
            await viewModel.collect()
        }
    }

    // MARK: - Expanded Player Content
    @ViewBuilder
    private func expandedPlayerContent(
        currentPlaying: QueueItem,
        screenWidth: CGFloat,
        ratio: CGFloat,
        collapsedOffset: CGFloat
    ) -> some View {
        VStack(alignment: .center, spacing: 0) {
            HStack {
                Button(action: {
                    isExpanded = false
                    yOffset = collapsedOffset
                }) {
                    Image(systemName: "chevron.down")
                        .font(.system(size: 24, weight: .semibold))
                        .foregroundStyle(.white)
                        .frame(width: 44, height: 44)
                }

                Spacer()

                Button(action: {
                    showActionSheet = true
                }) {
                    Image(systemName: "ellipsis")
                        .font(.system(size: 24, weight: .semibold))
                        .foregroundStyle(.white)
                        .frame(width: 44, height: 44)
                }
            }
            .padding(.horizontal, 12)

            Spacer(minLength: 12)

            KFImage(URL(string: currentPlaying.coverImagePath?.normalizeUrl() ?? ""))
                .placeholder {
                    Image("MainLogo")
                        .resizable()
                        .aspectRatio(contentMode: .fit)
                }
                .resizable()
                .frame(width: min(screenWidth * 0.7, 400), height: min(screenWidth * 0.7, 400))
                .clipShape(RoundedRectangle(cornerRadius: 16))
                .shadow(color: .black.opacity(0.3), radius: 20, x: 0, y: 10)
                .padding(.horizontal, 12)

            Spacer(minLength: 16)

            VStack(alignment: .center, spacing: 8) {
                MarqueeView {
                    Text(currentPlaying.title)
                        .font(Typography.titleLarge)
                        .foregroundStyle(.white)
                }
                .frame(height: 24)

                MarqueeView {
                    Text(currentPlaying.subTitle)
                        .font(Typography.titleMedium)
                        .foregroundStyle(.white.opacity(0.7))
                        .lineLimit(1)
                }
                .frame(height: 20)

            }

            Spacer(minLength: 16)

            PlayerSlider(
                value: state.currentPositionMs,
                total: currentPlaying.duration.millis(),
                onValueChangeFinished: { value in
                    viewModel.handle(intent: PlayerIntentSeekTo(value: value))
                }
            )
            .id(currentPlaying.id)
            .padding(.horizontal, 20)

            Spacer(minLength: 24)

            HStack(spacing: 0) {
                Color.clear
                    .frame(width: 56, height: 56)
                    .padding(.leading, 8)

                Spacer()

                // Skip backward 10 seconds
                Button(action: {
                    HapticFeedback.light()
                    viewModel.handle(intent: PlayerIntentSeek(forward: false))
                }) {
                    Image(systemName: "gobackward.10")
                        .font(.system(size: 32))
                        .foregroundStyle(.white)
                }
                .buttonStyle(PlayerControlButtonStyle())

                Spacer()

                // Play/Pause button
                Button(action: {
                    HapticFeedback.medium()
                    viewModel.handle(intent: PlayerIntentTogglePlaybackState())
                }) {
                    ZStack {
                        Circle()
                            .fill(.white)
                            .frame(width: 64, height: 64)

                        if state.playbackState == STATE_LOADING {
                            ProgressView()
                                .tint(.black)
                        } else {
                            Image(systemName: state.playbackState == STATE_PAUSED ? "play.fill" : "pause.fill")
                                .font(.system(size: 28))
                                .foregroundStyle(.black)
                                .offset(x: state.playbackState == STATE_PAUSED ? 2 : 0)
                        }
                    }
                }

                Spacer()

                // Skip forward 30 seconds
                Button(action: {
                    HapticFeedback.light()
                    viewModel.handle(intent: PlayerIntentSeek(forward: true))
                }) {
                    Image(systemName: "goforward.30")
                        .font(.system(size: 32))
                        .foregroundStyle(.white)
                }
                .buttonStyle(PlayerControlButtonStyle())

                Spacer()

                QueueButton(
                    count: state.queuedCount,
                    onClick: {
                        withAnimation {
                            showQueueScreen = true
                        }
                    }
                )
                .frame(width: 56, height: 56)
                .padding(.trailing, 8)

            }
            .padding(.horizontal, 12)

            Spacer(minLength: 48)

            ZStack {
                Button(action: {
                    viewModel.handle(intent: PlayerIntentPause())
                    showSnipEditScreen = true
                }) {
                    HStack(spacing: 8) {
                        Image(systemName: "scissors")
                            .font(.system(size: 20))

                        Text(currentPlaying.referenceUuid.isEmpty ? Strings.shared.CREATE_SNIP.string() : Strings.shared.UPDATE_SNIP.string())
                            .font(Typography.titleLarge)
                            .fontWeight(.semibold)
                    }
                    .foregroundStyle(Colors.onSecondaryContainer)
                    .padding(.horizontal, 10)
                    .padding(.vertical, 14)
                    .background(Color.white.opacity(0.2))
                    .clipShape(Capsule())
                }
                .frame(alignment: .center)
                .buttonStyle(.glassProminent)

                if currentPlaying.referenceType == .lesson {
                    Button(action: {
                        showSnipScreen = true
                    }) {
                        ZStack {
                            Circle()
                                .fill(Color.white.opacity(0.2))
                                .frame(width: 48, height: 48)

                            if state.snipCount == -1 {
                                ProgressView()
                                    .tint(.white)
                            } else {
                                Text(state.snipCount <= 99 ? "\(state.snipCount)" : "99+")
                                    .font(Typography.titleLarge)
                                    .foregroundStyle(.white)
                            }
                        }
                    }
                    .frame(maxWidth: .infinity, alignment: .trailing)
                    .padding(.trailing, 8)
                }
            }
            .padding(.horizontal, 12)

        }
        .fixedSize(horizontal: false, vertical: true)
        .opacity(1.0 - ratio / 0.8)
    }

    // MARK: - Collapsed Player Content
    @ViewBuilder
    private func collapsedPlayerContent(
        currentPlaying: QueueItem
    ) -> some View {
        ZStack {
            ProgressView(
                value: Float(state.currentPositionMs),
                total: Float(currentPlaying.duration.millis())
            )
            .progressViewStyle(LinearProgressViewStyle(tint: .white))
            .frame(height: 2)
            .background(Color.white.opacity(0.5))
            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .bottom)

            ZStack(alignment: .center) {
                KFImage(URL(string: (currentPlaying.coverImagePath ?? "").normalizeUrl()))
                    .placeholder {
                        Image("MainLogo")
                            .resizable()
                            .aspectRatio(contentMode: .fit)
                    }
                    .resizable()
                    .aspectRatio(contentMode: .fill)
                    .clipShape(RoundedRectangle(cornerRadius: 6))
                    .frame(width: 55, height: 55)
                    .overlay(Color.black.opacity(0.3))
                    .blendMode(.sourceAtop)

                if state.playbackState == STATE_LOADING {
                    ProgressView()
                        .tint(.white)
                } else {
                    Button(action: {
                        viewModel.handle(intent: PlayerIntentTogglePlaybackState())
                    }) {
                        Image(systemName: state.playbackState == STATE_PAUSED ? "play.fill" : "pause.fill")
                            .font(.system(size: 20))
                            .frame(width: 28, height: 28)
                    }
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                    .buttonStyle(PlainButtonStyle())
                }
            }
            .frame(width: 55, height: 55)
            .padding(.leading, 8)
            .padding(.top, 4)
            .padding(.bottom, 5)
            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .leading)

            VStack(alignment: .leading, spacing: 0) {
                Text(currentPlaying.title)
                    .font(Typography.titleSmall)
                    .foregroundStyle(.white)
                    .lineLimit(1)

                Text(currentPlaying.subTitle)
                    .font(Typography.bodyMedium)
                    .foregroundStyle(.white.opacity(0.7))
                    .lineLimit(1)
            }
            .padding(.leading, 80)
            .padding(.trailing, 60)
            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .leading)

            QueueButton(
                count: state.queuedCount,
                onClick: {
                    isExpanded = true
                    yOffset = 0
                    showQueueScreen = true
                }
            )
            .frame(width: 56, height: 56)
            .padding(.trailing, 8)
            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .trailing)  // Add this line
        }
        .contentShape(Rectangle())
        .background(collapsedColor)
        .onTapGesture {
            isExpanded = true
            yOffset = 0
        }
    }

    private func loadBackgroundColors(coverImagePath: String?) async {
        let defaultColors = env.playerBackgroundColors
        if let imagePath = coverImagePath {
            backgroundColors = await withCheckedContinuation { continuation in
                KingfisherManager.shared.retrieveImage(
                    with: URL(string: imagePath.normalizeUrl())!,
                    options: [
                        .processor(
                            ResizingImageProcessor(referenceSize: CGSize(width: 112, height: 112))
                        )
                    ]
                ) { result in
                    switch result {
                    case .success(let imageResult):
                        let palette = Palette.Builder(image: imageResult.image)
                            .generate()
                        let vibrant = palette.getVibrantColor(
                            palette.getDarkVibrantColor(
                                palette.getDominantColor(0)
                            )
                        )
                        let colors = vibrant != 0 ? [vibrant.lighten(amount: 0.3), vibrant.darken(amount: 0.6)] : defaultColors
                        continuation.resume(returning: colors)
                    case .failure:
                        continuation.resume(returning: defaultColors)
                    }
                }
            }
        } else {
            backgroundColors = defaultColors
        }

        collapsedColor = lerpColor(backgroundColors.first!, backgroundColors.last!, 0.7)
    }
}

struct PlayerControlButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .scaleEffect(configuration.isPressed ? 0.85 : 1.0)
            .animation(.spring(response: 0.3, dampingFraction: 0.6), value: configuration.isPressed)
    }
}

// MARK: - Preview
#Preview {
    PreviewRoot {
        PlayerScreen(
            viewModel: ObservableViewModel<PlayerState, PlayerIntent, PlayerEvent, PlayerViewModel>()
        )
    }
}
