//
//  TimeRangeSelectorState 2.swift
//  ios
//
//  Created by Anas Erkinjonov on 14/02/26.
//

internal import Shared
import SwiftUI

@Observable
class TimeRangeSelectorState {
    var start: Int
    var end: Int
    var total: Int

    init(initialStart: Int, initialEnd: Int, total: Int) {
        self.start = initialStart
        self.end = initialEnd
        self.total = total
    }
}

struct TimeRangeSelector: View {
    @Bindable var state: TimeRangeSelectorState
    var color: Color
    var currentPosition: Int = -1

    @State private var sliderStart: CGFloat = 0
    @State private var sliderEnd: CGFloat = 0

    @State private var scrollStartId: Int?
    @State private var scrollEndId: Int?

    var body: some View {
        VStack(spacing: 8) {
            // Range Slider
            CustomRangeSlider(
                startValue: $sliderStart,
                endValue: $sliderEnd,
                bounds: 0...CGFloat(max(1, state.total / 60)),
                currentPosition: currentPosition,
                total: state.total
            )
            .onChange(of: sliderStart) { _, newValue in
                let newStart = Int(newValue * 60)
                if newStart != state.start {
                    state.start = min(newStart, state.end - 1)
                }
            }
            .onChange(of: sliderEnd) { _, newValue in
                let newEnd = Int(newValue * 60)
                if newEnd != state.end {
                    state.end = max(newEnd, state.start + 1)
                }
            }

            // Spinners
            HStack(spacing: 8) {
                TimeSpinner(
                    scrollId: $scrollStartId,
                    value: state.start,
                    otherValue: state.end,
                    total: state.total,
                    currentPosition: currentPosition,
                    title: Strings.shared.START.string(),
                    color: color,
                    isStart: true
                )

                TimeSpinner(
                    scrollId: $scrollEndId,
                    value: state.end,
                    otherValue: state.start,
                    total: state.total,
                    currentPosition: currentPosition,
                    title: Strings.shared.END.string(),
                    color: color,
                    isStart: false
                )
            }
        }
        .onAppear {
            scrollStartId = state.start
            scrollEndId = state.end
            sliderStart = CGFloat(state.start) / 60.0
            sliderEnd = CGFloat(state.end) / 60.0
        }
        // Sync scroll IDs to state with bounds checking (prevent overlapping items)
        .onChange(of: scrollStartId) { _, newValue in
            guard let newValue else { return }

            let maxAllowedStart = max(0, state.end - 10)

            if newValue > maxAllowedStart {
                // User scrolled too close to the end. Force the scroll view back!
                scrollStartId = maxAllowedStart
                state.start = maxAllowedStart
            } else {
                state.start = newValue
            }
        }
        .onChange(of: scrollEndId) { _, newValue in
            guard let newValue else { return }

            let minAllowedEnd = min(state.total, state.start + 10)

            if newValue < minAllowedEnd {
                // User scrolled too close to the start. Force the scroll view back!
                scrollEndId = minAllowedEnd
                state.end = minAllowedEnd
            } else {
                state.end = newValue
            }
        }
        // Sync state back to slider and scroll views
        .onChange(of: state.start) { _, newValue in
            if scrollStartId != newValue { scrollStartId = newValue }
            let minuteValue = CGFloat(newValue) / 60.0
            if sliderStart != minuteValue { sliderStart = minuteValue }
        }
        .onChange(of: state.end) { _, newValue in
            if scrollEndId != newValue { scrollEndId = newValue }
            let minuteValue = CGFloat(newValue) / 60.0
            if sliderEnd != minuteValue { sliderEnd = minuteValue }
        }
    }
}

// MARK: - Time Spinner

private struct TimeSpinner: View {
    @Binding var scrollId: Int?
    var value: Int
    var otherValue: Int
    var total: Int
    var currentPosition: Int
    var title: String
    var color: Color
    var isStart: Bool

    let itemWidth: CGFloat = 10.0

    var body: some View {
        GeometryReader { geom in
            let halfWidth = geom.size.width / 2

            VStack(alignment: .leading, spacing: 4) {
                Text(title)
                    .font(Typography.titleMedium)

                ZStack(alignment: .top) {
                    RoundedRectangle(cornerRadius: 8)
                        .stroke(Color.white, lineWidth: 1.5)
                        .background(Color.clear)

                    ZStack {
                        ScrollView(.horizontal, showsIndicators: false) {
                            LazyHStack(alignment: .bottom, spacing: 0) {
                                ForEach(0...total, id: \.self) { second in
                                    TickMark(second: second)
                                        .frame(width: itemWidth)
                                        .id(second)
                                }
                            }
                            .scrollTargetLayout()
                        }
                        .contentMargins(.horizontal, halfWidth - (itemWidth / 2), for: .scrollContent)
                        .scrollTargetBehavior(.viewAligned)
                        .scrollPosition(id: $scrollId)

                        SpinnerOverlay(
                            value: value,
                            otherValue: otherValue,
                            currentPosition: currentPosition,
                            isStart: isStart,
                            itemWidth: itemWidth
                        )
                    }
                    .mask(
                        HStack(spacing: 0) {
                            if isStart {
                                Color.black
                                LinearGradient(colors: [.black, .clear], startPoint: .leading, endPoint: .trailing)
                                    .frame(width: 32)
                            } else {
                                LinearGradient(colors: [.clear, .black], startPoint: .leading, endPoint: .trailing)
                                    .frame(width: 32)
                                Color.black
                            }
                        }
                    )

                    Text(value.formatTime())
                        .fontWeight(.semibold)
                        .padding(.top, 8)
                }
                .clipShape(RoundedRectangle(cornerRadius: 8))
            }
        }
        .frame(height: 84)
    }
}

// MARK: - Spinner Subcomponents

private struct SpinnerOverlay: View {
    var value: Int
    var otherValue: Int
    var currentPosition: Int
    var isStart: Bool
    var itemWidth: CGFloat

    var body: some View {
        GeometryReader { geom in
            let centerX = geom.size.width / 2
            let height = geom.size.height
            let bottomY = height - 7.5  // Aligns with bottom big tick center

            // Range highlight area
            let diff = CGFloat(otherValue - value) * itemWidth
            if isStart && diff > 0 {
                Rectangle()
                    .fill(.white.opacity(0.4))
                    .frame(width: diff, height: 15)
                    .position(x: centerX + diff / 2, y: bottomY)
            } else if !isStart && diff < 0 {
                let w = abs(diff)
                Rectangle()
                    .fill(.white.opacity(0.4))
                    .frame(width: w, height: 15)
                    .position(x: centerX - w / 2, y: bottomY)
            }

            // Current target value marker (Main Line)
            Rectangle()
                .fill(.white)
                .frame(width: 2, height: 15)
                .position(x: centerX, y: bottomY)

            // Other bounds line (if visible in view)
            let otherX = centerX + diff
            if otherX >= 0 && otherX <= geom.size.width {
                Rectangle()
                    .fill(.white)
                    .frame(width: 2, height: 15)
                    .position(x: otherX, y: bottomY)
            }

            // Red Current Position Marker
            if currentPosition != -1 {
                let currX = centerX + CGFloat(currentPosition - value) * itemWidth
                if currX >= 0 && currX <= geom.size.width {
                    Rectangle()
                        .fill(Color.red)
                        .frame(width: 2, height: 15)
                        .position(x: currX, y: bottomY)
                }
            }
        }
    }
}

private struct TickMark: View {
    let second: Int
    let isTen: Bool

    init(second: Int) {
        self.second = second
        self.isTen = second % 10 == 0
    }

    var body: some View {
        VStack(spacing: 0) {
            Spacer()

            if isTen {
                Text(second.formatTime())
                    .font(Typography.labelSmall)
                    .foregroundColor(.white.opacity(0.6))
                    .fixedSize()
                    .frame(height: 14)
            } else {
                Spacer().frame(height: 14)
            }

            Rectangle()
                .fill(Color.white)
                .frame(width: isTen ? 2 : 1, height: isTen ? 15 : 10)
        }
        .frame(height: 64)
    }
}

// MARK: - Custom Range Slider

private struct CustomRangeSlider: View {
    @Binding var startValue: CGFloat
    @Binding var endValue: CGFloat
    var bounds: ClosedRange<CGFloat>
    var currentPosition: Int
    var total: Int

    // Helper to convert minute value to X coordinate
    func xPosition(for value: CGFloat, range: CGFloat, width: CGFloat) -> CGFloat {
        ((value - bounds.lowerBound) / range) * width
    }

    // Helper to convert X coordinate to snapped minute value
    func snappedValue(for x: CGFloat, range: CGFloat, width: CGFloat) -> CGFloat {
        let raw = bounds.lowerBound + (x / width) * range
        return min(bounds.upperBound, max(bounds.lowerBound, raw.rounded()))
    }

    var body: some View {
        GeometryReader { geom in
            let width = geom.size.width
            let range = bounds.upperBound - bounds.lowerBound
            let minuteCount = Int(range)

            ZStack(alignment: .leading) {
                // 1. Inactive Track
                Capsule()
                    .fill(Color.white.opacity(0.15))
                    .frame(height: 16)

                // 2. Minute "Stops" (Visual Indicators)
                if minuteCount > 0 && minuteCount < 100 {  // Avoid overcrowding
                    HStack(spacing: 0) {
                        ForEach(0...minuteCount, id: \.self) { _ in
                            Circle()
                                .fill(Color.white.opacity(0.3))
                                .frame(width: 2, height: 2)
                            if minuteCount > 0 { Spacer(minLength: 0) }
                        }
                    }
                    .padding(.horizontal, 2)
                }

                // 3. Active Track
                Rectangle()
                    .fill(.white)
                    .frame(
                        width: max(0, xPosition(for: endValue, range: range, width: width) - xPosition(for: startValue, range: range, width: width)),
                        height: 16
                    )
                    .offset(x: xPosition(for: startValue, range: range, width: width))

                // 4. Playback Indicator
                if currentPosition != -1 && total > 0 {
                    Circle()
                        .fill(Color.red)
                        .frame(width: 4, height: 4)
                        .position(
                            x: (CGFloat(currentPosition) / CGFloat(total)) * width,
                            y: geom.size.height
                        )
                }

                // 5. Start Thumb
                ThumbView()
                    .position(x: xPosition(for: startValue, range: range, width: width), y: geom.size.height / 2)
                    .gesture(
                        DragGesture().onChanged { val in
                            let newMinute = snappedValue(for: val.location.x, range: range, width: width)
                            // Ensure start doesn't cross end (at least 1 min apart if desired)
                            startValue = min(newMinute, endValue)
                        }
                    )

                // 6. End Thumb
                ThumbView()
                    .position(x: xPosition(for: endValue, range: range, width: width), y: geom.size.height / 2)
                    .gesture(
                        DragGesture().onChanged { val in
                            let newMinute = snappedValue(for: val.location.x, range: range, width: width)
                            endValue = max(newMinute, startValue)
                        }
                    )
            }
        }
        .frame(height: 32)
    }
}

private struct ThumbView: View {
    var body: some View {
        ZStack {
            RoundedRectangle(cornerRadius: 2)
                .fill(Color.white)
                .frame(width: 4, height: 24)

            // Larger invisible hit area for better UX
            Color.black.opacity(0.001)
                .frame(width: 30, height: 40)
        }
    }
}

// MARK: - Preview

#Preview {
    @Previewable @State var state = TimeRangeSelectorState(initialStart: 100, initialEnd: 1000, total: 2000)

    PreviewRoot {
        ZStack {
            Color.black.ignoresSafeArea()

            TimeRangeSelector(
                state: state,
                color: Color.blue,  // MaterialTheme.colorScheme.secondary equivalent
                currentPosition: 5
            )
            .padding()
            .preferredColorScheme(.dark)
        }
    }
}
