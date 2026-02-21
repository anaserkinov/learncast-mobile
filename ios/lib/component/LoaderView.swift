//
//  LoaderView.swift
//  ios
//
//  Created by Anas Erkinjonov on 31/01/26.
//

import SwiftUI

struct LoaderView: View {

    private let binding = Binding {
        true
    } set: { _ in
    }

    var body: some View {
        Color.clear
            .fullScreenCover(isPresented: binding) {
                ZStack {
                    Color.black.opacity(0.3)
                        .ignoresSafeArea()

                    ProgressView()
                        .progressViewStyle(CircularProgressViewStyle(tint: .white))
                        .scaleEffect(1.5)
                        .padding()
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
            }
            .transaction { $0.disablesAnimations = true }
    }
}
