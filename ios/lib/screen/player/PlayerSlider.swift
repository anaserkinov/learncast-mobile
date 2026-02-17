//
//  PlayerSlider.swift
//  ios
//
//  Created by Anas Erkinjonov on 03/02/26.
//

import SwiftUI

struct PlayerSlider: View {
    var value: Int64
    let total: Int64
    let onValueChangeFinished: (Int64) -> Void

    @State private var isDragging = false
    @State private var dragValue: Int64 = 0
    @State private var currentValue: Int64 = 0

    // Computed property for the current display value
    private var displayValue: Int64 {
        isDragging ? dragValue : currentValue
    }

    private var cornerRadius: CGFloat {
        isDragging ? 8 : 4
    }

    var body: some View {
        GeometryReader { geometry in
            ZStack(alignment: .leading) {
                // Track background
                RoundedRectangle(cornerRadius: cornerRadius)
                    .fill(Color.white.opacity(0.3))
                    .frame(height: isDragging ? 16 : 8)
                    .animation(.spring(response: 0.3, dampingFraction: 0.7), value: isDragging)

                // Progress fill
                UnevenRoundedRectangle(cornerRadii: RectangleCornerRadii(topLeading: cornerRadius, bottomLeading: cornerRadius))
                    .fill(Color.white)
                    .frame(
                        width: progressWidth(in: geometry.size.width),
                        height: isDragging ? 16 : 8
                    )
                    .animation(.spring(response: 0.3, dampingFraction: 0.7), value: isDragging)

                // Thumb
                Circle()
                    .fill(Color.white)
                    .frame(width: 16, height: 16)
                    .shadow(color: .black.opacity(0.2), radius: 2, x: 0, y: 1)
                    .offset(x: thumbOffset(in: geometry.size.width))
                    .animation(.spring(response: 0.3, dampingFraction: 0.7), value: isDragging)
            }
            .contentShape(Rectangle())
            .gesture(
                DragGesture(minimumDistance: 0)
                    .onChanged { gesture in
                        if !isDragging {
                            isDragging = true
                            HapticFeedback.light()
                        }

                        let width = geometry.size.width
                        let ratio = min(max(gesture.location.x / width, 0), 1)
                        dragValue = Int64(ratio * CGFloat(total))
                    }
                    .onEnded { _ in
                        currentValue = dragValue  // Update our state first
                        isDragging = false
                        onValueChangeFinished(dragValue)
                        HapticFeedback.medium()
                    }
            )
            .frame(maxHeight: .infinity, alignment: .center)
        }
        .frame(height: 24)  // Increase touch target
        .onAppear {
            currentValue = value
        }
        .onChange(of: value) { _, newValue in
            if !isDragging && (currentValue != dragValue || abs(newValue - currentValue) < 3000) {
                currentValue = newValue
            }
        }

        HStack {
            Text(displayValue.formatTimeFromMillis())
                .font(Typography.bodyMedium)

            Spacer()

            Text(total.formatTimeFromMillis())
                .font(Typography.bodyMedium)
        }
    }

    private func progressWidth(in totalWidth: CGFloat) -> CGFloat {
        guard totalWidth > 0, total > 0 else { return 0 }
        return totalWidth * CGFloat(displayValue) / CGFloat(total)
    }

    private func thumbOffset(in totalWidth: CGFloat) -> CGFloat {
        guard totalWidth > 0 else { return 0 }
        let thumbWidth: CGFloat = isDragging ? 16 : 12
        return progressWidth(in: totalWidth) - (thumbWidth / 2)
    }
}

// MARK: - Preview
#Preview {
    VStack(spacing: 40) {
        PlayerSlider(
            value: 3,
            total: 10,
            onValueChangeFinished: { value in }
        )
        .padding(.horizontal, 20)

        PlayerSlider(
            value: 7,
            total: 10,
            onValueChangeFinished: { value in }
        )
        .padding(.horizontal, 20)
    }
    .frame(maxWidth: .infinity, maxHeight: .infinity)
    .background(Color.black)
}
