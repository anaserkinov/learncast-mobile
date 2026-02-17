//
//  TopicScreen.swift
//  ios
//
//  Created by Anas Erkinjonov on 12/02/26.
//

internal import Shared
import SwiftUI

struct TopicScreen: View {
    @Environment(\.env) var env: AppEnvironment
    @Environment(\.navController) var navController: NavController

    let topic: Topic

    @State
    private var viewModel = ObservableViewModel<TopicState, TopicIntent, TopicEvent, TopicViewModel>()

    var body: some View {
        ZStack {
            PagingList(
                flow: viewModel.viewModel.lessons,
                id: \.?.id,
                header: {
                    VStack(alignment: .leading, spacing: 0) {
                        // Play All and Search buttons
                        HStack(spacing: 24) {
                            Button(
                                action: {
                                    viewModel.handle(
                                        intent: TopicIntentPlayAll(
                                            topicId: topic.id,
                                            authorId: topic.authorId
                                        )
                                    )
                                }
                            ) {
                                Text(Strings.shared.PLAY_ALL.string())
                            }
                            .buttonStyle(.glass)

                            Button {
                                navController.navigate(
                                    screen: .search(
                                        authorId: topic.authorId,
                                        topicId: topic.id
                                    )
                                )
                            } label: {
                                Image(systemName: "magnifyingglass")
                                    .foregroundColor(.primary)
                            }

                            Spacer()
                        }
                        .padding(.horizontal, 16)
                        .padding(.bottom, 8)

                        // Description if available
                        if let description = topic.description_ {
                            Text(description)
                                .font(Typography.bodyMedium)
                                .padding(.horizontal, 16)
                                .padding(.bottom, 8)
                        }
                    }
                }
            ) { (lesson: Lesson?) in
                if let lesson {
                    LessonCell(lesson: lesson) {
                        viewModel.handle(intent: TopicIntentAddToQueue(lesson: lesson))
                    }
                } else {
                    EmptyView()
                }
            }
            .background(env.backgroundGradient())
            .navigationTitle(topic.title)
            .navigationBarTitleDisplayMode(.large)
            .navigationSubtitle(topic.authorName)
            .task {
                await viewModel.collect()
            }
            .onAppear {
                viewModel.handle(
                    intent: TopicIntentLoad(
                        topicId: topic.id,
                        authorId: topic.authorId
                    )
                )
            }

            if viewModel.state.isLoading {
                LoaderView()
            }
        }
    }
}

#Preview {
    PreviewRoot {
        TopicScreen(topic: getSampleTopic())
    }
}
