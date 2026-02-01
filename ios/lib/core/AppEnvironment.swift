//
//  AppEnvironment.swift
//  ios
//
//  Created by Anas Erkinjonov on 30/01/26.
//

import Foundation
import SwiftUI

@Observable
public class AppEnvironment {
    var backgroundColors: [Color]
    var playerBackgroundColors: [Color]
    
    public init(
        backgroundColors: [Color] = [
            Color(red: 0.094, green: 0.122, blue: 0.2),
            Color(red: 0.055, green: 0.071, blue: 0.122)
        ],
        playerBackgroundColors: [Color] = [
            Color(red: 0.224, green: 0.282, blue: 0.42),
            Color(red: 0.075, green: 0.094, blue: 0.157)
        ]
    ) {
        self.backgroundColors = backgroundColors
        self.playerBackgroundColors = playerBackgroundColors
    }
    
    func backgroundGradient() -> LinearGradient {
        LinearGradient(
            stops: [
                .init(color: backgroundColors[0], location: 0),
                .init(color: backgroundColors[1], location: 0.25)
            ],
            startPoint: .top,
            endPoint: .bottom
        )
    }
}


private struct AppEnvironmentKey: EnvironmentKey {
    static var defaultValue: AppEnvironment = AppEnvironment()
}

public extension EnvironmentValues {
    var env: AppEnvironment {
        get { self[AppEnvironmentKey.self] }
        set { self[AppEnvironmentKey.self] = newValue }
    }
}
