import SwiftUI
import Shared

//
//  ListPagingItems.swift
//  iosApp
//
//  Created by Anas Erkinjonov on 02/12/25.
//


@Observable
final class LazyPagingItems<T: AnyObject> {
    
    var items: [T] = []
    var loadState: Paging_commonCombinedLoadStates

    private let flow: SkieSwiftFlow<Paging_commonPagingData<T>>
    private let presenter: SwiftPagingDataPresenter<T>

    init(flow: SkieSwiftFlow<Paging_commonPagingData<T>>) {
        let IncompleteLoadState = Paging_commonLoadState.NotLoading(endOfPaginationReached: false)
        let InitialLoadStates = Paging_commonLoadStates(
            refresh: Paging_commonLoadState.Loading(),
            prepend: IncompleteLoadState,
            append: IncompleteLoadState
        )
        self.loadState = Paging_commonCombinedLoadStates(
            refresh: InitialLoadStates.refresh,
            prepend: InitialLoadStates.prepend,
            append: InitialLoadStates.append,
            source: InitialLoadStates,
            mediator: nil
        )
        
        self.flow = flow
        self.presenter = createSwiftPagingPresenter(cached: nil) as! SwiftPagingDataPresenter<T>
        self.presenter.onEvent = { pagingData in
            self.updateItemSnapshotList()
        };
    }
    
    private func updateItemSnapshotList() {
        items = presenter.snapshot() as! [T]
    }
    
    func itemCount() -> Int {
        return items.count
    }

    subscript(index: Int) -> T? {
        presenter.get(index: Int32(index))
        return items[index]
    }
    
    func peek(index: Int) -> T? {
        return items[index]
    }

    func retry() {
        presenter.retry()
    }

    func refresh() {
        presenter.refresh()
    }
    
    func collectLoadState() async {
        let stream = presenter.loadStateFlow.filter { loadState in
            loadState != nil
        }
        for await state in stream {
            loadState = state!
        }
    }
    
    func collectPagingData() async {
        for await pagingData in flow {
            try? await skie(presenter).collectFromPagingData(pagingData: pagingData)
        }
    }
}
