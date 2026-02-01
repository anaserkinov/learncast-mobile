//
//  PrimaryButton.swift
//  ios
//
//  Created by Anas Erkinjonov on 29/01/26.
//

import SwiftUI
import Shared

extension PrimaryButton {
    init(
        titleKey: String,
        icon: String? = nil,
        clip: Bool = true,
        padding: EdgeInsets = EdgeInsets(top: 12, leading: 12, bottom: 12, trailing: 12),
        spacing: CGFloat = 8,
        backgroundColor: Color = Colors.TertiaryContainer,
        horizontalAlignment: HorizontalAlignment = .leading,
        onClick: @escaping () -> Void
    ) {
        self.init(
            title: titleKey.string(),
            icon: icon,
            clip: clip,
            padding: padding,
            spacing: spacing,
            backgroundColor: backgroundColor,
            horizontalAlignment: horizontalAlignment,
            onClick: onClick
        )
    }
}

struct PrimaryButton: View {
    let title: String
    let icon: String?
    let clip: Bool
    let padding: EdgeInsets
    let spacing: CGFloat
    let backgroundColor: Color
    let horizontalAlignment: HorizontalAlignment
    let onClick: () -> Void
    
    init(
        title: String,
        icon: String? = nil,
        clip: Bool = true,
        padding: EdgeInsets = EdgeInsets(top: 12, leading: 12, bottom: 12, trailing: 12),
        spacing: CGFloat = 8,
        backgroundColor: Color = Color(Colors.TertiaryContainer),
        horizontalAlignment: HorizontalAlignment = .leading,
        onClick: @escaping () -> Void
    ) {
        self.title = title
        self.icon = icon
        self.clip = clip
        self.padding = padding
        self.spacing = spacing
        self.backgroundColor = backgroundColor
        self.horizontalAlignment = horizontalAlignment
        self.onClick = onClick
    }

    var body: some View {
        Button(action: onClick) {
            HStack(spacing: spacing) {
                if let icon {
                    Image(systemName: icon)
                        .font(.system(size: 20))
                }

                Text(title)
                    .lineLimit(1)
                    .font(Typography.TitleMedium)
            }
            .padding(padding)
            .frame(maxWidth: .infinity, alignment: alignment)
            .background(backgroundColor)
        }
        .tint(Colors.OnTertiaryContainer)
        .applyIf(clip) { view in
            view.clipShape(RoundedRectangle(cornerRadius: 8))
        }
        .buttonStyle(.plain)
    }

    private var alignment: Alignment {
        switch horizontalAlignment {
        case .leading:
            return .leading
        case .center:
            return .center
        case .trailing:
            return .trailing
        default:
            return .leading
        }
    }
}

#Preview {
    PreviewRoot{
        PrimaryButton(titleKey: Strings.shared.SEARCH, icon: "magnifyingglass") {
            
        }
    }
}
