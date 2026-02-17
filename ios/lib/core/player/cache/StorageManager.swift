//
//  StorageManager.swift
//  ios
//
//  Unified storage manager for downloads and cache with LRU eviction
//  Now uses 5MB chunked files for efficient caching
//

import CryptoKit
import Foundation
internal import Shared

/// Errors that can occur during storage operations
enum StorageError: Error {
    case downloadNotFound
    case storageReadFailed
    case cacheSizeLimitExceeded
    case fileOperationFailed
    case invalidRange
}

/// Manages unified storage (downloads and cache) with LRU eviction
class StorageManager {

    // MARK: - Properties

    private let metadataIndex: MetadataIndex = inject()
    private let cacheIndex: CacheIndex = inject(qualifier: named(name: PlaybackCacheScope.shared.ID))
    private let downloadIndex: CacheIndex = inject(qualifier: named(name: DownloadCacheScope.shared.ID))
    private let fileManager = FileManager.default

    let cacheDirectory: URL
    let downloadDirectory: URL

    // Limits
    private static let maxCacheSize: Int64 = 200 * 1024 * 1024  // 200 MB
    static let chunkSize: Int64 = 5 * 1024 * 1024  // 5 MB per chunk file

    // Queue for thread-safe operations
    private let queue = DispatchQueue(label: "com.app.storage", qos: .userInitiated)

    // MARK: - Initialization

    init() {
        self.cacheDirectory = fileManager.urls(
            for: .cachesDirectory,
            in: .userDomainMask
        ).first!.appendingPathComponent("audio", isDirectory: true)
        try? fileManager.createDirectory(at: cacheDirectory, withIntermediateDirectories: true)

        self.downloadDirectory = fileManager.urls(
            for: .documentDirectory,
            in: .userDomainMask
        ).first!.appendingPathComponent("audio", isDirectory: true)
        try? fileManager.createDirectory(at: downloadDirectory, withIntermediateDirectories: true)
    }

    // MARK: - Chunk File Helpers

    /// Calculate which chunk index a byte offset belongs to
    private func chunkIndex(for offset: Int64) -> Int {
        return Int(offset / StorageManager.chunkSize)
    }

    /// Get chunk file path for a given chunk index
    private func sanitizeKey(_ key: String) -> String {
        let hash = SHA256.hash(data: Data(key.utf8))
        let hashString = hash.compactMap { String(format: "%02x", $0) }.joined()
        return "audio_\(hashString)"
    }

    private func chunkFilePath(forKey key: String, chunkIndex: Int) -> URL {
        let safeKey = sanitizeKey(key)
        let fileName = "\(safeKey)_chunk_\(chunkIndex)"
        return cacheDirectory.appendingPathComponent(fileName)
    }

    /// Calculate offset within a chunk file
    private func offsetInChunk(for offset: Int64) -> Int64 {
        return offset % StorageManager.chunkSize
    }

    // MARK: - Content Information

    func saveMetadata(forKey key: String, contentLength: Int64, contentType: String) async {
        try? await metadataIndex.insert(metadata: Metadata(key: key, contentLength: contentLength, contentType: contentType))
    }

    /// Get content metadata (content-length and type) for a resource
    /// This can be retrieved even if the file is not fully downloaded
    func getMetadata(forKey key: String) async throws -> (contentLength: Int64, contentType: String)? {
        let metadata = try? await metadataIndex.get(key: key)
        if metadata == nil {
            return nil
        }
        return (metadata!.contentLength, metadata!.contentType)
    }

    // MARK: - Download Management

    /// Get complete download for a resource key
    func getDownload(forKey key: String) async throws -> CacheSpan? {
        let download = try await downloadIndex.get(key: key)

        // Touch to update LRU
        if download != nil {
            try? await downloadIndex.touch(span: download!)
        }

        return download
    }

    // MARK: - Cache Management

    /// Get stored ranges that intersect with the requested range
    func getSpans(
        forKey key: String,
        intersecting range: Range<Int64>
    ) async throws -> [CacheSpan] {
        let spans = try await cacheIndex.getRanges(key: key, from: range.lowerBound, to: range.upperBound - 1)

        return spans
    }

    func touchSpan(span: CacheSpan) async {
        try? await cacheIndex.touch(span: span)
    }

    /// Save data to cache/storage for a specific range using 5MB chunked files
    func saveCacheSpan(
        forKey key: String,
        data: Data,
        range: Range<Int64>
    ) async throws {
        guard !data.isEmpty else { return }

        let startOffset = range.lowerBound
        let endOffset = range.upperBound - 1  // Inclusive

        try await enforceCacheSizeLimit(additionalSize: Int64(data.count))

        // Calculate which chunks this range spans
        let startChunk = chunkIndex(for: startOffset)
        let endChunk = chunkIndex(for: endOffset)

        var dataOffset = 0

        // Write to each chunk file that this range touches
        for chunkIdx in startChunk...endChunk {
            let chunkStart = Int64(chunkIdx) * StorageManager.chunkSize
            let chunkEnd = chunkStart + StorageManager.chunkSize - 1

            // Calculate the portion of data that goes into this chunk
            let writeStart = max(startOffset, chunkStart)
            let writeEnd = min(endOffset, chunkEnd)
            let writeLength = Int(writeEnd - writeStart + 1)

            // Extract the data slice for this chunk
            let chunkData = data.subdata(in: dataOffset..<(dataOffset + writeLength))

            // Get chunk file path
            let chunkFileURL = chunkFilePath(forKey: key, chunkIndex: chunkIdx)

            // Write to chunk file at the appropriate offset
            try writeToChunkFile(
                at: chunkFileURL,
                data: chunkData,
                offset: offsetInChunk(for: writeStart)
            )

            // Create CacheSpan entry for this portion
            let span = CacheSpan(
                key: key,
                startOffset: writeStart,
                endOffset: writeEnd,
                filePath: chunkFileURL.lastPathComponent,
                lastAccessedAt: Int64(Date().timeIntervalSinceReferenceDate) * 1000
            )

            _ = try await cacheIndex.insert(span: span)

            dataOffset += writeLength
        }
    }

    /// Write data to a chunk file at a specific offset
    private func writeToChunkFile(at fileURL: URL, data: Data, offset: Int64) throws {
        // Create file if it doesn't exist
        if !fileManager.fileExists(atPath: fileURL.path()) {
            fileManager.createFile(atPath: fileURL.path(), contents: nil)
        }

        let fileHandle = try FileHandle(forWritingTo: fileURL)
        defer { try? fileHandle.close() }

        try fileHandle.seek(toOffset: UInt64(offset))
        try fileHandle.write(contentsOf: data)
    }

    // MARK: - Size Management

    /// Enforce cache size limit by evicting oldest entries
    private func enforceCacheSizeLimit(additionalSize: Int64) async throws {
        let currentSize = (try await cacheIndex.getTotalLength()).int64Value
        let projectedSize = currentSize + additionalSize

        guard projectedSize > StorageManager.maxCacheSize else {
            return  // Within limit
        }

        // Calculate how much we need to free
        let bytesToFree = projectedSize - StorageManager.maxCacheSize
        var freedBytes: Int64 = 0

        // Get oldest entries (LRU) - only cache segments, not complete downloads
        let oldestEntries = try await cacheIndex.getOldestCacheEntries(limit: 100)

        for entry in oldestEntries {
            guard freedBytes < bytesToFree else {
                break
            }

            // Delete file
            try? fileManager.removeItem(at: URL(filePath: entry.filePath))

            // Remove from index
            try await cacheIndex.delete(span: entry)

            freedBytes += entry.length
        }

        if freedBytes < bytesToFree {
            // Still over limit after evicting oldest entries
            // This might happen if a single new entry is too large
            print("⚠️ [Storage] Warning: Could not free enough space. Freed: \(freedBytes), Needed: \(bytesToFree)")
        }
    }
}
