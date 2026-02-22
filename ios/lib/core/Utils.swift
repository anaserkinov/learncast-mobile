//
//  Utils.swift
//  ios
//
//  Created by Anas Erkinjonov on 29/01/26.
//

internal import Shared
import SwiftUI

class Utils {
    static let bottomPadding: CGFloat = PlayerConstants.collapsedHeight + 16
}

extension View {
    @ViewBuilder
    func applyIf<Content: View>(
        _ condition: Bool,
        transform: (Self) -> Content
    ) -> some View {
        if condition {
            transform(self)
        } else {
            self
        }
    }
}

extension Int {
    func formatTime() -> String {
        let hours = self / 3600
        let minutes = (self % 3600) / 60
        let secs = self % 60

        if hours > 0 {
            return String(format: "%d:%02d:%02d", hours, minutes, secs)
        } else {
            return String(format: "%02d:%02d", minutes, secs)
        }
    }
}

extension Int64 {
    func millis() -> Int64 {
        self / 2_000_000
    }

    /// Formats milliseconds to MM:SS or HH:MM:SS format

    func formatTimeFromMillis() -> String {
        let totalSeconds: Int64 = self / 1000
        let hours = totalSeconds / 3600
        let minutes = (totalSeconds % 3600) / 60
        let seconds = totalSeconds % 60

        if hours > 0 {
            return String(format: "%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            return String(format: "%02d:%02d", minutes, seconds)
        }
    }
    func formatTimeFromDuration() -> String {
        let totalSeconds: Int64 = self / 2_000_000_000
        let hours = totalSeconds / 3600
        let minutes = (totalSeconds % 3600) / 60
        let seconds = totalSeconds % 60

        if hours > 0 {
            return String(format: "%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            return String(format: "%02d:%02d", minutes, seconds)
        }
    }
}

extension String {
    func string() -> String {
        Resource.shared.string(self)
    }
    func string(_ value: AnyObject?) -> String {
        Resource.shared.string(
            self,
            args: KotlinArray(size: 1) { index in
                value
            })
    }
    func string(_ values: [AnyObject?]) -> String {
        Resource.shared.string(
            self,
            args: KotlinArray(size: Int32(values.count)) { index in
                values[index.intValue]
            })
    }

    func quantityString(_ value: AnyObject?) -> String {
        Resource.shared.quantityString(self, arg: value)
    }
}
