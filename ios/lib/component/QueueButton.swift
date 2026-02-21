//
//  QueueButton.swift
//  ios
//
//  Created by Anas Erkinjonov on 05/02/26.
//

import SwiftUI

struct QueueButton: View {
    let count: Int32
    let onClick: () -> Void

    var body: some View {
        Button(action: onClick) {
            HStack(alignment: .center, spacing: 4) {
                Image(systemName: "line.3.horizontal")  // or your custom icon
                    .font(.system(size: 20))
                    .scaleEffect(x: 0.25, y: 1.0)
                    .fontWeight(.semibold)

                Text("\(count)")
                    .font(.system(size: 12, weight: .bold))
                    .foregroundColor(.black)
                    .lineLimit(1)
                    .padding(.horizontal, 2)
                    .frame(minWidth: 16, minHeight: 16)
                    .background(
                        RoundedRectangle(cornerRadius: 2)
                            .fill(Color.white)
                    )
                    .offset(x: -8)
            }
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
    }
}

#Preview {
    QueueButton(count: 100) {
    }
}
