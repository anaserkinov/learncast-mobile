//
//  FilterChip.swift
//  ios
//
//  Created by Anas Erkinjonov on 01/02/26.
//

internal import Shared
import SwiftUI

struct FilterChip: View {
    let title: String
    let isSelected: Bool
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            Text(title)
                .font(Typography.labelLarge)
                .padding(.horizontal, 16)
                .padding(.vertical, 4)
                .foregroundColor(
                    isSelected ? Colors.onSecondaryContainer : Colors.onSurface
                )
        }
        .applyAdaptiveButtonStyle(isSelected: isSelected)
    }
}

extension View {
    @ViewBuilder
    fileprivate func applyAdaptiveButtonStyle(isSelected: Bool) -> some View {
        if isSelected {
            self.buttonStyle(.glassProminent)
        } else {
            self.buttonStyle(.glass)
        }
    }
}

#Preview {
    PreviewRoot {
        FilterChip(
            title: Strings.shared.LATEST,
            isSelected: true,
            onTap: {}
        )
        FilterChip(
            title: Strings.shared.LATEST,
            isSelected: false,
            onTap: {}
        )
    }
}
