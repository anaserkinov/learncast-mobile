//
//  SnipListScreen.swift
//  ios
//
//  Created by Anas Erkinjonov on 30/01/26.
//

internal import Shared
import SwiftUI

struct SnipListScreen: View {
    @Environment(\.env) var env: AppEnvironment
    @Environment(\.navController) var navController: NavController

    @State
    private var viewModel = ObservableViewModel<SnipListState, SnipListIntent, SnipListEvent, SnipListViewModel>()

    var body: some View {
        PagingList(
            flow: viewModel.viewModel.snips,
            id: \.?.id,
            header: {
                VStack(alignment: .leading, spacing: 0) {
                    SearchButton(
                        searchQuery: viewModel.binding(
                            getValue: { state in
                                state.inSearchMode ? state.searchQuery ?? "" : state.searchQuery
                            },
                            getIntent: { value in
                                SnipListIntentUpdateSearchQuery(
                                    query: value == "" ? nil : value,
                                    inSearchMode: value != nil
                                )
                            }
                        )
                    )
                    .padding(.top, 8)
                    .padding(.horizontal, 16)
                }
            }
        ) { snip in
            if let snip {
                SnipCell(snip: snip) {
                    viewModel.handle(intent: SnipListIntentAddToQueue(snip: snip))
                }
            } else {
                EmptyView()
            }
        }
        .background(env.backgroundGradient())
        .navigationTitle(Strings.shared.SNIPS.string())
        .navigationBarTitleDisplayMode(.large)
        .task {
            await viewModel.collect()
        }
    }
}

#Preview {
    PreviewRoot {
        SnipListScreen()
    }
}
