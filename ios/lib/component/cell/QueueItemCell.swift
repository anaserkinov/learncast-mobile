//
//  QueueItemCell.swift
//  ios
//
//  Created by Anas Erkinjonov on 12/02/26.
//

internal import Kingfisher
internal import Shared
import SwiftUI

struct QueueItemCell: View {
    let queueItem: QueueItem
    var paddingTop: CGFloat = 8
    let onClick: () -> Void

    var body: some View {
        Button(action: onClick) {
            HStack(alignment: .center, spacing: 8) {
                KFImage(URL(string: queueItem.coverImagePath?.normalizeUrl() ?? ""))
                    .placeholder {
                        Image("MainLogo")
                            .resizable()
                            .aspectRatio(contentMode: .fit)
                    }
                    .resizable()
                    .clipShape(RoundedRectangle(cornerRadius: 8))
                    .frame(width: 80, height: 80)
                    .padding(.trailing, 8)
                VStack(alignment: .leading, spacing: 4) {
                    Text(queueItem.subTitle)
                        .font(Typography.bodyMedium)
                        .opacity(0.7)

                    Text(queueItem.title)
                        .font(Typography.titleMedium)
                        .lineLimit(2)
                }
                .frame(height: 80, alignment: .center)
            }
            .padding(4)
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.top, paddingTop)
            .background(Color.clear)
            .contentShape(Rectangle())
        }
        .buttonStyle(PlainButtonStyle())
    }
}

#Preview {
    PreviewRoot {
        QueueItemCell(queueItem: getSampleQueueItem(id: 1), onClick: {})
    }
}
