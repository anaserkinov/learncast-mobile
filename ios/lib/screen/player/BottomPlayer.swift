//
//  BottomPlayer.swift
//  ios
//
//  Created by Anas Erkinjonov on 14/02/26.
//

internal import Kingfisher
internal import Shared
import SwiftUI

struct BottomPlayer: View {
    let currentPlaying: QueueItem
    let currentPositionMs: Int64
    let playbackState: Int32
    let backgroundColors: [Color]
    let onClicked: () -> Void
    let togglePlaybackState: () -> Void

    private var progress: Double {
        let duration = currentPlaying.duration.millis()
        guard duration > 0 else { return 0 }
        return Double(currentPositionMs) / Double(duration)
    }

    var body: some View {
        HStack(spacing: 0) {
            // Cover Image
            KFImage(URL(string: currentPlaying.coverImagePath?.normalizeUrl() ?? ""))
                .placeholder {
                    Image("MainLogo")
                        .resizable()
                        .aspectRatio(contentMode: .fit)
                }
                .resizable()
                .clipShape(RoundedRectangle(cornerRadius: 6))
                .frame(width: 55, height: 56)
                .padding(.leading, 12)
                .padding(.vertical, 4)

            // Title and Subtitle
            VStack(alignment: .leading, spacing: 0) {
                Text(currentPlaying.title)
                    .font(Typography.titleSmall)
                    .foregroundColor(.white)
                    .lineLimit(1)
                    .lineSpacing(0)

                Text(currentPlaying.subTitle)
                    .font(Typography.bodyMedium)
                    .foregroundColor(.white.opacity(0.7))
                    .lineLimit(1)
                    .lineSpacing(0)

                // Progress Bar
                ProgressView(value: progress)
                    .progressViewStyle(LinearProgressViewStyle(tint: .white))
                    .frame(height: 3)
                    .background(Color.white.opacity(0.5))
                    .clipShape(Capsule())
                    .padding(.top, 6)
            }
            .padding(.leading, 16)
            .padding(.trailing, 12)
            .padding(.vertical, 4)
            .frame(maxWidth: .infinity, alignment: .leading)

            // Play/Pause Button
            ZStack {
                if playbackState == STATE_LOADING {
                    ProgressView()
                        .progressViewStyle(CircularProgressViewStyle(tint: .white))
                        .frame(width: 28, height: 28)
                } else {
                    Button(action: togglePlaybackState) {
                        Image(systemName: playbackState == STATE_PAUSED ? "play.fill" : "pause.fill")
                            .font(.system(size: 20))
                            .foregroundColor(.white)
                            .frame(width: 56, height: 56)
                    }
                }
            }
            .padding(.trailing, 12)
        }
        .frame(height: 64)
        .background(
            LinearGradient(
                colors: [Color.clear, backgroundColors.last ?? .clear],
                startPoint: .top,
                endPoint: UnitPoint(x: 0.5, y: 0.10)
            )
        )
        .clipShape(RoundedRectangle(cornerRadius: 6))
        .contentShape(Rectangle())
        .onTapGesture {
            onClicked()
        }
    }
}

// MARK: - Preview
private struct BottomPlayerPreview: View {
    @Environment(\.env) var env: AppEnvironment

    var body: some View {
        VStack {
            Spacer()

            BottomPlayer(
                currentPlaying: getSampleQueueItem(id: 1),
                currentPositionMs: 2_700_000,  // 45 minutes
                playbackState: STATE_LOADING,
                backgroundColors: env.playerBackgroundColors,
                onClicked: {},
                togglePlaybackState: {}
            )
            .padding(.horizontal, 8)
            .padding(.bottom, 8)
        }
        .background(Color.black)
    }

}
#Preview {
    PreviewRoot {
        BottomPlayerPreview()
    }
}
