//
//  AppInitializer.swift
//  ios
//
//  Created by Anas Erkinjonov on 07/02/26.
//

import FirebaseCore
import GoogleSignIn
internal import Shared

public class AppInitializer {
    public static func initialize() {
        FirebaseApp.configure()

        googleAuthManagerFactory = {
            IosGoogleAuthManager()
        }
        AVPlayerDelegateCompanion.shared.factory = {
            AVPlayerDelegateImpl()
        }
        downloadManagerFactory = {
            IosDownloadManager.shared
        }

        #if DEBUG
            Initializer.shared.doInitApp(debug: true)
        #else
            Initializer.shared.doInitApp(debug: false)
        #endif
    }

    public static func handle(url: URL) -> Bool {
        let handled = GIDSignIn.sharedInstance.handle(url)
        return handled
    }
}
