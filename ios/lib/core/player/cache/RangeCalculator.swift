//
//  RangeCalculator.swift
//  ios
//
//  Calculates optimal data source strategy for byte range requests
//

import Foundation
internal import Shared

/// Represents a segment of data from a specific source
enum DataSource {
    case download(span: CacheSpan, offset: Int64, length: Int64)
    case cache(span: CacheSpan, offset: Int64, length: Int64)
    case remote(startOffset: Int64, endOffset: Int64)  // Inclusive end
}

struct DataSegment {
    let source: DataSource
    let requestedStart: Int64  // Offset in the original request
    let requestedEnd: Int64  // Inclusive

    var length: Int64 {
        return requestedEnd - requestedStart + 1
    }
}

/// Calculates the optimal strategy for fulfilling byte range requests

class RangeCalculator {

    /// Merge stored ranges and remote segments to cover the requested range
    static func mergeSegments(
        requestedRange: Range<Int64>,
        cacheSpans: [CacheSpan]
    ) -> [DataSegment] {

        let requestStart = requestedRange.lowerBound
        let requestEnd = requestedRange.upperBound - 1

        var segments: [DataSegment] = []
        var currentOffset = requestStart

        let cacheSpans = cacheSpans.sorted { $0.startOffset < $1.startOffset }

        for span in cacheSpans {
            guard span.startOffset <= requestEnd && span.endOffset >= currentOffset else {
                continue
            }

            // Fill gap with remote
            if currentOffset < span.startOffset {
                segments.append(
                    DataSegment(
                        source: .remote(
                            startOffset: currentOffset,
                            endOffset: span.startOffset - 1
                        ),
                        requestedStart: currentOffset,
                        requestedEnd: span.startOffset - 1
                    ))
                currentOffset = span.startOffset
            }

            // Add stored segment
            let segmentStart = max(currentOffset, span.startOffset)
            let segmentEnd = min(requestEnd, span.endOffset)

            let offsetInStoredFile = segmentStart % StorageManager.chunkSize

            let segmentLength = segmentEnd - segmentStart + 1

            segments.append(
                DataSegment(
                    source: .cache(
                        span: span,
                        offset: offsetInStoredFile,
                        length: segmentLength
                    ),
                    requestedStart: segmentStart,
                    requestedEnd: segmentEnd
                ))

            currentOffset = segmentEnd + 1

            if currentOffset > requestEnd {
                break
            }
        }

        // Fill remaining tail with remote
        if currentOffset <= requestEnd {
            segments.append(
                DataSegment(
                    source: .remote(
                        startOffset: currentOffset,
                        endOffset: requestEnd
                    ),
                    requestedStart: currentOffset,
                    requestedEnd: requestEnd
                ))
        }

        return segments
    }
}
