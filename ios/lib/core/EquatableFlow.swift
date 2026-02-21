//
//  EquatableFlow.swift
//  ios
//
//  Created by Anas Erkinjonov on 12/02/26.
//

import Foundation
internal import Shared

struct EquatableFlow: Equatable {

    let flow: Kotlinx_coroutines_coreFlow
    private let identity = UUID()

    static func == (lhs: EquatableFlow, rhs: EquatableFlow) -> Bool {
        lhs.identity == rhs.identity
    }
}
