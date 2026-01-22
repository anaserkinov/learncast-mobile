//
// Created by Anas Erkinjonov on 12/11/25.
//

import Foundation
import UIKit
import Shared

class AppDelegate: NSObject, UIApplicationDelegate {

    func application(_ application: UIApplication,
                     didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey : Any]? = nil) -> Bool {
        AppConfig.companion.update(
            appName: "LearnCast",
            mainLogo: "MainLogo",
            transparentLogo: "TransparentLogo",
            apiBaseUrl: "https://api.anasmusa.me/learncast/",
            publicBaseUrl: "https://learncast.anasmusa.me",
            telegramBotId: 8538344134,
            googleClientId: "22454749576-42ii04497d5aceqndkbvpnvn29nvub02.apps.googleusercontent.com"
        )
        
        Initializer.shared.doInitApp()
        return true
    }

    // You can add other AppDelegate methods here if needed (like push notifications)
}
