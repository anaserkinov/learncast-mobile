//
//  SwipeDragBox.swift
//  ios
//
//  Created by Anas Erkinjonov on 14/02/26.
//

import SwiftUI

struct SwipeDragBox<Content: View>: View {
    let id: Int64
    let swipeWidth: CGFloat
    @Binding var swipingId: Int64
    let onRemove: () -> Void
    @ViewBuilder let content: Content

    @State private var offset: CGFloat = 0
    @State private var initialOffset: CGFloat = -1

    var body: some View {
        ZStack(alignment: .leading) {
            // Background - Remove Button
            HStack {
                Button(action: onRemove) {
                    Image(systemName: "text.badge.minus")
                        .font(.system(size: 24))
                        .foregroundColor(.red)
                        .frame(width: 56, height: 56)
                }
                Spacer()
            }
            .padding(.trailing, 16)
            .opacity(offset > 10 ? 1 : 0)

            // Foreground - Content
            content
                .offset(x: offset)
                .gesture(
                    DragGesture()
                        .onChanged { value in
                            if initialOffset == -1 {
                                initialOffset = offset
                            }
                            // Only allow rightward swipe
                            let translation = initialOffset + value.translation.width
                            if translation > 0 {
                                offset = min(translation, swipeWidth)
                                if offset > 10 {
                                    swipingId = id
                                }
                            }
                        }
                        .onEnded { value in
                            initialOffset = -1
                            // Snap to open or closed position
                            withAnimation(.spring(response: 0.3, dampingFraction: 0.8)) {
                                if offset > swipeWidth / 2 {
                                    offset = swipeWidth
                                    swipingId = id
                                } else {
                                    offset = 0
                                }
                            }
                        }
                )
        }
        .onChange(of: swipingId) { oldValue, newValue in
            // Close this item if another item is being swiped
            if newValue != id && offset > 0 {
                withAnimation(.spring(response: 0.3, dampingFraction: 0.8)) {
                    offset = 0
                }
            }
        }
    }
}
