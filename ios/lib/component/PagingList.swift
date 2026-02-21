//
//  PagingView.swift
//  ios
//
//  Created by Anas Erkinjonov on 31/01/26.
//

internal import Shared
import SwiftUI

struct PagingList<T: AnyObject & Hashable & Identifiable, Content: View, Header: View>: View {

    @State private var pagingState = ObservablePagingState<T>()
    @State private var position = ScrollPosition(idType: T.ID.self)

    private let flow: SkieSwiftFlow<ListPagingState<T>>
    private let cell: (T) -> Content
    private let header: (() -> Header)?

    init(
        flow: SkieSwiftFlow<ListPagingState<T>>,
        header: (() -> Header)? = nil,
        @ViewBuilder cell: @escaping (T) -> Content
    ) {
        self.flow = flow
        self.header = header
        self.cell = cell
    }

    var body: some View {
        ScrollViewReader { scrollView in
            ScrollView {
                LazyVStack {
                    if let header {
                        header()
                            .listRowBackground(Color.clear)
                            .listRowSeparator(.hidden)
                            .listRowInsets(EdgeInsets())
                    }

                    ForEach(pagingState.indices, id: \.self) { index in
                        if let item = pagingState[index] {
                            cell(item)
                                .id(item.id)
                                .onAppear {
                                    pagingState.notify(index: index)
                                }
                        } else {
                            Spacer()
                        }
                    }
                    .padding(EdgeInsets(top: 0, leading: 16, bottom: 0, trailing: 16))

                    if pagingState.loadState.mediator?.append is Paging_commonLoadState.Loading {
                        ProgressView()
                            .frame(maxWidth: .infinity, alignment: .center)
                    }
                }
                .scrollTargetLayout()
            }
            .scrollPosition($position)
            .refreshable {
                await pagingState.refresh()
            }
            .task {
                var task: Task<(), any Error>? = nil
                for await state in flow {
                    task?.cancel()
                    task = Task {
                        await pagingState.collect(state: state)
                    }
                }
                task?.cancel()
            }
        }
    }

}

extension PagingList where Header == EmptyView {
    init(
        flow: SkieSwiftFlow<ListPagingState<T>>,
        @ViewBuilder cell: @escaping (T) -> Content
    ) {
        self.init(
            flow: flow,
            header: nil,
            cell: cell
        )
    }
}
