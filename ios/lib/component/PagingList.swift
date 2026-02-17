//
//  PagingView.swift
//  ios
//
//  Created by Anas Erkinjonov on 31/01/26.
//

internal import Shared
import SwiftUI

struct PagingList<T: AnyObject & Hashable, Content: View, ID: Hashable, Header: View, Footer: View>:
    View
{

    @State
    private var pagingState = ObservablePagingState<T>()

    private let flow: SkieSwiftFlow<ListPagingState<T>>
    private let cell: (T?) -> Content
    private let id: KeyPath<Array<T>.Element?, ID>
    private let header: (() -> Header)?
    private let footer: (() -> Footer)?

    init(
        flow: SkieSwiftFlow<ListPagingState<T>>,
        id: KeyPath<Array<T>.Element?, ID>,
        header: (() -> Header)? = nil,
        footer: (() -> Footer)? = nil,
        @ViewBuilder cell: @escaping (Array<T>.Element?) -> Content
    ) {
        self.flow = flow
        self.id = id
        self.header = header
        self.footer = footer
        self.cell = cell
    }

    var body: some View {
        List {
            if let header {
                header()
                    .listRowBackground(Color.clear)
                    .listRowSeparator(.hidden)
                    .listRowInsets(EdgeInsets())
            }

            ForEach(
                pagingState.list,
                id: id
            ) { item in
                cell(item)
            }
            .listRowBackground(Color.clear)
            .listRowSeparator(.hidden)
            .listRowInsets(EdgeInsets(top: 0, leading: 16, bottom: 0, trailing: 16))

            if pagingState.loadState.append is Paging_commonLoadState.Loading {
                ProgressView()
                    .frame(maxWidth: .infinity, alignment: .center)
            }

            if let footer {
                footer()
                    .listRowBackground(Color.clear)
                    .listRowSeparator(.hidden)
                    .listRowInsets(EdgeInsets())
            }
        }
        .listStyle(.plain)
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

extension PagingList where Header == EmptyView, Footer == EmptyView {
    init(
        flow: SkieSwiftFlow<ListPagingState<T>>,
        id: KeyPath<Array<T>.Element?, ID>,
        @ViewBuilder cell: @escaping (Array<T>.Element?) -> Content
    ) {
        self.init(
            flow: flow,
            id: id,
            header: nil,
            footer: nil,
            cell: cell
        )
    }
}

extension PagingList where Footer == EmptyView {
    init(
        flow: SkieSwiftFlow<ListPagingState<T>>,
        id: KeyPath<Array<T>.Element?, ID>,
        header: @escaping () -> Header,
        @ViewBuilder cell: @escaping (Array<T>.Element?) -> Content
    ) {
        self.init(
            flow: flow,
            id: id,
            header: header,
            footer: nil,
            cell: cell
        )
    }
}
