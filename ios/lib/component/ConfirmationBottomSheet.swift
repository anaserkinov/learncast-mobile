//
//  ConfirmationBottomSheet.swift
//  ios
//
//  Created by Anas Erkinjonov on 13/02/26.
//

internal import Shared
import SwiftUI

struct ConfirmationBottomSheet: View {
    @Environment(\.env) var env: AppEnvironment

    let title: String?
    let message: String?
    let positiveButtonTitle: String?
    let negativeButtonTitle: String?
    let onConfirm: () -> Void
    let onDismiss: () -> Void

    init(
        title: String? = nil,
        message: String? = nil,
        positiveButtonTitle: String? = nil,
        negativeButtonTitle: String? = nil,
        onConfirm: @escaping () -> Void,
        onDismiss: @escaping () -> Void
    ) {
        self.title = title
        self.message = message
        self.positiveButtonTitle = positiveButtonTitle
        self.negativeButtonTitle = negativeButtonTitle
        self.onConfirm = onConfirm
        self.onDismiss = onDismiss
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            Spacer()
                .frame(height: 12)

            // Title
            if let title = title {
                Text(title)
                    .font(Typography.headlineSmall)
                    .fontWeight(.semibold)
                    .foregroundColor(.primary)

                Spacer()
                    .frame(height: 12)
            }

            // Message
            if let message = message {
                Text(message)
                    .font(Typography.bodyMedium)
                    .foregroundColor(.primary)
            }

            Spacer()
                .frame(height: 24)

            // Positive Button
            SheetMenuWhiteButton(
                icon: nil,
                title: positiveButtonTitle ?? Strings.shared.YES_TEXT.string(),
                horizontalAlignment: .center,
                onClick: onConfirm
            )

            Spacer()
                .frame(height: 12)

            // Negative Button
            SheetMenuButton(
                icon: nil,
                title: negativeButtonTitle ?? Strings.shared.CANCEL.string(),
                horizontalAlignment: .center,
                onClick: onDismiss
            )

            Spacer()
                .frame(height: 8)
        }
        .padding(.horizontal, 20)
        .presentationDetents([.height(calculateHeight())])
        .presentationBackground(env.backgroundGradient())
        .presentationDragIndicator(.visible)
    }

    private func calculateHeight() -> CGFloat {
        var height: CGFloat = 0

        // Title height
        if title != nil {
            height += 32 + 12  // Approximate title height + spacing
        }

        // Message height
        if message != nil {
            height += 64  // Approximate message height
        }

        // Spacing + buttons + bottom padding
        height += 24 + 56 + 12 + 56 + 8 + 16  // spacing + button1 + gap + button2 + padding + safe area

        return height
    }
}

#Preview {
    Text("Preview")
        .sheet(isPresented: .constant(true)) {
            ConfirmationBottomSheet(
                title: "Sign Out?",
                message: "Are you sure you want to sign out?",
                positiveButtonTitle: "Sign Out",
                negativeButtonTitle: "Cancel",
                onConfirm: {},
                onDismiss: {}
            )
        }
}
