//
//  AuthorCell.swift
//  ios
//
//  Created by Anas Erkinjonov on 22/01/26.
//

internal import Kingfisher
internal import Shared
import SwiftUI

struct LessonCell: View {
    let lesson: Lesson
    let onClick: () -> Void

    var body: some View {
        Button(action: onClick) {
            HStack(alignment: .center, spacing: 8) {
                KFImage(URL(string: lesson.coverImagePath?.normalizeUrl() ?? ""))
                    .placeholder {
                        Image("MainLogo")
                            .resizable()
                            .aspectRatio(contentMode: .fit)
                    }
                    .resizable()
                    .clipShape(RoundedRectangle(cornerRadius: 8))
                    .frame(width: 80, height: 80)
                    .padding(.trailing, 8)
                VStack(alignment: .leading) {
                    Text(lesson.authorName)
                        .font(Typography.bodyMedium)
                        .opacity(0.7)

                    Text(
                        lesson.topicTitle != nil
                            ? "\(lesson.title) - \(lesson.topicTitle!)"
                            : lesson.title
                    )
                    .font(Typography.titleMedium)
                    .lineLimit(2)

                    Text(
                        "\(lesson.createdAt.dayMonth()) · \(lesson.audioDuration.formatTimeFromDuration())"
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
        LessonCell(lesson: getSampleLesson(id: 1), onClick: {})
    }
}
