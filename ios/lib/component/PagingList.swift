//
//  PagingView.swift
//  ios
//
//  Created by Anas Erkinjonov on 31/01/26.
//

import SwiftUI
import Shared

struct PagingList<T: AnyObject & Hashable, Content: View, ID: Hashable, Header: View, Footer: View>: View {
    
    private let pagingState: ListPagingState<T>
    private let cell: (T?) -> Content
    private let id: KeyPath<ListPagingState<T>.Element, ID>
    private let header: (() -> Header)?
    private let footer: (() -> Footer)?

    
    init(
        pagingState: ListPagingState<T>,
        id: KeyPath<ListPagingState<T>.Element, ID>,
        header: (() -> Header)? = nil,
        footer: (() -> Footer)? = nil,
        @ViewBuilder cell: @escaping (ListPagingState<T>.Element) -> Content
    ) {
        self.pagingState = pagingState
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
                pagingState,
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
            pagingState.refresh()
        }
    }
    
}

#Preview {
    PreviewRoot {
        let pagingState = ListPagingState<Lesson>()
        pagingState.update(flow: getLessonSamplePagingData(count: 2))
        return PagingList(
            pagingState: pagingState,
            id: \.?.id
        ) { lesson in
            if let lesson {
                LessonCell(lesson: lesson) {
                    // handle click
                }
            } else {
                EmptyView()
            }
        }
        .background(AppEnvironment().backgroundGradient())
        .listStyle(PlainListStyle())
    }
}

extension PagingList where Header == EmptyView, Footer == EmptyView {
    init(
        pagingState: ListPagingState<T>,
        id: KeyPath<ListPagingState<T>.Element, ID>,
        @ViewBuilder cell: @escaping (ListPagingState<T>.Element) -> Content
    ) {
        self.init(
            pagingState: pagingState,
            id: id,
            header: nil,
            footer: nil,
            cell: cell
        )
    }
}

extension PagingList where Footer == EmptyView {
    init(
        pagingState: ListPagingState<T>,
        id: KeyPath<ListPagingState<T>.Element, ID>,
        header: @escaping () -> Header,
        @ViewBuilder cell: @escaping (ListPagingState<T>.Element) -> Content
    ) {
        self.init(
            pagingState: pagingState,
            id: id,
            header: header,
            footer: nil,
            cell: cell
        )
    }
}
