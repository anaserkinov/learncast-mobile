//
//  IosPlayerController.swift
//  ios
//
//  Created by Anas Erkinjonov on 06/02/26.
//

import AVFoundation
import Combine
internal import Kingfisher
import MediaPlayer
internal import Shared

class AVPlayerDelegateImpl: AVPlayerDelegate {

    private var player = AVPlayer()
    private let session = AVAudioSession.sharedInstance()

    private var queue = [QueueItem]()
    private var playWhenReadyValue = true
    private var requestedSeekTime: CMTime? = nil
    private var currentIndex = -1
    private var lastDispatchedPlaybackState: PlaybackState? = nil
    private var lastDispatchedIsPlaying = false
    private var playInBackground = true
    private var isPaused = false

    private let defaultImage = UIImage(named: "MainLogo")!
    private let storageManager = StorageManager()
    private var resourceLoader: ResourceLoaderDelegate? = nil

    private var cancellables = Set<AnyCancellable>()
    private var timeControlCancellable: AnyCancellable? = nil
    private var interruptionCancellable: AnyCancellable? = nil

    private var timeObserverToken: Any? = nil
    private var callback: (any AVPlayerCallback)? = nil
    private var tokenProvider: (any TokenProvider)? = nil

    init() {
        player.actionAtItemEnd = .pause
        player.automaticallyWaitsToMinimizeStalling = false

        timeControlCancellable = player.publisher(for: \.timeControlStatus)
            .sink { timeControlStatus in
                let isPlaying = timeControlStatus == .playing
                if self.lastDispatchedIsPlaying != isPlaying {
                    self.isPaused = !isPlaying
                    self.callback?.onIsPlayingChanged(isPlaying: isPlaying)
                    self.lastDispatchedIsPlaying = isPlaying
                }

                self.dispatchUpdate(
                    queueItem: self.currentItem(),
                    playerItem: self.player.currentItem,
                    status: self.player.currentItem?.status,
                    timeControlStatus: timeControlStatus
                )
            }

        timeObserverToken = player.addPeriodicTimeObserver(
            forInterval: CMTime(seconds: 1, preferredTimescale: CMTimeScale(NSEC_PER_SEC)),
            queue: .main
        ) { time in
            MPNowPlayingInfoCenter.default().nowPlayingInfo?[MPNowPlayingInfoPropertyElapsedPlaybackTime] = time.seconds
        }

        interruptionCancellable = NotificationCenter.default.publisher(for: AVAudioSession.interruptionNotification)
            .sink { notification in
                guard let typeValue = notification.userInfo?[AVAudioSessionInterruptionTypeKey] as? UInt,
                    let type = AVAudioSession.InterruptionType(rawValue: typeValue)
                else { return }

                switch type {
                case .began:
                    // Phone call, Siri, another app took audio focus
                    self.pause()

                case .ended:
                    guard let optionsValue = notification.userInfo?[AVAudioSessionInterruptionOptionKey] as? UInt else { return }
                    let options = AVAudioSession.InterruptionOptions(rawValue: optionsValue)
                    if options.contains(.shouldResume) {
                        self.play()
                    }

                @unknown default:
                    break
                }
            }

        do {
            try session.setCategory(
                .playback,
                mode: .default,
                options: []
            )
            logi(message: "🔊 Audio session category set to playback")
        } catch {
            loge(message: "❌ Failed to set audio session category: \(error)")
        }

        let commandCenter = MPRemoteCommandCenter.shared()

        commandCenter.playCommand.isEnabled = true
        commandCenter.playCommand.addTarget { [weak self] _ in
            self?.play()
            return .success
        }

        commandCenter.pauseCommand.isEnabled = true
        commandCenter.pauseCommand.addTarget { [weak self] _ in
            self?.pause()
            return .success
        }

        commandCenter.skipBackwardCommand.isEnabled = true
        commandCenter.skipBackwardCommand.preferredIntervals = [10]
        commandCenter.skipBackwardCommand.addTarget { [weak self] _ in
            if let playerD = self {
                let time = playerD.player.currentTime().seconds
                if time != .infinity && time != .nan {
                    playerD.seekTo(positionMs: Int64((time - 10) * 1000))
                }
            }
            return .success
        }

        commandCenter.skipForwardCommand.isEnabled = true
        commandCenter.skipForwardCommand.preferredIntervals = [30]
        commandCenter.skipForwardCommand.addTarget { [weak self] _ in
            if let playerD = self {
                let time = playerD.player.currentTime().seconds
                if time != .infinity && time != .nan {
                    playerD.seekTo(positionMs: Int64((time + 30) * 1000))
                }
            }
            return .success
        }
    }

    func setCallback(callback: any AVPlayerCallback) {
        self.callback = callback
    }

    func setTokenProvider(provider: any TokenProvider) {
        self.tokenProvider = provider
    }

    func setPlayInBackground(value: Bool) {
        self.playInBackground = value
    }

    func isPlaying() -> Bool {
        player.timeControlStatus == .playing
    }

    func setPlayWhenReady(value: Bool) {
        playWhenReadyValue = value
    }

    func playWhenReady() -> Bool {
        playWhenReadyValue
    }

    func playbackState() -> PlaybackState {
        mapToPlaybackState(
            queueItem: currentItem(),
            playerItem: player.currentItem,
            status: player.currentItem?.status,
            timeControlStatus: player.timeControlStatus
        )
    }

    func add(index: Int32, item: QueueItem) {
        if currentIndex <= index {
            currentIndex += 1
        }
        queue.insert(item, at: Int(index))
    }

    func move(currentItem: QueueItem?, currentIndex: Int32, newIndex: Int32) {
        let removed = queue.remove(at: Int(currentIndex))
        if let currentItem = currentItem {
            queue.insert(currentItem, at: Int(newIndex))
        } else {
            queue.insert(removed, at: Int(newIndex))
        }
    }

    func seekTo(itemIndex: Int32, positionMs: Int64) {
        let oldItem = currentIndex == -1 ? nil : queue[currentIndex]

        seekToInternal(
            itemIndex: itemIndex,
            positionMs: positionMs,
            oldItem: oldItem
        )
    }

    func seekTo(positionMs: Int64) {
        if let item = currentItem(), let startTime = item.startMs {
            seekToInternal(positionMs: positionMs + startTime.int64Value)
        } else {
            seekToInternal(positionMs: positionMs)
        }
    }

    func play() {
        if let currentItem = currentItem() {
            guard player.status == .readyToPlay else {
                return
            }

            if updateNowPlayingInfo(item: currentItem) {
                isPaused = false
                player.play()
                MPNowPlayingInfoCenter.default().nowPlayingInfo?[MPNowPlayingInfoPropertyPlaybackRate] = 1.0
            }
        }
    }

    func pause() {
        isPaused = true
        player.pause()
        MPNowPlayingInfoCenter.default().nowPlayingInfo?[MPNowPlayingInfoPropertyPlaybackRate] = 0.0
    }

    func reload() {
        player.replaceCurrentItem(with: createAVPlayerItem(item: queue[currentIndex]))
        if let time = requestedSeekTime {
            player.seek(
                to: time, toleranceBefore: .zero, toleranceAfter: .zero,
                completionHandler: { finished in
                    if finished && self.playWhenReadyValue && !self.isPaused {
                        self.player.play()
                    }
                })
        }
    }

    func currentItem() -> QueueItem? {
        if currentIndex == -1 || currentIndex >= queue.count {
            nil
        } else {
            queue[currentIndex]
        }
    }

    func replace(index: Int32, item: QueueItem) {
        let oldItem = queue[Int(index)]
        queue[Int(index)] = item

        if index == currentIndex {
            let currentPosition = getCurrentPositionInternal()
            replaceItem(
                oldItem: oldItem,
                newItem: item,
                seekTo: currentPosition,
                reason: .other
            )
        }
    }

    func setItem(item: QueueItem, startPosition: Int64) {
        let oldItem = currentIndex == -1 ? nil : queue[currentIndex]
        queue = [item]
        seekToInternal(itemIndex: 0, positionMs: startPosition, oldItem: oldItem)
    }

    func setItems(items: [QueueItem], startIndex: Int32, startPosition: Int64) {
        let oldItem = currentIndex == -1 ? nil : queue[currentIndex]
        queue = items
        seekToInternal(itemIndex: startIndex, positionMs: startPosition, oldItem: oldItem)
    }

    func getCurrentPosition() -> Int64 {
        if let currentItem = currentItem() {
            let internalPosition = getCurrentPositionInternal()
            if currentItem.referenceType == ReferenceType.snip, let startTime = currentItem.startMs {
                return internalPosition - startTime.int64Value
            } else {
                return internalPosition
            }
        } else {
            return 0
        }
    }

    private func getCurrentPositionInternal() -> Int64 {
        let currentTime = player.currentTime()
        return currentTime == .invalid || currentTime.seconds == .infinity || currentTime.seconds == .nan ? 0 : Int64(currentTime.seconds * 1000)
    }

    func getCurrentItemIndex() -> Int32 {
        Int32(currentIndex)
    }

    func getDuration() -> Int64 {
        Int64((player.currentItem?.duration.seconds ?? 0) * 1000)
    }

    func getItemCount() -> Int32 {
        Int32(queue.count)
    }

    func seekToNextItem() {
        if currentIndex != queue.count - 1 {
            let oldItem = currentIndex == -1 ? nil : queue[currentIndex]
            currentIndex += 1
            let newItem = queue[currentIndex]
            replaceItem(
                oldItem: oldItem,
                newItem: newItem,
                seekTo: newItem.lastPositionMs,
                reason: .other
            )
        }
    }

    func removeItem(index: Int32) {
        let oldItem = queue.remove(at: Int(index))
        let currentRemoved = currentIndex == index
        if index <= currentIndex {
            currentIndex -= 1
        }
        if currentRemoved {
            let newItem = queue.isEmpty ? nil : currentIndex >= queue.count ? queue[queue.count - 1] : currentIndex == -1 ? queue[0] : queue[currentIndex]
            replaceItem(
                oldItem: oldItem,
                newItem: newItem,
                seekTo: newItem?.lastPositionMs ?? 0,
                reason: .other
            )
        }
    }

    func removeItems(from: Int32, to: Int32) {
        let oldItem = queue[currentIndex]
        queue.removeSubrange(Int(from)...Int(to))
        if queue.isEmpty {
            replaceItem(
                oldItem: oldItem,
                newItem: nil,
                seekTo: 0,
                reason: .other
            )
        }
    }

    func release() {
        cancellables.removeAll()
        player.pause()
        if let token = timeObserverToken {
            player.removeTimeObserver(token)
            timeObserverToken = nil
        }
        player.replaceCurrentItem(with: nil)
        callback = nil
        timeControlCancellable?.cancel()
        interruptionCancellable?.cancel()
    }

    private func replaceItem(
        oldItem: QueueItem?,
        newItem: QueueItem?,
        seekTo positionMs: Int64,
        reason: ItemTransitionReason
    ) {
        isPaused = false
        cancellables.removeAll()
        let currentPostionMs = getCurrentPosition()
        if let item = newItem {
            if playWhenReadyValue {
                let _ = updateNowPlayingInfo(item: item)
            }

            let avPlayerItem = createAVPlayerItem(item: item)
            addItemObservers(queueItem: item, playerItem: avPlayerItem)
            player.replaceCurrentItem(with: avPlayerItem)

            if let start = item.startMs {
                seekToInternal(positionMs: start.int64Value)
            } else {
                seekToInternal(positionMs: positionMs)
            }
        } else {
            player.pause()
            player.replaceCurrentItem(with: nil)
            MPNowPlayingInfoCenter.default().nowPlayingInfo = nil
        }
        callback?.onItemTransition(
            oldItem: oldItem,
            oldPositionMs: currentPostionMs,
            newItem: newItem,
            reason: reason
        )
    }

    private func addItemObservers(queueItem: QueueItem, playerItem: AVPlayerItem) {
        playerItem.publisher(for: \.status)
            .sink { status in
                let state = self.mapToPlaybackState(
                    queueItem: queueItem,
                    playerItem: playerItem,
                    status: status,
                    timeControlStatus: self.player.timeControlStatus
                )
                if self.playWhenReadyValue && state == PlaybackState.stateReady && self.player.timeControlStatus != .playing {
                    self.play()
                } else {
                    self.dispatchUpdate(
                        queueItem: queueItem,
                        playerItem: playerItem,
                        status: status,
                        timeControlStatus: self.player.timeControlStatus
                    )
                }
            }.store(in: &cancellables)

        NotificationCenter.default.publisher(for: AVPlayerItem.didPlayToEndTimeNotification, object: playerItem)
            .sink { notification in
                if self.currentIndex == self.queue.count - 1 {
                    self.dispatchUpdate(
                        queueItem: queueItem,
                        playerItem: playerItem,
                        status: playerItem.status,
                        timeControlStatus: self.player.timeControlStatus
                    )
                } else {
                    let oldItem = self.queue[self.currentIndex]
                    self.currentIndex += 1
                    let newItem = self.queue[self.currentIndex]
                    self.replaceItem(
                        oldItem: oldItem,
                        newItem: newItem,
                        seekTo: newItem.lastPositionMs,
                        reason: .auto
                    )
                }
            }.store(in: &cancellables)
    }

    private func seekToInternal(itemIndex: Int32, positionMs: Int64, oldItem: QueueItem?) {
        currentIndex = Int(itemIndex)
        replaceItem(
            oldItem: oldItem,
            newItem: queue[currentIndex],
            seekTo: positionMs,
            reason: .other
        )
    }

    private func seekToInternal(positionMs: Int64) {
        let seconds = Double(positionMs) / 1000.0
        let time = CMTime(seconds: seconds, preferredTimescale: 1000)
        requestedSeekTime = time
        player.seek(to: time, toleranceBefore: .zero, toleranceAfter: .zero) { finished in
            if finished {
                MPNowPlayingInfoCenter.default().nowPlayingInfo?[MPNowPlayingInfoPropertyElapsedPlaybackTime] = time.seconds

                if self.playWhenReadyValue && !self.isPaused {
                    self.player.play()
                }
            }
        }
    }

    private func createAVPlayerItem(item: QueueItem) -> AVPlayerItem {
        let originalUrl = URL(string: item.audioPath.normalizeUrl())!
        var components = URLComponents(url: originalUrl, resolvingAgainstBaseURL: false)!
        components.scheme = "learncast"

        let asset = AVURLAsset(url: components.url!)

        resourceLoader?.invalidate()
        resourceLoader = ResourceLoaderDelegate(
            storageManager: storageManager,
            tokenProvider: tokenProvider!,
            resourceURL: originalUrl,
            cacheKey: item.audioPath
        )

        asset.resourceLoader.setDelegate(resourceLoader, queue: .main)

        let playerItem = AVPlayerItem(asset: asset)
        if let start = item.startMs, let end = item.endMs {
            playerItem.forwardPlaybackEndTime = CMTime(seconds: Double(end.int64Value) / 1000, preferredTimescale: 1000)
            playerItem.reversePlaybackEndTime = CMTime(seconds: Double(start.int64Value) / 1000, preferredTimescale: 1000)
        }
        return playerItem
    }

    private func updateNowPlayingInfo(item: QueueItem) -> Bool {
        do {
            try session.setActive(true)

            if !playInBackground {
                return true
            }

            var info: [String: Any] = [:]

            info[MPMediaItemPropertyTitle] = item.title
            info[MPMediaItemPropertyArtist] = item.subTitle
            info[MPMediaItemPropertyPlaybackDuration] = TimeInterval(item.duration.millis()) / 1000
            info[MPNowPlayingInfoPropertyPlaybackRate] = 1.0

            info[MPMediaItemPropertyArtwork] = MPMediaItemArtwork(
                boundsSize: defaultImage.size,
                requestHandler: { size in
                    self.defaultImage
                })

            MPNowPlayingInfoCenter.default().nowPlayingInfo = info

            if let coverImagePath = item.coverImagePath {
                loadArtwork(url: coverImagePath.normalizeUrl()) { image in
                    let currentItem = self.currentIndex == -1 || self.currentIndex >= self.queue.count ? nil : self.queue[self.currentIndex]
                    if currentItem?.id == item.id, let image = image {
                        MPNowPlayingInfoCenter.default().nowPlayingInfo?[MPMediaItemPropertyArtwork] = MPMediaItemArtwork(boundsSize: image.size) { _ in image }
                    }
                }
            }

            return true
        } catch {
            return false
        }
    }

    private func dispatchUpdate(
        queueItem: QueueItem?,
        playerItem: AVPlayerItem?,
        status: AVPlayerItem.Status?,
        timeControlStatus: AVPlayer.TimeControlStatus
    ) {
        let state = mapToPlaybackState(
            queueItem: queueItem,
            playerItem: playerItem,
            status: status,
            timeControlStatus: timeControlStatus
        )
        if state != self.lastDispatchedPlaybackState {
            self.callback?.onPlaybackStateChanged(state: state)
            self.lastDispatchedPlaybackState = state
        }
    }

    private func mapToPlaybackState(
        queueItem: QueueItem?,
        playerItem: AVPlayerItem?,
        status: AVPlayerItem.Status?,
        timeControlStatus: AVPlayer.TimeControlStatus
    ) -> PlaybackState {
        guard let item = playerItem else {
            return .stateIdle
        }

        // 1. If AVPlayerItem failed
        if status == .failed {
            return .stateIdle
        }

        // 2. If playback ended
        if item.status == .readyToPlay && item.duration != .indefinite && queueItem != nil {
            let currentTime = Int64(item.currentTime().seconds * 1000)
            if currentTime >= queueItem!.duration.millis() {
                return .stateEnded
            }
        }

        // 3. Buffering
        if timeControlStatus == .waitingToPlayAtSpecifiedRate {
            return .stateBuffering
        }

        // 4. Ready to play
        if status == .readyToPlay {
            return .stateReady
        }

        // fallback
        return .stateIdle
    }

    private func loadArtwork(
        url: String,
        completion: @escaping (UIImage?) -> Void
    ) {
        KingfisherManager.shared.retrieveImage(with: URL(string: url)!) { result in
            switch result {
            case .success(let value):
                completion(value.image)

            case .failure:
                completion(nil)
            }
        }
    }

}
