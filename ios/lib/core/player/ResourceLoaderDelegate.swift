//
//  ResourceLoaderDelegate.swift
//  ios
//
//  Optimized for unified downloads and cache with chunked streaming
//

import AVFoundation
internal import Shared

class ResourceLoaderDelegate: NSObject, AVAssetResourceLoaderDelegate {

    // MARK: - Properties
    private var tokenProvider: TokenProvider

    // Track the single resource this loader handles
    private let resourceURL: URL
    private var presignedURL: URL?

    // Unified storage manager
    private let storageManager: StorageManager

    // Resource key for storage
    private let cacheKey: String

    // Active loading requests
    private var pendingRequests: Set<AVAssetResourceLoadingRequest> = []

    // Content metadata (cached in memory for fast access)
    private var contentType: String?
    private var contentLength: Int64?

    // Prevent concurrent presigned URL fetches
    private var presignedURLTask: Task<URL, Error>?

    // URLSession without redirect following
    private lazy var urlSession: URLSession = {
        let config = URLSessionConfiguration.default
        config.requestCachePolicy = .reloadIgnoringLocalCacheData
        return URLSession(
            configuration: config,
            delegate: NoRedirectSessionDelegate(),
            delegateQueue: nil
        )
    }()

    // Queue for thread-safe request management
    private let queue = DispatchQueue(label: "com.app.resourceloader", qos: .userInitiated)

    // MARK: - Initialization

    init(
        storageManager: StorageManager,
        tokenProvider: TokenProvider,
        resourceURL: URL,
        cacheKey: String
    ) {
        self.storageManager = storageManager
        self.tokenProvider = tokenProvider
        self.resourceURL = resourceURL
        self.cacheKey = cacheKey
        super.init()
    }

    // MARK: - AVAssetResourceLoaderDelegate

    func resourceLoader(
        _ resourceLoader: AVAssetResourceLoader,
        shouldWaitForLoadingOfRequestedResource loadingRequest: AVAssetResourceLoadingRequest
    ) -> Bool {
        logi(message: "🔵 [ResourceLoader] New loading request")
        logd(message: "   URL: \(loadingRequest.request.url?.absoluteString ?? "nil")")

        queue.sync {
            pendingRequests.insert(loadingRequest)
        }

        if loadingRequest.contentInformationRequest != nil {
            logd(message: "   📋 Content info requested")
            if let dataRequest = loadingRequest.dataRequest {
                logd(message: "   📦 Data requested: offset=\(dataRequest.requestedOffset), length=\(dataRequest.requestedLength)")
            }
        } else if let dataRequest = loadingRequest.dataRequest {
            logd(message: "   📦 Data only: offset=\(dataRequest.requestedOffset), length=\(dataRequest.requestedLength)")
        }

        Task {
            await processLoadingRequest(loadingRequest)
        }

        return true
    }

    func resourceLoader(
        _ resourceLoader: AVAssetResourceLoader,
        didCancel loadingRequest: AVAssetResourceLoadingRequest
    ) {
        logw(message: "🟡 [ResourceLoader] Request cancelled")
        if let dataRequest = loadingRequest.dataRequest {
            logd(message: "   Range: offset=\(dataRequest.requestedOffset), length=\(dataRequest.requestedLength)")
        }

        queue.sync {
            pendingRequests.remove(loadingRequest)
        }
    }

    // MARK: - Request Processing

    private func processLoadingRequest(_ loadingRequest: AVAssetResourceLoadingRequest) async {
        do {
            // Step 1: Handle content info request first (if present)
            if loadingRequest.contentInformationRequest != nil {
                try await fillContentInformation(for: loadingRequest)
            }

            // Step 2: Handle data request with multi-source streaming
            if let dataRequest = loadingRequest.dataRequest {
                try await streamDataFromSources(for: loadingRequest, dataRequest: dataRequest)
            }

            // Finish successfully
            if !loadingRequest.isCancelled {
                loadingRequest.finishLoading()
                logi(message: "✅ [ResourceLoader] Request completed successfully")
            }

        } catch {
            loge(message: "❌ [ResourceLoader] Request failed: \(error.localizedDescription)")
            if !loadingRequest.isCancelled {
                loadingRequest.finishLoading(with: error)
            }
        }

        queue.sync {
            pendingRequests.remove(loadingRequest)
        }
    }

    // MARK: - Content Information

    private func fillContentInformation(
        for loadingRequest: AVAssetResourceLoadingRequest
    ) async throws {
        guard let contentInfo = loadingRequest.contentInformationRequest else { return }

        // If we already have content info cached in memory, use it
        if let cachedContentType = contentType,
            let cachedContentLength = contentLength
        {

            logd(message: "📋 [ResourceLoader] Using memory-cached content info")
            contentInfo.contentType = cachedContentType
            contentInfo.contentLength = cachedContentLength
            contentInfo.isByteRangeAccessSupported = true
            return
        }

        // Check if we have metadata in storage (even from partial download/cache)
        if let metadata = try? await storageManager.getMetadata(forKey: cacheKey) {

            logd(message: "📋 [ResourceLoader] Using storage metadata")
            contentInfo.contentType = metadata.contentType
            contentInfo.contentLength = metadata.contentLength
            contentInfo.isByteRangeAccessSupported = true

            self.contentType = metadata.contentType
            self.contentLength = metadata.contentLength
            return
        }

        // Otherwise, fetch from remote
        try await fetchContentInformationFromRemote(for: loadingRequest)
    }

    private func fetchContentInformationFromRemote(
        for loadingRequest: AVAssetResourceLoadingRequest
    ) async throws {
        logd(message: "📋 [ResourceLoader] Fetching content metadata from remote...")

        let urlToLoad = try await getPresignedURL()

        let from = loadingRequest.dataRequest?.requestedOffset ?? 0
        let to = from + max(Int64(loadingRequest.dataRequest?.requestedLength ?? 2), 128)

        var request = URLRequest(url: urlToLoad)
        request.setValue("bytes=\(from)-\(to)", forHTTPHeaderField: "Range")
        request.httpMethod = "GET"

        let (data, response) = try await urlSession.data(for: request)

        guard let httpResponse = response as? HTTPURLResponse else {
            throw NSError(domain: "ResourceLoaderDelegate", code: -1, userInfo: nil)
        }

        // Handle auth errors
        if httpResponse.statusCode == 401 || httpResponse.statusCode == 403 {
            logw(message: "🔄 [ResourceLoader] Auth failed, refreshing presigned URL...")
            self.presignedURL = nil
            presignedURLTask?.cancel()
            presignedURLTask = nil

            let freshURL = try await getPresignedURL()
            var retryRequest = URLRequest(url: freshURL)
            request.setValue("bytes=\(from)-\(to)", forHTTPHeaderField: "Range")
            retryRequest.httpMethod = "GET"

            let (data, retryResponse) = try await urlSession.data(for: retryRequest)
            guard let retryHTTPResponse = retryResponse as? HTTPURLResponse,
                retryHTTPResponse.statusCode < 400
            else {
                throw NSError(domain: "ResourceLoaderDelegate", code: httpResponse.statusCode, userInfo: nil)
            }

            try await parseAndStoreContentInfo(
                from: from,
                to: to,
                data: data,
                response: retryHTTPResponse,
                loadingRequest: loadingRequest
            )
        } else if httpResponse.statusCode >= 400 {
            throw NSError(domain: "ResourceLoaderDelegate", code: httpResponse.statusCode, userInfo: nil)
        } else {
            try await parseAndStoreContentInfo(
                from: from,
                to: to,
                data: data,
                response: httpResponse,
                loadingRequest: loadingRequest
            )
        }
    }

    private func parseAndStoreContentInfo(
        from: Int64,
        to: Int64,
        data: Data,
        response: HTTPURLResponse,
        loadingRequest: AVAssetResourceLoadingRequest
    ) async throws {
        guard let contentInfo = loadingRequest.contentInformationRequest else { return }

        // Get content type
        let type = response.mimeType ?? "audio/mpeg"
        contentInfo.contentType = type
        self.contentType = type
        logd(message: "   ✓ Content-Type: \(type)")

        // Get content length - try Content-Range first, then Content-Length
        var length: Int64 = 0

        // Try parsing from Content-Range header first (e.g., "bytes 0-1023/5000")
        if let contentRange = response.value(forHTTPHeaderField: "Content-Range"),
            let totalLength = parseContentLengthFromRange(contentRange)
        {
            contentInfo.contentLength = totalLength
            self.contentLength = totalLength
            length = totalLength
            logd(message: "   ✓ Content-Length (from Range): \(totalLength)")
        }
        // Fall back to Content-Length header
        else if let contentLengthString = response.value(forHTTPHeaderField: "Content-Length"),
            let parsedLength = Int64(contentLengthString)
        {
            contentInfo.contentLength = parsedLength
            self.contentLength = parsedLength
            length = parsedLength
            logd(message: "   ✓ Content-Length: \(parsedLength)")
        } else {
            logw(message: "   ⚠️ No Content-Length or Content-Range header")
        }

        // Store metadata in storage for future use
        if let length = Optional(length) {
            await storageManager.saveMetadata(
                forKey: cacheKey,
                contentLength: length,
                contentType: type
            )
            try? await storageManager.saveCacheSpan(
                forKey: cacheKey,
                data: data,
                range: from..<(to + 1)
            )
        }

        // CRITICAL: Mark as byte-range accessible
        contentInfo.isByteRangeAccessSupported = true
        logd(message: "   ✓ Byte-range access: true")
    }

    private func parseContentLengthFromRange(_ contentRange: String) -> Int64? {
        // Content-Range format: "bytes start-end/total" or "bytes */total"
        // Examples: "bytes 0-1023/5000", "bytes */5000"
        let components = contentRange.components(separatedBy: "/")
        guard components.count == 2,
            let totalString = components.last,
            totalString != "*",
            let total = Int64(totalString)
        else {
            return nil
        }
        return total
    }

    // MARK: - Multi-Source Data Streaming

    private func streamDataFromSources(
        for loadingRequest: AVAssetResourceLoadingRequest,
        dataRequest: AVAssetResourceLoadingDataRequest
    ) async throws {
        let requestedOffset = dataRequest.requestedOffset
        let requestedLength = dataRequest.requestedLength
        let currentOffset = dataRequest.currentOffset

        logd(message: "📦 [ResourceLoader] Processing data request...")
        logd(message: "   Requested: \(requestedOffset) - \(requestedOffset + Int64(requestedLength) - 1)")
        logd(message: "   Current offset: \(currentOffset)")

        // Calculate actual range to fetch
        let startOffset = currentOffset
        let endOffset = requestedOffset + Int64(requestedLength) - 1
        let requestRange = startOffset..<(endOffset + 1)

        // Get all stored ranges (complete download or cache segments)
        let segments: [DataSegment]

        if let downloaded = try await storageManager.getDownload(forKey: cacheKey) {
            logd(message: "   📥 Complete download available")
            segments = [
                DataSegment(
                    source: .download(
                        span: downloaded,
                        offset: startOffset,
                        length: endOffset - startOffset + 1
                    ),
                    requestedStart: startOffset,
                    requestedEnd: endOffset
                )
            ]
        } else {
            let cacheSpans = try await storageManager.getSpans(
                forKey: cacheKey,
                intersecting: requestRange
            )
            logd(message: "   💾 Stored ranges: \(cacheSpans.count)")

            // Calculate segments
            segments = RangeCalculator.mergeSegments(
                requestedRange: requestRange,
                cacheSpans: cacheSpans
            )
        }

        logd(message: "   📊 Segments to process: \(segments.count)")
        #if DEBUG
            for (index, segment) in segments.enumerated() {
                switch segment.source {
                case .download:
                    logv(message: "      \(index): Download(\(segment.requestedStart)-\(segment.requestedEnd))")
                case .cache:
                    logv(message: "      \(index): Cache(\(segment.requestedStart)-\(segment.requestedEnd))")
                case .remote(let start, let end):
                    logv(message: "      \(index): Remote(\(start)-\(end))")
                }
            }
        #endif

        // Stream segments
        try await streamSegments(
            segments,
            to: dataRequest,
            loadingRequest: loadingRequest
        )
    }

    private func streamSegments(
        _ segments: [DataSegment],
        to dataRequest: AVAssetResourceLoadingDataRequest,
        loadingRequest: AVAssetResourceLoadingRequest
    ) async throws {
        var totalBytesSent: Int64 = 0

        var lastFileHandlePath: String? = nil
        var fileHandle: FileHandle? = nil

        defer {
            try? fileHandle?.close()
        }

        for segment in segments {
            // Check for cancellation
            if loadingRequest.isCancelled {
                logw(message: "🟡 [ResourceLoader] Request cancelled during segment streaming")
                throw CancellationError()
            }

            switch segment.source {
            case .download(let span, let offset, let length):
                if lastFileHandlePath != span.filePath {
                    lastFileHandlePath = span.filePath
                    fileHandle = try FileHandle(forReadingFrom: storageManager.downloadDirectory.appendingPathComponent(span.filePath))
                }
                try await streamFromStorage(
                    fileHandle: fileHandle!,
                    offset: offset,
                    length: length,
                    to: dataRequest,
                    loadingRequest: loadingRequest,
                    totalBytesSent: &totalBytesSent
                )
            case .cache(let span, let offset, let length):
                await storageManager.touchSpan(span: span)
                if lastFileHandlePath != span.filePath {
                    lastFileHandlePath = span.filePath
                    fileHandle = try FileHandle(forReadingFrom: storageManager.cacheDirectory.appendingPathComponent(span.filePath))
                }
                try await streamFromStorage(
                    fileHandle: fileHandle!,
                    offset: offset,
                    length: length,
                    to: dataRequest,
                    loadingRequest: loadingRequest,
                    totalBytesSent: &totalBytesSent
                )
            case .remote(let startOffset, let endOffset):
                try await streamFromRemote(
                    startOffset: startOffset,
                    endOffset: endOffset,
                    to: dataRequest,
                    loadingRequest: loadingRequest,
                    totalBytesSent: &totalBytesSent
                )
            }
        }

        logi(message: "✅ [ResourceLoader] All segments streamed: \(totalBytesSent) bytes total")
    }

    // MARK: - Stream from Storage

    private func streamFromStorage(
        fileHandle: FileHandle,
        offset: Int64,
        length: Int64,
        to dataRequest: AVAssetResourceLoadingDataRequest,
        loadingRequest: AVAssetResourceLoadingRequest,
        totalBytesSent: inout Int64
    ) async throws {
        logd(message: "   💾 Streaming from storage: offset=\(offset), length=\(length)")

        let chunkSize: Int64 = 256 * 1024  // 256KB chunks
        var remainingLength = length
        var currentOffset = offset

        while remainingLength > 0 {
            // Check for cancellation
            if loadingRequest.isCancelled {
                logw(message: "🟡 [ResourceLoader] Cancelled during storage streaming")
                throw CancellationError()
            }

            let bytesToRead = min(chunkSize, remainingLength)

            // Seek to offset
            try fileHandle.seek(toOffset: UInt64(currentOffset))

            // Read data
            guard let data = try fileHandle.read(upToCount: Int(bytesToRead)) else {
                throw StorageError.storageReadFailed
            }

            dataRequest.respond(with: data)
            totalBytesSent += Int64(data.count)
            remainingLength -= Int64(data.count)
            currentOffset += Int64(data.count)

            logv(message: "      ↗️ Sent chunk from storage: \(data.count) bytes")
        }
    }

    // MARK: - Stream from Remote (with caching)

    private func streamFromRemote(
        startOffset: Int64,
        endOffset: Int64,
        to dataRequest: AVAssetResourceLoadingDataRequest,
        loadingRequest: AVAssetResourceLoadingRequest,
        totalBytesSent: inout Int64
    ) async throws {
        logd(message: "   🌐 Streaming from remote: \(startOffset)-\(endOffset)")

        let urlToLoad = try await getPresignedURL()

        var request = URLRequest(url: urlToLoad)
        request.setValue("bytes=\(startOffset)-\(endOffset)", forHTTPHeaderField: "Range")

        let (asyncBytes, response) = try await urlSession.bytes(for: request)

        guard let httpResponse = response as? HTTPURLResponse else {
            throw NSError(domain: "ResourceLoaderDelegate", code: -1, userInfo: nil)
        }

        // Handle auth errors
        if httpResponse.statusCode == 401 || httpResponse.statusCode == 403 {
            logw(message: "🔄 [ResourceLoader] Auth failed during remote streaming, refreshing URL...")
            self.presignedURL = nil
            presignedURLTask?.cancel()
            presignedURLTask = nil

            let freshURL = try await getPresignedURL()
            var retryRequest = URLRequest(url: freshURL)
            retryRequest.setValue("bytes=\(startOffset)-\(endOffset)", forHTTPHeaderField: "Range")

            let (retryBytes, retryResponse) = try await urlSession.bytes(for: retryRequest)
            guard let retryHTTPResponse = retryResponse as? HTTPURLResponse,
                retryHTTPResponse.statusCode < 400
            else {
                throw NSError(domain: "ResourceLoaderDelegate", code: httpResponse.statusCode, userInfo: nil)
            }

            try await streamAndCacheRemoteBytes(
                asyncBytes: retryBytes,
                startOffset: startOffset,
                to: dataRequest,
                loadingRequest: loadingRequest,
                totalBytesSent: &totalBytesSent
            )
        } else if httpResponse.statusCode >= 400 {
            throw NSError(domain: "ResourceLoaderDelegate", code: httpResponse.statusCode, userInfo: nil)
        } else {
            try await streamAndCacheRemoteBytes(
                asyncBytes: asyncBytes,
                startOffset: startOffset,
                to: dataRequest,
                loadingRequest: loadingRequest,
                totalBytesSent: &totalBytesSent
            )
        }
    }

    private func streamAndCacheRemoteBytes(
        asyncBytes: URLSession.AsyncBytes,
        startOffset: Int64,
        to dataRequest: AVAssetResourceLoadingDataRequest,
        loadingRequest: AVAssetResourceLoadingRequest,
        totalBytesSent: inout Int64
    ) async throws {
        let chunkSize = 256 * 1024  // 256KB chunks

        var cacheBuffer = Data()
        var buffer = Data()

        for try await byte in asyncBytes {
            // Check for cancellation
            if loadingRequest.isCancelled {
                logw(message: "🟡 [ResourceLoader] Cancelled during remote streaming")

                // Save partial buffer to cache before cancelling
                cacheBuffer.append(buffer)
                if !cacheBuffer.isEmpty {
                    try? await storageManager.saveCacheSpan(
                        forKey: cacheKey,
                        data: cacheBuffer,
                        range: startOffset..<(startOffset + Int64(cacheBuffer.count))
                    )
                    logv(message: "      💾 Cached chunk: \(cacheBuffer.count) bytes")
                }

                throw CancellationError()
            }

            buffer.append(byte)

            // Send and cache chunk when buffer reaches chunk size
            if buffer.count >= chunkSize {
                dataRequest.respond(with: buffer)
                totalBytesSent += Int64(buffer.count)
                cacheBuffer.append(buffer)

                logv(message: "      ↗️ Sent chunk: \(buffer.count) bytes")

                buffer.removeAll(keepingCapacity: true)
            }
        }

        // Send and cache remaining data
        if !buffer.isEmpty {
            dataRequest.respond(with: buffer)
            totalBytesSent += Int64(buffer.count)
            cacheBuffer.append(buffer)
            logv(message: "      ↗️ Sent final chunk: \(buffer.count) bytes")
        }

        try? await storageManager.saveCacheSpan(
            forKey: cacheKey,
            data: cacheBuffer,
            range: startOffset..<(startOffset + Int64(cacheBuffer.count))
        )
    }

    // MARK: - Presigned URL Management

    private func getPresignedURL() async throws -> URL {
        // Return cached if available
        if let cached = presignedURL {
            return cached
        }

        // If fetch already in progress, await it
        if let existingTask = presignedURLTask {
            return try await existingTask.value
        }

        // Create new fetch task
        let fetchTask = Task<URL, Error> {
            // Fetch presigned URL
            let url = try await self.fetchPresignedURL(from: resourceURL)

            // Cache it
            self.presignedURL = url
            self.presignedURLTask = nil

            return url
        }

        presignedURLTask = fetchTask
        return try await fetchTask.value
    }

    private func fetchPresignedURL(from url: URL, isRetry: Bool = false) async throws -> URL {
        logi(message: "🔗 [ResourceLoader] Fetching presigned URL...")

        var request = URLRequest(url: url)
        request.httpMethod = "GET"

        let tokens = tokenProvider.getTokens()

        // Attach bearer token
        if let token = tokens?.second {
            request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        }

        let (_, response) = try await urlSession.data(for: request)

        guard let httpResponse = response as? HTTPURLResponse else {
            throw NSError(domain: "ResourceLoaderDelegate", code: -1, userInfo: nil)
        }

        // Handle 401 - refresh token and retry once
        if httpResponse.statusCode == 401 && !isRetry && tokens != nil {
            logw(message: "🔄 [ResourceLoader] Token expired, refreshing...")
            tokenProvider.refreshTokens(refreshToken: tokens!.first! as String)
            return try await fetchPresignedURL(from: url, isRetry: true)
        }

        // Handle 307 redirect to Cloudflare
        if httpResponse.statusCode == 307,
            let locationString = httpResponse.value(forHTTPHeaderField: "Location"),
            let presignedURL = URL(string: locationString)
        {
            logi(message: "✅ [ResourceLoader] Got presigned URL")
            return presignedURL
        }

        throw NSError(
            domain: "ResourceLoaderDelegate",
            code: httpResponse.statusCode,
            userInfo: [NSLocalizedDescriptionKey: "Failed to get presigned URL: \(httpResponse.statusCode)"]
        )
    }

    // MARK: - Cleanup

    func invalidate() {
        logi(message: "🧹 [ResourceLoader] Invalidating loader")

        queue.sync {
            for request in pendingRequests {
                request.finishLoading()
            }
            pendingRequests.removeAll()
        }

        presignedURL = nil
        presignedURLTask?.cancel()
        presignedURLTask = nil
        contentType = nil
        contentLength = nil
    }

    deinit {
        logi(message: "♻️ [ResourceLoader] Deallocating")
        invalidate()
    }

}
