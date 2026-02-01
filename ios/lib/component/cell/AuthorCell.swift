//
//  AuthorCell.swift
//  ios
//
//  Created by Anas Erkinjonov on 22/01/26.
//

import SwiftUI
import Shared
import Kingfisher

struct AuthorCell: View {
    let author: Author
    let onClick: () -> Void

    var body: some View {
        Button(action: onClick) {
            HStack(alignment: .center, spacing: 8) {
                if let avatarPath = author.avatarPath,
                   let url = URL(string: UtilsKt.normalizeUrl(avatarPath)) {
                    KFImage(url)
                        .resizable()
                        .onSuccess { result in
                            print("Image loaded from cache: \(result.cacheType)")
                        }
                        .clipShape(Circle())
                        .frame(width: 64, height: 64)
                        .padding(.trailing, 8)
                } else {
                    Image("MainLogo")
                        .resizable()
                        .aspectRatio(contentMode: .fill)
                        .frame(width: 64, height: 64)
                        .clipShape(Circle())
                        .padding(.trailing, 8)
                }
                VStack(alignment: .leading, spacing: 4) {
                    Text(author.name)
                        .font(Typography.TitleMedium)
                        .lineLimit(2)
                        .truncationMode(.tail)
                    Text(Resource.shared.quantityString(Strings.shared.LESSON, arg: author.lessonCount))
                        .font(Typography.BodyMedium)
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
