//
//  Environment.swift
//  ios
//
//  Created by Anas Erkinjonov on 13/02/26.
//

import SwiftUI

extension EnvironmentValues {
    @Entry public var env = AppEnvironment()
    @Entry public var navController = NavController()
    @Entry public var navigationAnimation: Namespace.ID? = nil
}
