//
//  ListPagingItems.swift
//  ios
//
//  Created by Anas Erkinjonov on 30/01/26.
//

import SwiftUI
import Shared

private let IncompleteLoadState = Paging_commonLoadState.NotLoading(endOfPaginationReached: false)
private let InitialLoadStates = Paging_commonLoadStates(
    refresh: Paging_commonLoadState.Loading(),
    prepend: IncompleteLoadState,
    append: IncompleteLoadState
)


@Observable
class ListPagingState<T: AnyObject>: RandomAccessCollection {
    
    //    private let identity = UUID()
    //    static func == (lhs: ListPagingState<T>, rhs: ListPagingState<T>) -> Bool {
    //        return lhs.identity == rhs.identity
    //    }
    
    private var pagingDataPresenter: SwiftPagingDataPresenter<T>? = nil
    var itemSnapshotList = [T]()
    
    var loadState: Paging_commonCombinedLoadStates
    
    private var loadStateTask: Task<Void, Never>?
    private var pagingDataTask: Task<Void, Never>?
    
    var itemCount: Int {
        itemSnapshotList.count
    }
    
    @ObservationIgnored
    var startIndex: Int = 0
    
    var endIndex: Int {
        itemSnapshotList.count
    }
    
    init() {
        loadState = Paging_commonCombinedLoadStates(
            refresh: InitialLoadStates.refresh,
            prepend: InitialLoadStates.prepend,
            append: InitialLoadStates.append,
            source: InitialLoadStates,
            mediator: nil
        )
    }
    
    deinit {
        stopCollecting()
    }
    
    func update(flow: any SkieSwiftFlowProtocol<Paging_commonPagingData<T>>) {
        stopCollecting()
        let cached: Paging_commonPagingData<T>? = flow is SkieSwiftSharedFlow<Paging_commonPagingData<T>> ?
        (flow as! SkieSwiftSharedFlow<Paging_commonPagingData<T>>).replayCache.first : nil
        
        pagingDataPresenter = SwiftPagingDataPresenter<T>(cachedPagingData: cached)
        let snapshot = (pagingDataPresenter!.snapshot() as? [T]) ?? []
        itemSnapshotList = snapshot
        
        loadState = pagingDataPresenter!.loadStateFlow.value ?? Paging_commonCombinedLoadStates(
            refresh: InitialLoadStates.refresh,
            prepend: InitialLoadStates.prepend,
            append: InitialLoadStates.append,
            source: InitialLoadStates,
            mediator: nil
        )
        
        pagingDataPresenter!.onEvent = { [weak self] _ in
            self?.itemSnapshotList = (self?.pagingDataPresenter?.snapshot() as? [T]) ?? []
        }
        
        startCollecting(flow: flow)
    }
    
    private func startCollecting(flow: any SkieSwiftFlowProtocol<Paging_commonPagingData<T>>) {
        loadStateTask = Task { [weak self] in
            await self?.collectLoadState()
        }
        
        pagingDataTask = Task { [weak self] in
            await self?.collectPagingData(flow: flow)
        }
    }
    
    private func stopCollecting() {
        loadStateTask?.cancel()
        pagingDataTask?.cancel()
        loadStateTask = nil
        pagingDataTask = nil
    }
    
    subscript(index: Int) -> T? {
        pagingDataPresenter!.get(index: Int32(index))
        return itemSnapshotList[index]
    }
    
    func peek(index: Int) -> T? {
        return itemSnapshotList[index]
    }
    
    func retry() {
        pagingDataPresenter!.retry()
    }
    
    func refresh() {
        pagingDataPresenter!.refresh()
    }
    
    private func collectLoadState() async {
        for await loadState in pagingDataPresenter!.loadStateFlow {
            if loadState != nil {
                self.loadState = loadState!
            }
        }
    }
    
    private func collectPagingData(flow: any SkieSwiftFlowProtocol<Paging_commonPagingData<T>>) async {
        let presenter = skie(pagingDataPresenter!)
        do {
            for try await pagingData in flow {
                do {
                    try await presenter.collectFrom(pagingData: pagingData as! Paging_commonPagingData<T>)
                } catch {
                    // Swallow presenter collection errors to keep stream alive, but log for diagnostics.
                    print("collectFrom error: \(error)")
                }
            }
        } catch {
            // Handle errors thrown by the async sequence iteration.
            print("collect paging sequence error: \(error)")
        }
    }
    
}
