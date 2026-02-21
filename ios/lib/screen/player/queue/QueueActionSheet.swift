//
//  QueueActionSheet.swift
//  ios
//
//  Created by Anas Erkinjonov on 14/02/26.
//

internal import Kingfisher
internal import Shared
import SwiftUI

struct QueueActionSheet: View {
    let item: QueueItem
    let onDismissRequest: () -> Void
    let onPlay: () -> Void

    var body: some View {
        VStack(spacing: 0) {
            Spacer(minLength: 8)

            // Item Info Header
            HStack(alignment: .center, spacing: 8) {
                KFImage(URL(string: item.coverImagePath?.normalizeUrl() ?? ""))
                    .placeholder {
                        Image("MainLogo")
                            .resizable()
                            .aspectRatio(contentMode: .fit)
                    }
                    .resizable()
                    .aspectRatio(contentMode: .fill)
                    .frame(width: 64, height: 64)
                    .clipShape(RoundedRectangle(cornerRadius: 8))

                Text(item.title)
                    .font(Typography.titleMedium)
                    .foregroundColor(.primary)
                    .lineLimit(2)
                    .lineSpacing(1)

                Spacer()
            }
            .padding(.horizontal, 12)

            Spacer()
                .frame(height: 12)

            // Play Now Button
            SheetMenuButton(
                icon: "play.fill",
                title: Strings.shared.PLAY_NOW.string(),
                spacing: 28
            ) {
                onPlay()
            }
            .padding(.horizontal, 12)
        }
        .presentationDetents([.height(160)])
        .presentationDragIndicator(.visible)
        .presentationBackground(Colors.surfaceContainerLow)
    }
}

#Preview {
    PreviewRoot {
        Color.clear
            .sheet(isPresented: .constant(true)) {
                QueueActionSheet(
                    item: getSampleQueueItem(id: 1),
                    onDismissRequest: {},
                    onPlay: {}
                )
            }
    }
}
