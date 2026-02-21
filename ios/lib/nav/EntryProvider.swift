//
//  EntryProvider.swift
//  ios
//
//  Created by Anas Erkinjonov on 30/01/26.
//

internal import Shared
import SwiftUI

@ViewBuilder
func getView(screen: Screen?) -> some View {
    switch screen {
    case .login: LoginScreen()

    case .topicList: TopicListScreen()
    case .topic(let topic): TopicScreen(topic: topic)

    case .authorList: AuthorListScreen()
    case .author(let author): AuthorScreen(author: author)

    case .search(let authorId, let topicId, let selectedTab):
        SearchScreen(authorId: authorId, topicId: topicId, selectedTab: selectedTab)

    case .storageUsage: StorageUsageScreen()

    default: EmptyView()
    }
}
