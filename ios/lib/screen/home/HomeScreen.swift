//
//  HomeScreen.swift
//  ios
//
//  Created by Anas Erkinjonov on 30/01/26.
//

import Shared
import SwiftUI

struct HomeScreen: View {
    @Environment(\.env) var env: AppEnvironment

    @State
    private var viewModel = ObservableViewModel<HomeState, HomeIntent, HomeEvent, HomeViewModel>()

    @State
    private var pagingState = ListPagingState<Lesson>()

    var body: some View {
        PagingList(
            pagingState: pagingState,
            id: \.?.id,
            header: {
                VStack(alignment: .leading, spacing: 0) {
                    Text(Strings.shared.HOME.string())
                        .font(Typography.headlineMedium)
                        .fontWeight(.bold)
                        .padding(.horizontal, 16)
                        .padding(.top, 16)

                    HStack(spacing: 12) {
                        PrimaryButton(
                            titleKey: Strings.shared.AUTHORS,
                            icon: "person",
                            onClick: {

                            }
                        )
                        PrimaryButton(
                            titleKey: Strings.shared.TOPICS,
                            icon: "square.grid.2x2",
                            onClick: {

                            }
                        )
                    }
                    .padding(.top, 20)
                    .padding(.horizontal, 16)

                    SearchButton(
                        searchQuery: viewModel.binding(
                            getValue: { state in
                                state.inSearchMode ? state.searchQuery ?? "" : state.searchQuery
                            },
                            getIntent: { value in
                                HomeIntentUpdateSearchQuery(
                                    query: value == "" ? nil : value,
                                    inSearchMode: value != nil
                                )
                            }
                        )
                    )
                    .padding(.top, 12)
                    .padding(.horizontal, 16)

                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 12) {
                            ForEach(Filters.allCases, id: \.self) { filter in
                                FilterChip(
                                    title: filter.titleKey.string(),
                                    isSelected: filter == viewModel.state.selectedFilter,
                                    onTap: {
                                        viewModel.handle(
                                            intent: HomeIntentSelectFilter(filter: filter))
                                    }
                                )
                            }
                        }
                        .padding(.vertical, 12)
                        .padding(.horizontal, 16)
                    }
                }
            }
        ) { lesson in
            if let lesson {
                LessonCell(lesson: lesson) {

                }
            } else {
                EmptyView()
            }
        }
        .background(env.backgroundGradient())
        .scrollDismissesKeyboard(.interactively)
        .listStyle(PlainListStyle())
        .task {
            for await lessonsFlow in viewModel.viewModel.lessons {
                pagingState.update(flow: lessonsFlow.castToPagingFlow())
            }
        }
        .task {
            await viewModel.collect()
        }
    }
}

#Preview {
    PreviewRoot {
        HomeScreen()
    }
}
