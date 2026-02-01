//
//  Screen.swift
//  ios
//
//  Created by Anas Erkinjonov on 30/01/26.
//
import Foundation
import Shared

enum Screen: Hashable{
    case Entrance
    case Login
    case Home
    case Snips
    case Profile
    case TopicList
    case Topic(topic: Topic)
    case AuthorList
    case Author(author: Author)
    case Search(
        authorId: Int64,
        topicId: Int64?,
        selectedTab: Int = 0,
    )
    case StorageUsageScreen
}
