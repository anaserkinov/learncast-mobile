//
//  MarqueeText.swift
//  ios
//
//  Created by Anas Erkinjonov on 03/02/26.
//

import SwiftUI

struct MarqueeView<Content>: View where Content: View {

    @ViewBuilder var content: () -> Content

    @State private var offset: CGFloat = 0
    @State private var contentWidth: CGFloat = 0
    @State private var containerWidth: CGFloat = 0

    private var shouldScroll: Bool {
        contentWidth > containerWidth
    }

    var body: some View {
        GeometryReader { geometry in
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 32) {
                    content()
                        .background(
                            GeometryReader { contentGeometry in
                                Color.clear
                                    .onAppear {
                                        contentWidth = contentGeometry.size.width
                                    }
                            }
                        )

                    // Only show second copy if content overflows
                    if shouldScroll {
                        content()
                    }
                }
                .frame(minWidth: geometry.size.width, alignment: shouldScroll ? .leading : .center)
                .offset(x: offset)
                .onAppear {
                    containerWidth = geometry.size.width - 64

                    // Only animate if content is wider than container
                    DispatchQueue.main.asyncAfter(deadline: .now() + 0.1) {
                        if shouldScroll {
                            withAnimation(.linear(duration: 35.0).repeatForever(autoreverses: false)) {
                                offset = -(contentWidth + 32)
                            }
                        }
                    }
                }
                .onChange(of: contentWidth) { oldValue, newValue in
                    // Update animation when content changes
                    if shouldScroll {
                        withAnimation(.linear(duration: 35.0).repeatForever(autoreverses: false)) {
                            offset = -(contentWidth + 32)
                        }
                    } else {
                        offset = 0
                    }
                }
            }
            .disabled(true)
            .mask(
                HStack(spacing: 0) {
                    LinearGradient(
                        colors: [.clear, .black],
                        startPoint: .leading,
                        endPoint: .trailing
                    )
                    .frame(width: 32)

                    Rectangle()
                        .fill(.black)

                    LinearGradient(
                        colors: [.black, .clear],
                        startPoint: .leading,
                        endPoint: .trailing
                    )
                    .frame(width: 32)
                }
            )
        }
        .frame(maxWidth: .infinity)
    }
}
