//
//  SnackbarModifier.swift
//  ios
//
//  Created by Anas Erkinjonov on 31/01/26.
//

//
//  SnackbarModifier.swift
//  iosApp
//
//  Created by Anas Erkinjonov on 31/01/26.
//

import SwiftUI

struct SnackbarModifier: ViewModifier {
    @Binding var isPresented: Bool
    let message: String
    let duration: TimeInterval = 3.0

    func body(content: Content) -> some View {
        ZStack {
            content

            if isPresented {
                VStack {
                    Spacer()

                    Text(message)
                        .padding()
                        .background(
                            RoundedRectangle(cornerRadius: 8)
                                .fill(Color(.systemGray6))
                        )
                        .padding(.horizontal)
                        .padding(.bottom, 50)
                        .transition(.move(edge: .bottom).combined(with: .opacity))
                }
                .onAppear {
                    DispatchQueue.main.asyncAfter(deadline: .now() + duration) {
                        withAnimation {
                            isPresented = false
                        }
                    }
                }
            }
        }
        .animation(.easeInOut, value: isPresented)
    }
}

extension View {
    func snackbar(isPresented: Binding<Bool>, message: String) -> some View {
        modifier(SnackbarModifier(isPresented: isPresented, message: message))
    }
}
