//
//  EntryProvider.swift
//  ios
//
//  Created by Anas Erkinjonov on 30/01/26.
//

import SwiftUI

@ViewBuilder
func getView(screen: Screen?) -> some View {
    switch screen {
    case .Login: LoginScreen()
    default: EmptyView()
    }
}
