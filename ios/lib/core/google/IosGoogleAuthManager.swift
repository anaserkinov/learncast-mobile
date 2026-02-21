//
//  IosGoogleAuthManager.swift
//  ios
//
//  Created by Anas Erkinjonov on 15/02/26.
//

import GoogleSignIn
internal import Shared

class IosGoogleAuthManager: GoogleAuthManager {

    // swift-format-ignore
    func __signIn() async throws -> String? {
        guard let root = await UIApplication.shared.keyWindow?.rootViewController else {
            return nil
        }

        do {
            let result = try await GIDSignIn.sharedInstance.signIn(withPresenting: root)
            return result.user.idToken?.tokenString
        } catch {
            return nil
        }
    }

}
