//
//  LoaderView.swift
//  ios
//
//  Created by Anas Erkinjonov on 31/01/26.
//

import SwiftUI

struct LoaderView: View {
    var body: some View {
        ZStack {
            Color.black.opacity(0.3)
                .ignoresSafeArea()

            ProgressView()
                .progressViewStyle(CircularProgressViewStyle(tint: .white))
                .scaleEffect(1.5)
                .padding()
        }
    }
}
