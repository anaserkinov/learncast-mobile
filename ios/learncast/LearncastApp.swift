//
//  learncastApp.swift
//  learncast
//
//  Created by Anas Erkinjonov on 22/01/26.
//

import SwiftUI
import lib
import Shared

@main
struct LearncastApp: App {
    
    @UIApplicationDelegateAdaptor(AppDelegate.self) var delegate
    
    @State
    private var env = AppEnvironment(
        backgroundColors: [
            Color(red: 0.094, green: 0.122, blue: 0.2),
            Color(red: 0.055, green: 0.071, blue: 0.122)
        ],
        playerBackgroundColors: [
            Color(red: 0.224, green: 0.282, blue: 0.42),
            Color(red: 0.075, green: 0.094, blue: 0.157)
        ]
    )

    var body: some Scene {
        WindowGroup {
            AppView()
                .environment(\.env, env)
                .onOpenURL { url in
                    _ = Initializer.shared.googleHandleUrl(url: url)
                }
        }
    }
}
