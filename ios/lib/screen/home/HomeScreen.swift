//
//  HomeScreen.swift
//  ios
//
//  Created by Anas Erkinjonov on 30/01/26.
//

internal import Shared
import SwiftUI

struct HomeScreen: View {
    @Environment(\.env) var env: AppEnvironment
    @Environment(\.navController) var navController: NavController

    @State
    private var viewModel = ObservableViewModel<HomeState, HomeIntent, HomeEvent, HomeViewModel>()

    var body: some View {
        PagingList(
            flow: viewModel.viewModel.lessons,
            header: {
                VStack(alignment: .leading, spacing: 0) {
                    HStack(spacing: 12) {
                        PrimaryButton(
                            titleKey: Strings.shared.AUTHORS,
                            icon: "person",
                            onClick: {
                                navController.navigate(screen: .authorList)
                            }
                        )
                        PrimaryButton(
                            titleKey: Strings.shared.TOPICS,
                            icon: "square.grid.2x2",
                            onClick: {
                                navController.navigate(screen: .topicList)
                            }
                        )
                    }
                    .padding(.top, 8)
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
            LessonCell(lesson: lesson) {
                viewModel.handle(intent: HomeIntentAddToQueue(lesson: lesson))
            }
        }
        .contentMargins(.bottom, Utils.bottomPadding)
        .background(env.backgroundGradient())
        .navigationTitle(Strings.shared.HOME.string())
        .navigationBarTitleDisplayMode(.large)
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
