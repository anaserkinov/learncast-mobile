//
//  IosDownloadManager.swift
//  ios
//
//  Created by Anas Erkinjonov on 16/02/26.
//

import ActivityKit
import Foundation
internal import Shared
import UserNotifications

// MARK: - Download Item Model
private struct DownloadItem {
    let id: Int64
    let url: URL
    let audioPath: String
    let title: String
    var state: DownloadState
    var progress: Float
    var accessToken: String?
    var refreshToken: String?
    var task: URLSessionDownloadTask?
    var presignedUrl: URL?
    var retryCount: Int = 0
    var isRetryingAuth: Bool = false
}

// MARK: - Live Activity Attributes
public struct DownloadActivityAttributes: ActivityAttributes {
    public struct ContentState: Codable, Hashable {
        public var progress: Double
        public var downloadedCount: Int
        public var totalCount: Int
        public var currentFileName: String
        public var state: String  // "downloading", "paused", "completed", "failed"

        public init(progress: Double, downloadedCount: Int, totalCount: Int, currentFileName: String, state: String) {
            self.progress = progress
            self.downloadedCount = downloadedCount
            self.totalCount = totalCount
            self.currentFileName = currentFileName
            self.state = state
        }
    }

    public var appName: String

    public init(appName: String) {
        self.appName = appName
    }
}

// MARK: - iOS Download Service
class IosDownloadManager: NSObject, DownloadManager {

    // MARK: - Properties
    static let shared = IosDownloadManager()

    private var urlSession: URLSession!
    private let liveActivityManager = DownloadLiveActivityManager()
    private let fileManager = FileManager.default
    private let downloadDao: DownloadDao = inject()
    private let metadataIndex: MetadataIndex = inject()
    private let downloadIndex: CacheIndex = inject(qualifier: named(name: DownloadCacheScope.shared.ID))
    private let downloadDirectory: URL
    private var tokenProvider: TokenProvider?

    private var activeDownloads: [Int64: DownloadItem] = [:]
    private var downloadQueue: [DownloadItem] = []
    private let maxParallelDownloads = 1
    private var currentDownloadCount = 0
    private let maxRetryAttempts = 2

    // Map to track tasks by their taskIdentifier
    private var taskIdToDownloadId: [Int: Int64] = [:]

    private let sessionIdentifier = "com.learncast.media-download"

    // MARK: - Initialization
    private override init() {
        self.downloadDirectory = fileManager.urls(
            for: .documentDirectory,
            in: .userDomainMask
        ).first!.appendingPathComponent("audio", isDirectory: true)
        super.init()
        try? fileManager.createDirectory(
            at: downloadDirectory,
            withIntermediateDirectories: true
        )
        setupURLSession()
    }

    private func setupURLSession() {
        let configuration = URLSessionConfiguration.background(
            withIdentifier: sessionIdentifier
        )
        configuration.isDiscretionary = false
        configuration.sessionSendsLaunchEvents = true
        configuration.timeoutIntervalForRequest = 60
        configuration.timeoutIntervalForResource = 3600
        configuration.waitsForConnectivity = true

        urlSession = URLSession(
            configuration: configuration,
            delegate: self,
            delegateQueue: OperationQueue.main
        )
    }

    // MARK: - Public Methods

    func setTokenProvider(provider: any TokenProvider) {
        tokenProvider = provider
    }

    /// Start a new download
    func startDownload(id: Int64, url: String, audioPath: String, title: String) {
        Task {
            let downloadItem = DownloadItem(
                id: id,
                url: URL(string: url)!,
                audioPath: audioPath,
                title: title,
                state: .downloading,
                progress: 0.0,
                accessToken: nil,
                refreshToken: nil,
                task: nil,
                presignedUrl: nil,
                retryCount: 0,
                isRetryingAuth: false
            )

            await MainActor.run {
                if self.currentDownloadCount < self.maxParallelDownloads {
                    self.executeDownload(downloadItem)
                } else {
                    self.downloadQueue.append(downloadItem)
                    self.activeDownloads[id] = downloadItem
                }
            }
        }
    }

    func ensureDownloading(id: Int64, url: String, audioPath: String, title: String) {
        let downloadItem = activeDownloads[id]
        if downloadItem == nil || downloadItem?.state == .stopped || downloadItem?.task == nil {
            startDownload(id: id, url: url, audioPath: audioPath, title: title)
        }
    }

    /// Pause a download
    func pauseDownload(id: Int64) {
        guard var downloadItem = activeDownloads[id] else { return }

        downloadItem.task?.cancel()
        downloadItem.state = .stopped
        activeDownloads[id] = downloadItem

        Task {
            await MainActor.run {
                currentDownloadCount = max(0, currentDownloadCount - 1)
            }

            try await downloadDao.update(
                id: id,
                state: .stopped,
                percentDownloaded: downloadItem.progress
            )
        }

        updateLiveActivity()
        processQueue()
    }

    /// Resume a download
    func resumeDownload(id: Int64) -> Bool {
        guard var downloadItem = activeDownloads[id] else { return false }
        downloadItem.retryCount = 0
        activeDownloads[id] = downloadItem

        if currentDownloadCount < maxParallelDownloads {
            executeDownload(downloadItem)
        } else {
            downloadQueue.append(downloadItem)
        }
        return true
    }

    /// Cancel and remove a download
    func removeDownload(
        id: Int64,
        audioPath: String
    ) {
        let destinationURL = downloadDirectory.appendingPathComponent((audioPath as NSString).lastPathComponent)
        try? fileManager.removeItem(at: destinationURL)

        Task {
            try await downloadIndex.delete(key: audioPath)
        }

        guard var downloadItem = activeDownloads[id] else { return }

        downloadItem.task?.cancel()
        downloadItem.state = .removing

        activeDownloads.removeValue(forKey: id)

        Task {
            await MainActor.run {
                currentDownloadCount = max(0, currentDownloadCount - 1)
            }

            if let downloadState = try await downloadDao.getById(id: id) {
                try await downloadDao.delete(audioPath: downloadState.audioPath)
            }
        }

        updateLiveActivity()
        processQueue()
    }

    func clear() {
        // Cancel all active download tasks
        for (_, downloadItem) in activeDownloads {
            downloadItem.task?.cancel()
        }

        // Clear all tracking data
        activeDownloads.removeAll()
        downloadQueue.removeAll()
        taskIdToDownloadId.removeAll()
        currentDownloadCount = 0

        // End live activity
        //        liveActivityManager.endActivity()
    }

    // MARK: - Private Methods

    private func executeDownload(_ downloadItem: DownloadItem) {
        var item = downloadItem

        // Create request with authentication header for initial request
        var request = URLRequest(url: item.presignedUrl ?? item.url)
        request.httpMethod = "GET"
        request.timeoutInterval = 60

        if item.presignedUrl == nil {
            // Add authentication header
            if let tokens = tokenProvider?.getTokens() {
                item.accessToken = tokens.second as String?
                item.refreshToken = tokens.first as String?
                activeDownloads[item.id] = item
                request.setValue("Bearer \(item.accessToken!)", forHTTPHeaderField: "Authorization")
            }
        }

        // Create download task
        let task = urlSession.downloadTask(with: request)

        item.task = task
        item.state = .downloading
        activeDownloads[item.id] = item

        // Map task identifier to download ID
        taskIdToDownloadId[task.taskIdentifier] = item.id

        Task {
            await MainActor.run {
                currentDownloadCount += 1
            }

            try await downloadDao.update(
                id: item.id,
                state: .downloading,
                percentDownloaded: downloadItem.progress
            )
        }

        task.resume()

        updateLiveActivity()
    }

    private func retryDownloadWithRefreshedToken(downloadId: Int64) {
        guard var downloadItem = activeDownloads[downloadId],
            !downloadItem.isRetryingAuth,
            downloadItem.retryCount < maxRetryAttempts
        else {
            print("Cannot retry download \(downloadId): max retries reached or already retrying")
            handleDownloadError(
                downloadId: downloadId,
                error: NSError(
                    domain: "DownloadError",
                    code: 401,
                    userInfo: [NSLocalizedDescriptionKey: "Authentication failed after retries"]
                )
            )
            return
        }

        downloadItem.isRetryingAuth = true
        downloadItem.retryCount += 1
        activeDownloads[downloadId] = downloadItem

        print("Retrying download \(downloadId) with token refresh (attempt \(downloadItem.retryCount))")

        Task {
            // Refresh the token
            tokenProvider?.refreshTokens(refreshToken: downloadItem.refreshToken ?? "")
            if let newTokens = tokenProvider?.getTokens(), (newTokens.second as String?) != downloadItem.accessToken {

                await MainActor.run {
                    guard var item = activeDownloads[downloadId] else { return }
                    item.isRetryingAuth = false
                    item.task?.cancel()

                    activeDownloads[downloadId] = item
                    currentDownloadCount = max(0, currentDownloadCount - 1)

                    executeDownload(item)
                }
            } else {
                // Token refresh failed
                await MainActor.run {
                    guard var item = activeDownloads[downloadId] else { return }
                    item.isRetryingAuth = false
                    activeDownloads[downloadId] = item

                    handleDownloadError(
                        downloadId: downloadId,
                        error: NSError(
                            domain: "DownloadError",
                            code: 401,
                            userInfo: [NSLocalizedDescriptionKey: "Token refresh failed"]
                        )
                    )
                }
            }
        }
    }

    private func retryDownloadWithNewPresignedUrl(downloadId: Int64) {
        guard var downloadItem = activeDownloads[downloadId],
            downloadItem.retryCount < maxRetryAttempts
        else {
            print("Cannot retry download \(downloadId): max retries reached")
            handleDownloadError(
                downloadId: downloadId,
                error: NSError(
                    domain: "DownloadError",
                    code: 403,
                    userInfo: [NSLocalizedDescriptionKey: "Presigned URL expired after retries"]
                )
            )
            return
        }

        downloadItem.retryCount += 1
        downloadItem.presignedUrl = nil  // Clear old presigned URL
        downloadItem.task?.cancel()
        activeDownloads[downloadId] = downloadItem
        currentDownloadCount = max(0, currentDownloadCount - 1)

        print("Retrying download \(downloadId) to get new presigned URL (attempt \(downloadItem.retryCount))")

        executeDownload(downloadItem)
    }

    private func processQueue() {
        guard !downloadQueue.isEmpty, currentDownloadCount < maxParallelDownloads else {
            return
        }

        let nextDownload = downloadQueue.removeFirst()
        executeDownload(nextDownload)
    }

    private func getDownloadId(for task: URLSessionTask) -> Int64? {
        return taskIdToDownloadId[task.taskIdentifier]
    }

    private func updateLiveActivity() {
        let activeDownloadsList = activeDownloads.values.filter {
            $0.state == .downloading
        }

        guard !activeDownloadsList.isEmpty else {
            //            liveActivityManager.endActivity()
            return
        }

        //        let totalProgress = activeDownloadsList.reduce(0.0) {
        //            $0 + $1.progress
        //        } / Float(activeDownloadsList.count)
        //        let currentDownload = activeDownloadsList.first

        //        liveActivityManager.updateActivity(
        //            progress: Double(totalProgress) / 100.0,  // Convert to 0-1 range
        //            downloadedCount: activeDownloads.values.filter {
        //                $0.state == .completed
        //            }.count,
        //            totalCount: activeDownloads.count,
        //            currentFileName: currentDownload?.title ?? "Unknown",
        //            state: "downloading"
        //        )
    }
}

// MARK: - URLSessionDownloadDelegate
extension IosDownloadManager: URLSessionDownloadDelegate {

    // Called when download completes
    func urlSession(
        _ session: URLSession,
        downloadTask: URLSessionDownloadTask,
        didFinishDownloadingTo location: URL
    ) {
        guard let downloadId = getDownloadId(for: downloadTask),
            var downloadItem = activeDownloads[downloadId]
        else {
            return
        }

        // Move the downloaded file to permanent location
        let fileName = downloadItem.url.lastPathComponent
        let destinationURL = downloadDirectory.appendingPathComponent(fileName)

        // Remove existing file if present
        try? fileManager.removeItem(at: destinationURL)

        do {
            try fileManager.moveItem(at: location, to: destinationURL)

            // Update download state
            downloadItem.state = .completed
            downloadItem.progress = 100
            activeDownloads[downloadId] = downloadItem
            currentDownloadCount = max(0, currentDownloadCount - 1)

            // Clean up task mapping
            taskIdToDownloadId.removeValue(forKey: downloadTask.taskIdentifier)

            Task {
                try? await metadataIndex.insert(
                    metadata: Metadata(
                        key: downloadItem.audioPath,
                        contentLength: downloadTask.countOfBytesReceived,
                        contentType: downloadTask.response?.mimeType ?? "audio/mpeg"
                    )
                )
                try? await downloadIndex.insert(
                    span: CacheSpan(
                        key: downloadItem.audioPath,
                        startOffset: 0,
                        endOffset: downloadTask.countOfBytesReceived - 1,
                        filePath: destinationURL.lastPathComponent,
                        lastAccessedAt: 0
                    )
                )
                try? await downloadDao.update(
                    id: downloadId,
                    state: .completed,
                    percentDownloaded: 100
                )
            }

            // Update Live Activity or show completion notification
            let hasMoreDownloads = activeDownloads.values.contains {
                $0.state == .downloading
            }
            if hasMoreDownloads {
                updateLiveActivity()
            } else {
                //                liveActivityManager.endActivity(withCompletion: downloadItem.title)
            }

            processQueue()

        } catch {
            print("Error moving downloaded file: \(error.localizedDescription)")
            handleDownloadError(downloadId: downloadId, error: error)
        }
    }

    // Called periodically to report download progress
    func urlSession(
        _ session: URLSession,
        downloadTask: URLSessionDownloadTask,
        didWriteData bytesWritten: Int64,
        totalBytesWritten: Int64,
        totalBytesExpectedToWrite: Int64
    ) {
        guard let downloadId = getDownloadId(for: downloadTask),
            var downloadItem = activeDownloads[downloadId]
        else {
            return
        }

        let progress = Float(totalBytesWritten) * 100 / Float(totalBytesExpectedToWrite)
        downloadItem.progress = progress
        activeDownloads[downloadId] = downloadItem

        Task {
            try await downloadDao.update(
                id: downloadId,
                state: .downloading,
                percentDownloaded: progress
            )
        }

        updateLiveActivity()
    }

    // Called when download is resumed
    func urlSession(
        _ session: URLSession,
        downloadTask: URLSessionDownloadTask,
        didResumeAtOffset fileOffset: Int64,
        expectedTotalBytes: Int64
    ) {
        if let downloadId = getDownloadId(for: downloadTask) {
            Task {
                try await downloadDao.update(
                    id: downloadId,
                    state: .downloading,
                    percentDownloaded: Float(fileOffset * 100) / Float(expectedTotalBytes)
                )
            }
        }
    }

    private func handleDownloadError(downloadId: Int64, error: Error) {
        guard var downloadItem = activeDownloads[downloadId] else { return }

        downloadItem.state = .stopped
        activeDownloads[downloadId] = downloadItem
        currentDownloadCount = max(0, currentDownloadCount - 1)

        Task {
            try await downloadDao.update(
                id: downloadId,
                state: .stopped,
                percentDownloaded: downloadItem.progress
            )
        }

        //        liveActivityManager.updateActivity(
        //            progress: Double(downloadItem.progress) / 100.0,  // Convert to 0-1 range
        //            downloadedCount: 0,
        //            totalCount: activeDownloads.count,
        //            currentFileName: downloadItem.title,
        //            state: "failed"
        //        )

        processQueue()
        updateLiveActivity()
    }
}

// MARK: - URLSessionTaskDelegate (for handling redirects and errors)
extension IosDownloadManager: URLSessionTaskDelegate {
    // Handle 307 redirect - this is where we follow the redirect WITHOUT auth header
    func urlSession(
        _ session: URLSession,
        task: URLSessionTask,
        willPerformHTTPRedirection response: HTTPURLResponse,
        newRequest request: URLRequest,
        completionHandler: @escaping (URLRequest?) -> Void
    ) {
        guard let downloadId = getDownloadId(for: task) else {
            completionHandler(request)
            return
        }

        // Check if this is a 307 redirect (getting presigned URL)
        if response.statusCode == 307 {
            print("Received 307 redirect for download \(downloadId)")

            // Store the presigned URL
            if var downloadItem = activeDownloads[downloadId] {
                downloadItem.presignedUrl = request.url
                downloadItem.retryCount = 0  // Reset retry count on successful redirect
                activeDownloads[downloadId] = downloadItem
            }

            // Create new request WITHOUT authorization header
            var newRequestWithoutAuth = request
            newRequestWithoutAuth.setValue(nil, forHTTPHeaderField: "Authorization")

            // Follow the redirect without auth
            completionHandler(newRequestWithoutAuth)
        } else {
            // For other redirects, follow normally
            completionHandler(request)
        }
    }

    // Handle task completion with error
    func urlSession(
        _ session: URLSession,
        task: URLSessionTask,
        didCompleteWithError error: Error?
    ) {
        guard let error = error,
            let downloadId = getDownloadId(for: task)
        else {
            return
        }

        let nsError = error as NSError

        if nsError.domain == NSURLErrorDomain,
            nsError.code == NSURLErrorCancelled
        {
            // This might be our own cancellation for retry, don't handle as error
            return
        }

        if let response = task.response as? HTTPURLResponse, let downloadItem = activeDownloads[downloadId], response.statusCode == 401 || response.statusCode == 403 {
            // Handle 401 on initial request (getting presigned URL)
            if downloadItem.presignedUrl == nil {
                print("Received 401 on initial request for download \(downloadId)")
                retryDownloadWithRefreshedToken(downloadId: downloadId)
                return
            }

            if downloadItem.presignedUrl != nil {
                print("Received 401-403 on presigned URL for download \(downloadId)")
                retryDownloadWithNewPresignedUrl(downloadId: downloadId)
                return
            }
        }

        // Clean up task mapping
        taskIdToDownloadId.removeValue(forKey: task.taskIdentifier)

        // Handle the error
        print("Download \(downloadId) completed with error: \(error.localizedDescription)")
        handleDownloadError(downloadId: downloadId, error: error)
    }
}

// MARK: - Download Live Activity Manager
class DownloadLiveActivityManager {
    private var currentActivity: Activity<DownloadActivityAttributes>?

    func updateActivity(
        progress: Double,
        downloadedCount: Int,
        totalCount: Int,
        currentFileName: String,
        state: String
    ) {
        Task {
            let contentState = DownloadActivityAttributes.ContentState(
                progress: progress,
                downloadedCount: downloadedCount,
                totalCount: totalCount,
                currentFileName: currentFileName,
                state: state
            )

            if let activity = currentActivity {
                // Update existing activity
                await activity.update(
                    ActivityContent(
                        state: contentState,
                        staleDate: Date().addingTimeInterval(60)
                    )
                )
            } else {
                // Start new activity
                let attributes = DownloadActivityAttributes(appName: appConfig.appName)
                let content = ActivityContent(
                    state: contentState,
                    staleDate: Date().addingTimeInterval(60)
                )

                do {
                    currentActivity = try Activity.request(
                        attributes: attributes,
                        content: content,
                        pushType: nil
                    )
                } catch {
                    print("Error starting Live Activity: \(error.localizedDescription)")
                }
            }
        }
    }

    func endActivity(withCompletion fileName: String? = nil) {
        Task {
            guard let activity = currentActivity else { return }

            let finalState = DownloadActivityAttributes.ContentState(
                progress: 1.0,
                downloadedCount: 0,
                totalCount: 0,
                currentFileName: fileName ?? "All downloads complete",
                state: "completed"
            )

            await activity.end(
                ActivityContent(
                    state: finalState,
                    staleDate: Date()
                ),
                dismissalPolicy: .after(.now + 3)
            )

            currentActivity = nil
        }
    }
}
