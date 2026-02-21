//
//  AuthorCell.swift
//  ios
//
//  Created by Anas Erkinjonov on 22/01/26.
//

internal import Kingfisher
internal import Shared
import SwiftUI

struct AuthorCell: View {
    let author: Author
    let onClick: () -> Void

    var body: some View {
        Button(action: onClick) {
            HStack(alignment: .center, spacing: 8) {
                KFImage(URL(string: author.avatarPath?.normalizeUrl() ?? ""))
                    .placeholder {
                        Image("MainLogo")
                            .resizable()
                            .aspectRatio(contentMode: .fit)
                    }
                    .resizable()
                    .clipShape(Circle())
                    .frame(width: 64, height: 64)
                    .padding(.trailing, 8)
                VStack(alignment: .leading, spacing: 4) {
                    Text(author.name)
                        .font(Typography.titleMedium)
                        .lineLimit(2)
                        .truncationMode(.tail)
                    Text(
                        Resource.shared.quantityString(
                            Strings.shared.LESSON, arg: author.lessonCount)
                    )
                    .font(Typography.bodyMedium)
                    .opacity(0.7)
                }
                .frame(height: 64, alignment: .center)
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
        AuthorCell(author: getSampleAuthor(), onClick: {})
    }
}
