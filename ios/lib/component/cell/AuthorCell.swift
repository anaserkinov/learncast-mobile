//
//  AuthorCell.swift
//  ios
//
//  Created by Anas Erkinjonov on 22/01/26.
//

import SwiftUI
import Shared

struct AuthorCell: View {
    let author: Author
    let onClick: () -> Void

    var body: some View {
        Button(action: onClick) {
            HStack(alignment: .center, spacing: 8) {
                if let avatarPath = author.avatarPath,
                   let url = URL(string: UtilsKt.normalizeUrl(avatarPath)) {
                    AsyncImage(url: url) { phase in
                        if let image = phase.image {
                            image
                                .resizable()
                                .aspectRatio(contentMode: .fill)
                        } else {
                            Image(appConfig.mainLogoString)
                                .resizable()
                                .aspectRatio(contentMode: .fill)
                        }
                    }
                    .frame(width: 64, height: 64)
                    .clipShape(Circle())
                    .padding(.trailing, 8)
                } else {
                    Image(appConfig.mainLogoString)
                        .resizable()
                        .aspectRatio(contentMode: .fill)
                        .frame(width: 64, height: 64)
                        .clipShape(Circle())
                        .padding(.trailing, 8)
                }
                VStack(alignment: .leading, spacing: 4) {
                    Text(author.name)
                        .font(.title3)
                        .fontWeight(.semibold)
                        .lineLimit(2)
                        .truncationMode(.tail)
                    Text(Resource.shared.quantityString(Strings.shared.LESSON, arg: author.lessonCount))
                        .font(.body)
                        .foregroundColor(.secondary)
                        .opacity(0.7)
                }
                .frame(height: 64, alignment: .center)
            }
            .padding(4)
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.top, 8)
            .background(Color.clear) // Ensure the area isn't "empty"
            .contentShape(Rectangle()) // Make the entire area tappable and show highlight
        }
        .buttonStyle(PlainButtonStyle())
    }
}

#Preview {
    AuthorCell(author: getSampleAuthor(), onClick: {})
}
