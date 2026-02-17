//
//  AuthorScreen.swift
//  ios
//
//  Created by Anas Erkinjonov on 13/02/26.
//

internal import Shared
import SwiftUI

struct AuthorScreen: View {
    @Environment(\.env) var env: AppEnvironment
    @Environment(\.navController) var navController: NavController

    let author: Author

    @State
    private var viewModel = ObservableViewModel<AuthorState, AuthorIntent, AuthorEvent, AuthorViewModel>()

    var body: some View {
        ZStack {
            VStack(spacing: 0) {
                // Tab Selection
                Picker(
                    "",
                    selection: viewModel.binding(
                        getValue: { state in Int(state.selectedTabIndex) },
                        getIntent: { value in
                            if value == 1 {
                                viewModel.handle(intent: AuthorIntentLoadTopics(authorId: author.id))
                            }
                            return AuthorIntentSelectTab(index: Int32(exactly: value)!)
                        }
                    )
                ) {
                    Text(Strings.shared.LESSONS.string())
                        .tag(0)
                    Text(Strings.shared.TOPICS.string())
                        .tag(1)
                }
                .pickerStyle(.segmented)
                .padding(.horizontal, 16)
                .padding(.bottom, 8)

                // Content List
                if viewModel.state.selectedTabIndex == 0 {
                    // Lessons Tab
                    PagingList(
                        flow: viewModel.viewModel.lessons,
                        id: \.?.id
                    ) { (lesson: Lesson?) in
                        if let lesson {
                            LessonCell(lesson: lesson) {
                                viewModel.handle(intent: AuthorIntentAddToQueue(lesson: lesson))
                            }
                        } else {
                            EmptyView()
                        }
                    }
                } else {
                    // Topics Tab
                    PagingList(
                        flow: viewModel.viewModel.topics,
                        id: \.?.id
                    ) { (topic: Topic?) in
                        if let topic {
                            TopicCell(topic: topic) {
                                navController.navigate(screen: .topic(topic: topic))
                            }
                        } else {
                            EmptyView()
                        }
                    }
                }
            }
            .background(env.backgroundGradient())
            .navigationTitle(author.name)
            .navigationBarTitleDisplayMode(.inline)
            .navigationSubtitle(
                Strings.shared.LESSON.quantityString(NSNumber(value: author.lessonCount))
            )
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button {
                        navController.navigate(
                            screen: .search(
                                authorId: author.id,
                                topicId: nil,
                                selectedTab: Int(viewModel.state.selectedTabIndex)
                            )
                        )
                    } label: {
                        Image(systemName: "magnifyingglass")
                            .foregroundColor(.primary)
                    }
                }
            }
            .task {
                await viewModel.collect()
            }
            .onAppear {
                viewModel.handle(intent: AuthorIntentLoadLessons(authorId: author.id))
            }
        }
    }
}

#Preview {
    PreviewRoot {
        AuthorScreen(author: getSampleAuthor())
    }
}
