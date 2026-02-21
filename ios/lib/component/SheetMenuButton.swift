//
//  SheetMenuButton.swift
//  ios
//
//  Created by Anas Erkinjonov on 03/02/26.
//

import SwiftUI

struct SheetMenuButton: View {
    let icon: String?
    let title: String
    var clip: Bool = true
    var horizontalAlignment: HorizontalAlignment = .leading
    var padding: EdgeInsets = EdgeInsets(top: 12, leading: 12, bottom: 12, trailing: 12)
    var spacing: CGFloat = 8
    let onClick: () -> Void

    var body: some View {
        PrimaryButton(
            title: title,
            icon: icon,
            clip: clip,
            padding: padding,
            spacing: spacing,
            backgroundColor: Colors.surfaceContainerLowest,
            horizontalAlignment: horizontalAlignment,
            height: 56,
            onClick: onClick
        )
    }
}

struct SheetMenuWhiteButton: View {
    let icon: String?
    let title: String
    var clip: Bool = true
    var horizontalAlignment: HorizontalAlignment = .leading
    var padding: EdgeInsets = EdgeInsets(top: 12, leading: 12, bottom: 12, trailing: 12)
    var spacing: CGFloat = 8
    let onClick: () -> Void

    var body: some View {
        PrimaryButton(
            title: title,
            icon: icon,
            clip: clip,
            padding: padding,
            spacing: spacing,
            titleColor: Colors.onSecondaryContainer,
            backgroundColor: Colors.secondaryContainer,
            horizontalAlignment: horizontalAlignment,
            height: 56,
            onClick: onClick
        )
    }
}
