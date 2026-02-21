//
//  Screen.swift
//  ios
//
//  Created by Anas Erkinjonov on 30/01/26.
//
import Foundation
internal import Shared

enum Screen: Hashable {
    case entrance
    case login
    case home
    case snips
    case profile
    case topicList
    case topic(topic: Topic)
    case authorList
    case author(author: Author)
    case search(
        authorId: Int64,
        topicId: Int64?,
        selectedTab: Int = 0)
    case storageUsage
}
