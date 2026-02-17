//
//  TopicListScreen.swift
//  ios
//
//  Created by Anas Erkinjonov on 12/02/26.
//

internal import Shared
import SwiftUI

struct AuthorListScreen: View {
    @Environment(\.env) var env: AppEnvironment
    @Environment(\.navController) var navController: NavController

    @State
    private var viewModel = ObservableViewModel<AuthorListState, AuthorListIntent, AuthorListEvent, AuthorListViewModel>()

    var body: some View {
        PagingList(
            flow: viewModel.viewModel.authors,
            id: \.self,
            header: {
                SearchButton(
                    searchQuery: viewModel.binding(
                        getValue: { state in
                            state.inSearchMode ? state.searchQuery ?? "" : state.searchQuery
                        },
                        getIntent: { value in
                            AuthorListIntentUpdateSearchQuery(
                                query: value == "" ? nil : value,
                                inSearchMode: value != nil
                            )
                        }
                    )
                )
                .padding(.horizontal, 16)
                .padding(.vertical, 8)
            }
        ) { author in
            if let author {
                AuthorCell(author: author) {
                    navController.navigate(screen: .author(author: author))
                }
            } else {
                EmptyView()
            }
        }
        .background(env.backgroundGradient())
        .navigationTitle(Strings.shared.AUTHORS.string())
        .navigationBarTitleDisplayMode(.inline)
        .task {
            await viewModel.collect()
        }
    }
}

#Preview {
    PreviewRoot {
        AuthorListScreen()
    }
}
