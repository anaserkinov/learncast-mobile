//
//  PlayerActionSheet.swift
//  ios
//
//  Created by Anas Erkinjonov on 03/02/26.
//

internal import Shared
import SwiftUI

struct PlayerActionSheet: View {
    let isSnip: Bool
    let downloadState: DownloadState?
    let percentDownloaded: Float
    let isCompleted: Bool
    let isFavourite: Bool
    let onDismissRequest: () -> Void
    let onDownloadClicked: () -> Void
    let onRemoveDownloadClicked: () -> Void
    let onCompletedClicked: () -> Void
    let onFavouriteClicked: () -> Void
    let onDeleteClicked: () -> Void

    var body: some View {
        VStack(spacing: 0) {
            // Main actions section
            VStack(spacing: 0) {
                // Download action
                SheetMenuButton(
                    icon: downloadIcon,
                    title: downloadTitle,
                    clip: false,
                    spacing: 28,
                    onClick: {
                        onDismissRequest()
                        handleDownloadAction()
                    }
                )

                // Mark as played/not played (only for non-snips)
                if !isSnip {
                    SheetMenuButton(
                        icon: isCompleted ? "checkmark.circle.badge.xmark" : "checkmark.circle",
                        title: isCompleted ? Strings.shared.MARK_NOT_PLAYED.string() : Strings.shared.MARK_COMPLETED.string(),
                        clip: false,
                        spacing: 28,
                        onClick: {
                            onDismissRequest()
                            onCompletedClicked()
                        }
                    )

                    // Favourite action
                    SheetMenuButton(
                        icon: isFavourite ? "star.fill" : "star",
                        title: isFavourite ? Strings.shared.LESSON_IS_FAVOURITE.string() : Strings.shared.ADD_FAVOURITE.string(),
                        clip: false,
                        spacing: 28,
                        onClick: {
                            onDismissRequest()
                            onFavouriteClicked()
                        }
                    )
                }
            }
            .clipShape(RoundedRectangle(cornerRadius: 12))

            // Delete action (only for snips)
            if isSnip {
                SheetMenuButton(
                    icon: "trash",
                    title: Strings.shared.DELETE.string(),
                    spacing: 28,
                    onClick: {
                        onDismissRequest()
                        onDeleteClicked()
                    }
                )
                .padding(.top, 12)
            }
        }
        .padding(.horizontal, 12)
        .padding(.bottom, 12)
        .padding(.top, 36)
        .presentationBackground(Colors.surfaceContainerLow)
        .presentationDetents([.height(sheetHeight)])
        .presentationDragIndicator(.visible)
    }

    // MARK: - Download State Logic
    private var downloadIcon: String {
        switch downloadState {
        case .none, .stopped, .removing:
            return "arrow.down.circle"
        case .downloading:
            return "arrow.down.circle.fill"
        case .completed:
            return "xmark.circle"
        }
    }

    private var downloadTitle: String {
        switch downloadState {
        case .none, .removing:
            return Strings.shared.DOWNLOAD.string()
        case .stopped:
            return Strings.shared.RESUME_DOWNLOAD.string()
        case .downloading:
            return Strings.shared.DOWNLOADING.string(NSNumber(value: Int(percentDownloaded)))
        case .completed:
            return Strings.shared.REMOVE_DOWNLOAD.string()
        }
    }

    private func handleDownloadAction() {
        switch downloadState {
        case .none, .stopped, .downloading:
            onDownloadClicked()
        case .completed:
            onRemoveDownloadClicked()
        default:
            break  // Do nothing for downloading/removing states
        }
    }

    // MARK: - Sheet Height Calculation
    private var sheetHeight: CGFloat {
        let baseItemHeight: CGFloat = 56
        var itemCount: CGFloat = 1  // Download always present

        if !isSnip {
            itemCount += 2  // Completed + Favourite
        } else {
            itemCount += 1  // Delete button with spacing
        }

        return (itemCount * baseItemHeight) + 40  // 40 for padding
    }
}

// MARK: - Preview
#Preview("Lesson Action Sheet") {
    PreviewRoot {
        Color.black.opacity(0.3)
            .sheet(isPresented: .constant(true)) {
                PlayerActionSheet(
                    isSnip: false,
                    downloadState: .stopped,
                    percentDownloaded: 0,
                    isCompleted: false,
                    isFavourite: true,
                    onDismissRequest: {},
                    onDownloadClicked: {},
                    onRemoveDownloadClicked: {},
                    onCompletedClicked: {},
                    onFavouriteClicked: {},
                    onDeleteClicked: {}
                )
            }
    }
}

#Preview("Snip Action Sheet") {
    PreviewRoot {
        Color.black.opacity(0.3)
            .sheet(isPresented: .constant(true)) {
                PlayerActionSheet(
                    isSnip: true,
                    downloadState: .completed,
                    percentDownloaded: 100,
                    isCompleted: false,
                    isFavourite: false,
                    onDismissRequest: {},
                    onDownloadClicked: {},
                    onRemoveDownloadClicked: {},
                    onCompletedClicked: {},
                    onFavouriteClicked: {},
                    onDeleteClicked: {}
                )
            }
    }
}

#Preview("Downloading State") {
    PreviewRoot {
        Color.black.opacity(0.3)
            .sheet(isPresented: .constant(true)) {
                PlayerActionSheet(
                    isSnip: false,
                    downloadState: .downloading,
                    percentDownloaded: 45,
                    isCompleted: true,
                    isFavourite: false,
                    onDismissRequest: {},
                    onDownloadClicked: {},
                    onRemoveDownloadClicked: {},
                    onCompletedClicked: {},
                    onFavouriteClicked: {},
                    onDeleteClicked: {}
                )
            }
    }
}
