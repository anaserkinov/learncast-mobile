//
//  FilterChip.swift
//  ios
//
//  Created by Anas Erkinjonov on 01/02/26.
//

import SwiftUI
import Shared

struct FilterChip: View {
    let title: String
    let isSelected: Bool
    let onTap: () -> Void
    
    var body: some View {
        Button(action: onTap) {
            Text(title)
                .font(Typography.LabelLarge)
                .padding(.horizontal, 16)
                .padding(.vertical, 4)
                .foregroundColor(
                    isSelected ? Colors.OnSecondaryContainer : Colors.OnSurface
                )
        }
        .applyAdaptiveButtonStyle(isSelected: isSelected)
    }
}

private extension View {
    @ViewBuilder
    func applyAdaptiveButtonStyle(isSelected: Bool) -> some View {
        if isSelected {
            self.buttonStyle(.glassProminent)
        } else {
            self.buttonStyle(.glass)
        }
    }
}

#Preview {
    PreviewRoot {
        FilterChip (
            title: Strings.shared.LATEST,
            isSelected: true,
            onTap: {}
        )
        FilterChip (
            title: Strings.shared.LATEST,
            isSelected: false,
            onTap: {}
        )
    }
}
