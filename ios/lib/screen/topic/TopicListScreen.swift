//
//  TopicListScreen.swift
//  ios
//
//  Created by Anas Erkinjonov on 12/02/26.
//

internal import Shared
import SwiftUI

struct TopicListScreen: View {
    @Environment(\.env) var env: AppEnvironment
    @Environment(\.navController) var navController: NavController

    @State
    private var viewModel = ObservableViewModel<TopicListState, TopicListIntent, TopicListEvent, TopicListViewModel>()

    var body: some View {
        PagingList(
            flow: viewModel.viewModel.topics,
            header: {
                SearchButton(
                    searchQuery: viewModel.binding(
                        getValue: { state in
                            state.inSearchMode ? state.searchQuery ?? "" : state.searchQuery
                        },
                        getIntent: { value in
                            TopicListIntentUpdateSearchQuery(
                                query: value == "" ? nil : value,
                                inSearchMode: value != nil
                            )
                        }
                    )
                )
                .padding(.horizontal, 16)
                .padding(.vertical, 8)
            }
        ) { (topic: Topic?) in
            if let topic {
                TopicCell(topic: topic) {
                    navController.navigate(screen: .topic(topic: topic))
                }
            } else {
                EmptyView()
            }
        }
        .contentMargins(.bottom, Utils.bottomPadding)
        .background(env.backgroundGradient())
        .navigationTitle(Strings.shared.TOPICS.string())
        .navigationBarTitleDisplayMode(.inline)
        .task {
            await viewModel.collect()
        }
    }
}

#Preview {
    PreviewRoot {
        TopicListScreen()
    }
}
