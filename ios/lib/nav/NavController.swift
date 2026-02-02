//
//  AppEnvironment.swift
//  ios
//
//  Created by Anas Erkinjonov on 30/01/26.
//

import Foundation
import SwiftUI

@Observable
public class NavController {
    var backStack = [Screen]()

    func navigate(screen: Screen) {
        backStack.append(screen)
    }

    func popBack() {
        backStack.removeLast()
    }

    func removeAll() {
        backStack.removeAll()
    }
}

private struct NavControllerKey: EnvironmentKey {
    static var defaultValue: NavController = NavController()
}

extension EnvironmentValues {
    public var navController: NavController {
        get { self[NavControllerKey.self] }
        set { self[NavControllerKey.self] = newValue }
    }
}
