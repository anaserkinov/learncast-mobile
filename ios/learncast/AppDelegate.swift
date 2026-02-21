//
// Created by Anas Erkinjonov on 12/11/25.
//

import Foundation
import Shared
import UIKit
import lib

class AppDelegate: NSObject, UIApplicationDelegate {

    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        AppConfig.companion.update(
            appName: "LearnCast",
            mainLogo: "MainLogo",
            transparentLogo: "TransparentLogo",
            apiBaseUrl: "https://api.anasmusa.me/learncast/",
            publicBaseUrl: "https://learncast.anasmusa.me",
            telegramBotId: 8_538_344_134,
            googleClientId:
                "22454749576-42ii04497d5aceqndkbvpnvn29nvub02.apps.googleusercontent.com"
        )

        AppInitializer.initialize()
        return true
    }
}
