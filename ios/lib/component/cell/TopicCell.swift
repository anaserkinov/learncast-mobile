//
//  TopicCell.swift
//  ios
//
//  Created by Anas Erkinjonov on 12/02/26.
//

internal import Kingfisher
internal import Shared
import SwiftUI

struct TopicCell: View {
    let topic: Topic
    let onClick: () -> Void

    var body: some View {
        Button(action: onClick) {
            HStack(alignment: .center, spacing: 8) {
                KFImage(URL(string: topic.coverImagePath?.normalizeUrl() ?? ""))
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
                    Text(topic.authorName)
                        .font(Typography.bodyMedium)
                        .opacity(0.7)

                    Text(topic.title)
                        .font(Typography.titleMedium)
                        .lineLimit(2)

                    Text(
                        "\(topic.createdAt.monthYear()) · \(Strings.shared.LESSON.quantityString(NSNumber(value: topic.lessonCount)))"
                    )
                    .font(Typography.bodyMedium)
                    .opacity(0.7)
                }
                .frame(height: 80, alignment: .center)
            }
            .padding(4)
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.top, 8)
            .background(Color.clear)
            .contentShape(Rectangle())
        }
        .buttonStyle(PlainButtonStyle())
    }
}

#Preview {
    PreviewRoot {
        TopicCell(topic: getSampleTopic(), onClick: {})
    }
}
