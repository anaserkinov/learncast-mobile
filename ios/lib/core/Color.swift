//
//  Colors.swift
//  ios
//
//  Created by Anas Erkinjonov on 11/02/26.
//

import SwiftUI
import UIKit

func lerpColor(_ color1: Color, _ color2: Color, _ t: CGFloat) -> Color {
    let uiColor1 = UIColor(color1)
    let uiColor2 = UIColor(color2)

    var r1: CGFloat = 0
    var g1: CGFloat = 0
    var b1: CGFloat = 0
    var a1: CGFloat = 0
    var r2: CGFloat = 0
    var g2: CGFloat = 0
    var b2: CGFloat = 0
    var a2: CGFloat = 0

    uiColor1.getRed(&r1, green: &g1, blue: &b1, alpha: &a1)
    uiColor2.getRed(&r2, green: &g2, blue: &b2, alpha: &a2)

    return Color(
        red: r1 + (r2 - r1) * t,
        green: g1 + (g2 - g1) * t,
        blue: b1 + (b2 - b1) * t,
        opacity: a1 + (a2 - a1) * t
    )
}

extension Int {

    /// Darken color by amount - EXACT match to Android implementation
    /// - Parameter amount: Amount to darken (0.0 to 1.0)
    /// - Returns: SwiftUI Color
    func darken(amount: Float) -> Color {
        // Convert RGB Int to HSV (matching android.graphics.Color.colorToHSV)
        var hsv = colorToHSV(self)

        // Increase saturation
        hsv[1] = Swift.min(1.0, hsv[1] + amount)

        // Optional: tiny brightness reduction (NOT black)
        hsv[2] = Swift.max(0.0, hsv[2] * (1.0 - amount * 0.6))

        // Convert back to RGB Int (matching android.graphics.Color.HSVToColor)
        let rgbInt = hSVToColor(hsv)

        // Convert to SwiftUI Color (matching Jetpack Compose Color())
        return Color(rgbInt)
    }

    /// Lighten color by amount - EXACT match to Android implementation
    /// - Parameter amount: Amount to lighten (0.0 to 1.0)
    /// - Returns: SwiftUI Color
    func lighten(amount: Float) -> Color {
        // Convert RGB Int to HSV (matching android.graphics.Color.colorToHSV)
        var hsv = colorToHSV(self)

        hsv[2] = Swift.min(1.0, hsv[2] + hsv[2] * amount)

        // Convert back to RGB Int (matching android.graphics.Color.HSVToColor)
        let rgbInt = hSVToColor(hsv)

        // Convert to SwiftUI Color (matching Jetpack Compose Color())
        return Color(rgbInt)
    }

    // MARK: - HSV Conversion (matching Android's implementation)

    /// Converts RGB Int to HSV array - matches android.graphics.Color.colorToHSV
    private func colorToHSV(_ color: Int) -> [Float] {
        let r = Float((color >> 16) & 0xFF) / 255.0
        let g = Float((color >> 8) & 0xFF) / 255.0
        let b = Float(color & 0xFF) / 255.0

        let max = Swift.max(r, g, b)
        let min = Swift.min(r, g, b)
        let delta = max - min

        var h: Float = 0
        let s: Float = max == 0 ? 0 : delta / max
        let v = max

        if delta != 0 {
            if max == r {
                h = ((g - b) / delta) + (g < b ? 6 : 0)
            } else if max == g {
                h = ((b - r) / delta) + 2
            } else {
                h = ((r - g) / delta) + 4
            }
            h /= 6.0
        }

        return [h, s, v]
    }

    /// Converts HSV array to RGB Int - matches android.graphics.Color.HSVToColor
    private func hSVToColor(_ hsv: [Float]) -> Int {
        let h = hsv[0] * 6.0
        let s = hsv[1]
        let v = hsv[2]

        let i = Int(floor(h))
        let f = h - Float(i)
        let p = v * (1.0 - s)
        let q = v * (1.0 - f * s)
        let t = v * (1.0 - (1.0 - f) * s)

        var r: Float = 0
        var g: Float = 0
        var b: Float = 0

        switch i % 6 {
        case 0: (r, g, b) = (v, t, p)
        case 1: (r, g, b) = (q, v, p)
        case 2: (r, g, b) = (p, v, t)
        case 3: (r, g, b) = (p, q, v)
        case 4: (r, g, b) = (t, p, v)
        case 5: (r, g, b) = (v, p, q)
        default: break
        }

        let red = Int(round(r * 255))
        let green = Int(round(g * 255))
        let blue = Int(round(b * 255))

        return (red << 16) | (green << 8) | blue
    }
}

// MARK: - SwiftUI Color Initializer from RGB Int

extension Color {
    /// Initialize SwiftUI Color from RGB Int (matching Jetpack Compose Color constructor)
    init(_ rgbInt: Int) {
        let r = Double((rgbInt >> 16) & 0xFF) / 255.0
        let g = Double((rgbInt >> 8) & 0xFF) / 255.0
        let b = Double(rgbInt & 0xFF) / 255.0

        self.init(red: r, green: g, blue: b)
    }
}
