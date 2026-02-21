//
//  SearchScreen.swift
//  ios
//
//  Created by Anas Erkinjonov on 13/02/26.
//

internal import Shared
import SwiftUI

struct SearchScreen: View {
    @Environment(\.env) var env: AppEnvironment
    @Environment(\.navController) var navController: NavController
    @Namespace private var animation

    let authorId: Int64
    let topicId: Int64?
    let selectedTab: Int

    @State
    private var viewModel = ObservableViewModel<SearchState, SearchIntent, SearchEvent, SearchViewModel>()

    @FocusState private var searchFieldFocused: Bool

    var body: some View {
        VStack(spacing: 0) {
            // Search Input
            SearchInput(
                text: viewModel.binding(
                    getValue: { state in state.searchQuery },
                    getIntent: { value in
                        SearchIntentUpdateSearchQuery(query: value)
                    }
                ),
                isFocused: $searchFieldFocused
            )
            .padding(.horizontal, 16)
            .padding(.vertical, 8)

            // Tab Row (only if topicId is nil)
            if topicId == nil {
                Picker(
                    "",
                    selection: viewModel.binding(
                        getValue: { state in Int(state.selectedTab) },
                        getIntent: { value in
                            SearchIntentSelectTab(value: Int32(exactly: value)!)
                        }
                    )
                ) {
                    Text(Strings.shared.LESSONS.string())
                        .tag(0)
                    Text(Strings.shared.TOPICS.string())
                        .tag(1)
                }
                .pickerStyle(SegmentedPickerStyle())
                .padding(.horizontal, 16)
                .padding(.bottom, 8)
            }

            // Content based on selected tab
            if viewModel.state.selectedTab == 0 {
                // Lessons List
                PagingList(
                    flow: viewModel.viewModel.lessons
                ) { lesson in
                    LessonCell(lesson: lesson) {
                        viewModel.handle(intent: SearchIntentAddToQueue(lesson: lesson))
                    }
                }
            } else {
                // Topics List
                PagingList(
                    flow: viewModel.viewModel.topics
                ) { topic in
                    TopicCell(topic: topic) {
                        navController.navigate(screen: .topic(topic: topic))
                    }
                }
            }
        }
        .contentMargins(.bottom, Utils.bottomPadding)
        .background(env.backgroundGradient())
        .navigationBarTitleDisplayMode(.inline)
        .task {
            await viewModel.collect()
        }
        .onAppear {
            searchFieldFocused = true
            if selectedTab != 0 {
                viewModel.handle(intent: SearchIntentSelectTab(value: Int32(selectedTab)))
            }
            viewModel.handle(
                intent: SearchIntentLoad(
                    authorId: authorId,
                    topicId: topicId as? KotlinLong
                )
            )
        }
        .navigationTransition(.zoom(sourceID: "search", in: animation))
    }
}

#Preview {
    PreviewRoot {
        SearchScreen(authorId: 1, topicId: nil, selectedTab: 0)
    }
}
