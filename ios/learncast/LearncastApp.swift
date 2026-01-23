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

    var body: some Scene {
        WindowGroup {
            AppView()
                .onOpenURL { url in
                    _ = Initializer.shared.googleHandleUrl(url: url)
                }
        }
    }
}
