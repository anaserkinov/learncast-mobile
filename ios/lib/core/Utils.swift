//
//  Utils.swift
//  ios
//
//  Created by Anas Erkinjonov on 29/01/26.
//

import Shared
import SwiftUI

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

extension String {
    func string() -> String {
        Resource.shared.string(self)
    }
}

extension Kotlinx_coroutines_coreFlow {
    func castToPagingFlow<T>() -> SkieSwiftFlow<Paging_commonPagingData<T>> {
        SkieSwiftFlow(SkieKotlinFlow<Paging_commonPagingData<T>>(self))
    }
}
