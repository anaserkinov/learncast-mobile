//
//  NoRedirectSessionDelegate.swift
//  ios
//
//  Created by Anas Erkinjonov on 08/02/26.
//

import Foundation

final class NoRedirectSessionDelegate: NSObject, URLSessionTaskDelegate {

    func urlSession(
        _ session: URLSession,
        task: URLSessionTask,
        willPerformHTTPRedirection response: HTTPURLResponse,
        newRequest request: URLRequest,
        completionHandler: @escaping (URLRequest?) -> Void
    ) {
        // Stop automatic redirect
        completionHandler(nil)
    }
}
