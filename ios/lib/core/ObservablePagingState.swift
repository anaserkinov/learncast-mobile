//
//  ObservableViewModel.swift
//  iosApp
//
//  Created by Anas Erkinjonov on 26/02/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import Foundation
internal import Shared
import SwiftUI

@Observable
class ObservablePagingState<T: AnyObject & Identifiable & Equatable> {

    private var pagingState: ListPagingState<T>? = nil

    var range: Range<Int> = 0..<0
    var loadState: Paging_commonCombinedLoadStates = Paging_commonCombinedLoadStates(
        refresh: InitialLoadStates.refresh,
        prepend: InitialLoadStates.prepend,
        append: InitialLoadStates.append,
        source: InitialLoadStates,
        mediator: nil
    )

    func collect(state: ListPagingState<T>) async {
        self.pagingState = state
        async let loadState: Void = collectLoadState(pagingState: state)
        async let list: Void = collectList(pagingState: state)

        _ = await (loadState, list)
    }

    subscript(index: Int) -> T? {
        return pagingState!.peek(index: Int32(index))
    }

    func notify(index: Int) {
        if let state = pagingState {
            state.get(index: Int32(index))
        }
    }

    func refresh() async {
        if let state = pagingState {
            async let listen: Void = listenRefreshState(state: state)
            state.refresh()
            await listen
        }
    }

    private func listenRefreshState(state: ListPagingState<T>) async {
        var isRefreshing = false
        for await state in state.loadState {
            if isRefreshing {
                if !(state.refresh is Paging_commonLoadState.Loading) {
                    break
                }
            } else if state.refresh is Paging_commonLoadState.Loading {
                isRefreshing = true
            }
        }
    }

    private func collectLoadState(pagingState: ListPagingState<T>) async {
        for await state in pagingState.loadState {
            self.loadState = state
        }
    }

    private func collectList(pagingState: ListPagingState<T>) async {
        for await list in pagingState.itemSnapshotList {
            range = 0..<list.count
        }
    }
}
