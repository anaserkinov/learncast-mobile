//
//  PrimaryButton.swift
//  ios
//
//  Created by Anas Erkinjonov on 29/01/26.
//

internal import Shared
import SwiftUI

extension PrimaryButton {
    init(
        titleKey: String,
        icon: String? = nil,
        clip: Bool = true,
        padding: EdgeInsets = EdgeInsets(top: 12, leading: 12, bottom: 12, trailing: 12),
        spacing: CGFloat = 8,
        titleColor: Color = Colors.onTertiaryContainer,
        backgroundColor: Color = Colors.tertiaryContainer,
        horizontalAlignment: HorizontalAlignment = .leading,
        height: CGFloat = 48,
        onClick: @escaping () -> Void
    ) {
        self.init(
            title: titleKey.string(),
            icon: icon,
            clip: clip,
            padding: padding,
            spacing: spacing,
            titleColor: titleColor,
            backgroundColor: backgroundColor,
            horizontalAlignment: horizontalAlignment,
            height: height,
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
    let titleColor: Color
    let backgroundColor: Color
    let horizontalAlignment: HorizontalAlignment
    let height: CGFloat
    let onClick: () -> Void

    init(
        title: String,
        icon: String? = nil,
        clip: Bool = true,
        padding: EdgeInsets = EdgeInsets(top: 12, leading: 12, bottom: 12, trailing: 12),
        spacing: CGFloat = 8,
        titleColor: Color = Colors.onTertiaryContainer,
        backgroundColor: Color = Colors.tertiaryContainer,
        horizontalAlignment: HorizontalAlignment = .leading,
        height: CGFloat = 48,
        onClick: @escaping () -> Void
    ) {
        self.title = title
        self.icon = icon
        self.clip = clip
        self.padding = padding
        self.spacing = spacing
        self.titleColor = titleColor
        self.backgroundColor = backgroundColor
        self.horizontalAlignment = horizontalAlignment
        self.height = height
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
                    .font(Typography.titleMedium)
                    .foregroundStyle(titleColor)
            }
            .padding(padding)
            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: alignment)
            .background(backgroundColor)
        }
        .tint(Colors.onTertiaryContainer)
        .buttonStyle(RippleButtonStyle(clip: clip))
        .frame(height: height)
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

struct RippleButtonStyle: ButtonStyle {
    let clip: Bool

    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .clipShape(RoundedRectangle(cornerRadius: configuration.isPressed || clip ? 8 : 0))
            .scaleEffect(configuration.isPressed ? 0.96 : 1.0)
            .animation(.easeInOut(duration: 0.1), value: configuration.isPressed)
    }
}

#Preview {
    PreviewRoot {
        PrimaryButton(titleKey: Strings.shared.SEARCH, icon: "magnifyingglass") {

        }
    }
}
