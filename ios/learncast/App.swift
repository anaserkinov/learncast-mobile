//
//  learncastApp.swift
//  learncast
//
//  Created by Anas Erkinjonov on 22/01/26.
//

import SwiftUI
import Shared

@main
struct LearncastApp: App {
    var body: some Scene {
        WindowGroup {
            ContentView()
                .onOpenURL { url in
                    _ = Initializer.shared.googleHandleUrl(url: url)
                }
        }
    }
}
