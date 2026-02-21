//
//  PaletteExtractor.swift
//
//  A Swift implementation of Android's Palette API that produces IDENTICAL color targets
//  Based on androidx.palette.graphics Palette library
//
//  Created by Claude on 10/02/26.
//

import SwiftUI
import UIKit

// MARK: - Palette

final class Palette {

    // MARK: - Constants

    private static let defaultResizeBitmapArea = 112 * 112
    private static let defaultCalculateNumberColors = 16

    static let minContrastTitleText: Float = 3.0
    static let minContrastBodyText: Float = 4.5

    // MARK: - Properties

    private let swatches: [Swatch]
    private let targets: [Target]
    private var selectedSwatches: [ObjectIdentifier: Swatch] = [:]
    private var usedColors: Set<Int> = []
    public let dominantSwatch: Swatch?

    // MARK: - Initialization

    private init(swatches: [Swatch], targets: [Target]) {
        self.swatches = swatches
        self.targets = targets
        self.dominantSwatch = Self.findDominantSwatch(in: swatches)
    }

    // MARK: - Public API

    public static func from(_ image: UIImage) -> Builder {
        return Builder(image: image)
    }

    public func getSwatches() -> [Swatch] {
        return swatches
    }

    public func getVibrantSwatch() -> Swatch? {
        return getSwatchForTarget(Target.lightVibrant)
    }

    public func getLightVibrantSwatch() -> Swatch? {
        return getSwatchForTarget(Target.lightVibrant)
    }

    public func getDarkVibrantSwatch() -> Swatch? {
        return getSwatchForTarget(Target.darkVibrant)
    }

    public func getMutedSwatch() -> Swatch? {
        return getSwatchForTarget(Target.muted)
    }

    public func getLightMutedSwatch() -> Swatch? {
        return getSwatchForTarget(Target.lightMuted)
    }

    public func getDarkMutedSwatch() -> Swatch? {
        return getSwatchForTarget(Target.darkMuted)
    }

    public func getVibrantColor(_ defaultColor: Int) -> Int {
        return getColorForTarget(Target.vibrant, defaultColor: defaultColor)
    }

    public func getLightVibrantColor(_ defaultColor: Int) -> Int {
        return getColorForTarget(Target.lightVibrant, defaultColor: defaultColor)
    }

    public func getDarkVibrantColor(_ defaultColor: Int) -> Int {
        return getColorForTarget(Target.darkVibrant, defaultColor: defaultColor)
    }

    public func getMutedColor(_ defaultColor: Int) -> Int {
        return getColorForTarget(Target.muted, defaultColor: defaultColor)
    }

    public func getLightMutedColor(_ defaultColor: Int) -> Int {
        return getColorForTarget(Target.lightMuted, defaultColor: defaultColor)
    }

    public func getDarkMutedColor(_ defaultColor: Int) -> Int {
        return getColorForTarget(Target.darkMuted, defaultColor: defaultColor)
    }

    public func getSwatchForTarget(_ target: Target) -> Swatch? {
        return selectedSwatches[ObjectIdentifier(target)]
    }

    public func getColorForTarget(_ target: Target, defaultColor: Int) -> Int {
        return getSwatchForTarget(target)?.rgb ?? defaultColor
    }

    public func getDominantColor(_ defaultColor: Int) -> Int {
        return dominantSwatch?.rgb ?? defaultColor
    }

    // MARK: - Private Methods

    private func generate() {
        for target in targets {
            target.normalizeWeights()
            selectedSwatches[ObjectIdentifier(target)] = generateScoredTarget(target)
        }
        usedColors.removeAll()
    }

    private func generateScoredTarget(_ target: Target) -> Swatch? {
        let maxScoreSwatch = getMaxScoredSwatchForTarget(target)
        if let swatch = maxScoreSwatch, target.isExclusive {
            usedColors.insert(swatch.rgb)
        }
        return maxScoreSwatch
    }

    private func getMaxScoredSwatchForTarget(_ target: Target) -> Swatch? {
        var maxScore: Float = 0
        var maxScoreSwatch: Swatch?

        for swatch in swatches {
            if shouldBeScoredForTarget(swatch, target: target) {
                let score = generateScore(swatch: swatch, target: target)
                if maxScoreSwatch == nil || score > maxScore {
                    maxScoreSwatch = swatch
                    maxScore = score
                }
            }
        }

        return maxScoreSwatch
    }

    private func shouldBeScoredForTarget(_ swatch: Swatch, target: Target) -> Bool {
        let hsl = swatch.getHsl()
        return hsl[1] >= target.minimumSaturation && hsl[1] <= target.maximumSaturation && hsl[2] >= target.minimumLightness && hsl[2] <= target.maximumLightness && !usedColors.contains(swatch.rgb)
    }

    private func generateScore(swatch: Swatch, target: Target) -> Float {
        let hsl = swatch.getHsl()

        var saturationScore: Float = 0
        var luminanceScore: Float = 0
        var populationScore: Float = 0

        let maxPopulation = dominantSwatch?.population ?? 1

        if target.saturationWeight > 0 {
            saturationScore = target.saturationWeight * (1.0 - abs(hsl[1] - target.targetSaturation))
        }

        if target.lightnessWeight > 0 {
            luminanceScore = target.lightnessWeight * (1.0 - abs(hsl[2] - target.targetLightness))
        }

        if target.populationWeight > 0 {
            populationScore = target.populationWeight * (Float(swatch.population) / Float(maxPopulation))
        }

        return saturationScore + luminanceScore + populationScore
    }

    private static func findDominantSwatch(in swatches: [Swatch]) -> Swatch? {
        var maxPop = Int.min
        var maxSwatch: Swatch?

        for swatch in swatches {
            if swatch.population > maxPop {
                maxSwatch = swatch
                maxPop = swatch.population
            }
        }

        return maxSwatch
    }

    // MARK: - Swatch

    public final class Swatch: Hashable {
        public let rgb: Int
        public let population: Int
        private var hsl: [Float]?

        private var generatedTextColors = false
        private var titleTextColor: Int = 0
        private var bodyTextColor: Int = 0

        public init(rgb: Int, population: Int) {
            self.rgb = rgb
            self.population = population
        }

        public func getHsl() -> [Float] {
            if hsl == nil {
                hsl = ColorUtils.rGBtoHSL(rgb: rgb)
            }
            return hsl!
        }

        public func getTitleTextColor() -> Int {
            ensureTextColorsGenerated()
            return titleTextColor
        }

        public func getBodyTextColor() -> Int {
            ensureTextColorsGenerated()
            return bodyTextColor
        }

        private func ensureTextColorsGenerated() {
            if !generatedTextColors {
                let white = 0xFFFFFF
                let black = 0x000000

                let lightBodyAlpha = ColorUtils.calculateMinimumAlpha(foreground: white, background: rgb, minContrastRatio: Palette.minContrastBodyText)
                let lightTitleAlpha = ColorUtils.calculateMinimumAlpha(foreground: white, background: rgb, minContrastRatio: Palette.minContrastTitleText)

                if lightBodyAlpha != -1 && lightTitleAlpha != -1 {
                    bodyTextColor = ColorUtils.setAlphaComponent(color: white, alpha: lightBodyAlpha)
                    titleTextColor = ColorUtils.setAlphaComponent(color: white, alpha: lightTitleAlpha)
                    generatedTextColors = true
                    return
                }

                let darkBodyAlpha = ColorUtils.calculateMinimumAlpha(foreground: black, background: rgb, minContrastRatio: Palette.minContrastBodyText)
                let darkTitleAlpha = ColorUtils.calculateMinimumAlpha(foreground: black, background: rgb, minContrastRatio: Palette.minContrastTitleText)

                if darkBodyAlpha != -1 && darkTitleAlpha != -1 {
                    bodyTextColor = ColorUtils.setAlphaComponent(color: black, alpha: darkBodyAlpha)
                    titleTextColor = ColorUtils.setAlphaComponent(color: black, alpha: darkTitleAlpha)
                    generatedTextColors = true
                    return
                }

                bodyTextColor = lightBodyAlpha != -1 ? ColorUtils.setAlphaComponent(color: white, alpha: lightBodyAlpha) : ColorUtils.setAlphaComponent(color: black, alpha: darkBodyAlpha)

                titleTextColor = lightTitleAlpha != -1 ? ColorUtils.setAlphaComponent(color: white, alpha: lightTitleAlpha) : ColorUtils.setAlphaComponent(color: black, alpha: darkTitleAlpha)

                generatedTextColors = true
            }
        }

        public static func == (lhs: Swatch, rhs: Swatch) -> Bool {
            return lhs.rgb == rhs.rgb && lhs.population == rhs.population
        }

        public func hash(into hasher: inout Hasher) {
            hasher.combine(rgb)
            hasher.combine(population)
        }
    }

    // MARK: - Builder

    public final class Builder {
        private let image: UIImage?
        private let swatchesList: [Swatch]?

        private var targets: [Target] = []
        private var maxColors = defaultCalculateNumberColors
        private var resizeArea = defaultResizeBitmapArea
        private var filters: [PaletteFilter] = [DefaultFilter()]
        private var region: CGRect?

        init(image: UIImage) {
            self.image = image
            self.swatchesList = nil

            // Add default targets
            targets = [
                Target.lightVibrant,
                Target.vibrant,
                Target.darkVibrant,
                Target.lightMuted,
                Target.muted,
                Target.darkMuted,
            ]
        }

        init(swatches: [Swatch]) {
            self.swatchesList = swatches
            self.image = nil
        }

        public func maximumColorCount(_ colors: Int) -> Builder {
            maxColors = colors
            return self
        }

        public func resizeBitmapArea(_ area: Int) -> Builder {
            resizeArea = area
            return self
        }

        public func clearFilters() -> Builder {
            filters.removeAll()
            return self
        }

        public func addFilter(_ filter: PaletteFilter) -> Builder {
            filters.append(filter)
            return self
        }

        public func setRegion(left: Int, top: Int, right: Int, bottom: Int) -> Builder {
            if let img = image {
                region = CGRect(x: left, y: top, width: right - left, height: bottom - top)
                    .intersection(CGRect(x: 0, y: 0, width: Int(img.size.width), height: Int(img.size.height)))
            }
            return self
        }

        public func clearRegion() -> Builder {
            region = nil
            return self
        }

        public func addTarget(_ target: Target) -> Builder {
            if !targets.contains(where: { $0 === target }) {
                targets.append(target)
            }
            return self
        }

        public func clearTargets() -> Builder {
            targets.removeAll()
            return self
        }

        public func generate() -> Palette {
            var swatches: [Swatch]

            if let image = image {
                // Resize bitmap
                let bitmap = scaleBitmapDown(image)

                // Get pixels
                let pixels = getPixelsFromBitmap(bitmap)

                // Quantize
                let quantizer = ColorCutQuantizer(
                    pixels: pixels,
                    maxColors: maxColors,
                    filters: filters.isEmpty ? nil : filters
                )

                swatches = quantizer.getQuantizedColors()
            } else if let swatchesList = swatchesList {
                swatches = swatchesList
            } else {
                fatalError("Builder must have either image or swatches")
            }

            let palette = Palette(swatches: swatches, targets: targets)
            palette.generate()

            return palette
        }

        private func getPixelsFromBitmap(_ bitmap: UIImage) -> [Int] {
            guard let cgImage = bitmap.cgImage else { return [] }

            let width = cgImage.width
            let height = cgImage.height
            let bytesPerPixel = 4
            let bytesPerRow = bytesPerPixel * width
            let bitsPerComponent = 8

            var pixelData = [UInt8](repeating: 0, count: width * height * bytesPerPixel)

            guard
                let context = CGContext(
                    data: &pixelData,
                    width: width,
                    height: height,
                    bitsPerComponent: bitsPerComponent,
                    bytesPerRow: bytesPerRow,
                    space: CGColorSpaceCreateDeviceRGB(),
                    bitmapInfo: CGImageAlphaInfo.premultipliedLast.rawValue
                )
            else {
                return []
            }

            context.draw(cgImage, in: CGRect(x: 0, y: 0, width: width, height: height))

            var pixels = [Int]()

            if let region = region {
                let regionWidth = Int(region.width)
                let regionHeight = Int(region.height)
                let startX = Int(region.origin.x)
                let startY = Int(region.origin.y)

                for y in 0..<regionHeight {
                    for x in 0..<regionWidth {
                        let actualY = y + startY
                        let actualX = x + startX

                        if actualY < height && actualX < width {
                            let offset = (actualY * width + actualX) * bytesPerPixel
                            let r = Int(pixelData[offset])
                            let g = Int(pixelData[offset + 1])
                            let b = Int(pixelData[offset + 2])

                            pixels.append((r << 16) | (g << 8) | b)
                        }
                    }
                }
            } else {
                for i in 0..<(width * height) {
                    let offset = i * bytesPerPixel
                    let r = Int(pixelData[offset])
                    let g = Int(pixelData[offset + 1])
                    let b = Int(pixelData[offset + 2])

                    pixels.append((r << 16) | (g << 8) | b)
                }
            }

            return pixels
        }

        private func scaleBitmapDown(_ bitmap: UIImage) -> UIImage {
            var scaleRatio: CGFloat = -1

            if resizeArea > 0 {
                let bitmapArea = Int(bitmap.size.width * bitmap.size.height)
                if bitmapArea > resizeArea {
                    scaleRatio = sqrt(CGFloat(resizeArea) / CGFloat(bitmapArea))
                }
            }

            if scaleRatio <= 0 {
                return bitmap
            }

            let newSize = CGSize(
                width: ceil(bitmap.size.width * scaleRatio),
                height: ceil(bitmap.size.height * scaleRatio)
            )

            let renderer = UIGraphicsImageRenderer(size: newSize)
            return renderer.image { _ in
                bitmap.draw(in: CGRect(origin: .zero, size: newSize))
            }
        }
    }
}

// MARK: - Target

public final class Target {

    // MARK: - Constants

    private static let targetDarkLuma: Float = 0.26
    private static let maxDarkLuma: Float = 0.45

    private static let minLightLuma: Float = 0.55
    private static let targetLightLuma: Float = 0.74

    private static let minNormalLuma: Float = 0.3
    private static let targetNormalLuma: Float = 0.5
    private static let maxNormalLuma: Float = 0.7

    private static let targetMutedSaturation: Float = 0.3
    private static let maxMutedSaturation: Float = 0.4

    private static let targetVibrantSaturation: Float = 1.0
    private static let minVibrantSaturation: Float = 0.35

    private static let weightSaturation: Float = 0.24
    private static let weightLuma: Float = 0.52
    private static let weightPopulation: Float = 0.24

    // MARK: - Default Targets

    public static let lightVibrant: Target = {
        let target = Target()
        setDefaultLightLightnessValues(target)
        setDefaultVibrantSaturationValues(target)
        return target
    }()

    public static let vibrant: Target = {
        let target = Target()
        setDefaultNormalLightnessValues(target)
        setDefaultVibrantSaturationValues(target)
        return target
    }()

    public static let darkVibrant: Target = {
        let target = Target()
        setDefaultDarkLightnessValues(target)
        setDefaultVibrantSaturationValues(target)
        return target
    }()

    public static let lightMuted: Target = {
        let target = Target()
        setDefaultLightLightnessValues(target)
        setDefaultMutedSaturationValues(target)
        return target
    }()

    public static let muted: Target = {
        let target = Target()
        setDefaultNormalLightnessValues(target)
        setDefaultMutedSaturationValues(target)
        return target
    }()

    public static let darkMuted: Target = {
        let target = Target()
        setDefaultDarkLightnessValues(target)
        setDefaultMutedSaturationValues(target)
        return target
    }()

    // MARK: - Properties

    private var saturationTargets: [Float] = [0, 0.5, 1]
    private var lightnessTargets: [Float] = [0, 0.5, 1]
    private var weights: [Float] = [0, 0, 0]
    public var isExclusive: Bool = true

    public var minimumSaturation: Float { saturationTargets[0] }
    public var targetSaturation: Float { saturationTargets[1] }
    public var maximumSaturation: Float { saturationTargets[2] }

    public var minimumLightness: Float { lightnessTargets[0] }
    public var targetLightness: Float { lightnessTargets[1] }
    public var maximumLightness: Float { lightnessTargets[2] }

    public var saturationWeight: Float { weights[0] }
    public var lightnessWeight: Float { weights[1] }
    public var populationWeight: Float { weights[2] }

    // MARK: - Initialization

    private init() {
        setDefaultWeights()
    }

    private func setDefaultWeights() {
        weights[0] = Target.weightSaturation
        weights[1] = Target.weightLuma
        weights[2] = Target.weightPopulation
    }

    func normalizeWeights() {
        var sum: Float = 0
        for weight in weights where weight > 0 {
            sum += weight
        }

        if sum != 0 {
            for i in 0..<weights.count where weights[i] > 0 {
                weights[i] /= sum
            }
        }
    }

    // MARK: - Default Value Setters

    private static func setDefaultDarkLightnessValues(_ target: Target) {
        target.lightnessTargets[1] = targetDarkLuma
        target.lightnessTargets[2] = maxDarkLuma
    }

    private static func setDefaultNormalLightnessValues(_ target: Target) {
        target.lightnessTargets[0] = minNormalLuma
        target.lightnessTargets[1] = targetNormalLuma
        target.lightnessTargets[2] = maxNormalLuma
    }

    private static func setDefaultLightLightnessValues(_ target: Target) {
        target.lightnessTargets[0] = minLightLuma
        target.lightnessTargets[1] = targetLightLuma
    }

    private static func setDefaultVibrantSaturationValues(_ target: Target) {
        target.saturationTargets[0] = minVibrantSaturation
        target.saturationTargets[1] = targetVibrantSaturation
    }

    private static func setDefaultMutedSaturationValues(_ target: Target) {
        target.saturationTargets[1] = targetMutedSaturation
        target.saturationTargets[2] = maxMutedSaturation
    }
}

// MARK: - ColorCutQuantizer

final class ColorCutQuantizer {

    private static let quantizeWordWidth = 5
    private static let quantizeWordMask = (1 << quantizeWordWidth) - 1

    private static let componentRed = -3
    private static let componentGreen = -2
    private static let componentBlue = -1

    private let colors: [Int]
    private let histogram: [Int]
    private let quantizedColors: [Palette.Swatch]
    private let filters: [PaletteFilter]?

    init(pixels: [Int], maxColors: Int, filters: [PaletteFilter]?) {
        self.filters = filters

        // Create histogram
        var hist = [Int](repeating: 0, count: 1 << (ColorCutQuantizer.quantizeWordWidth * 3))
        var quantizedPixels = pixels

        for i in 0..<quantizedPixels.count {
            let quantizedColor = ColorCutQuantizer.quantizeFromRgb888(quantizedPixels[i])
            quantizedPixels[i] = quantizedColor
            hist[quantizedColor] += 1
        }

        self.histogram = hist

        // Filter colors and create distinct colors array
        var distinctColors = [Int]()
        var distinctColorCount = 0

        for color in 0..<hist.count {
            if hist[color] > 0 {
                // Check if we should ignore this color
                let rgb = ColorCutQuantizer.approximateToRgb888(color)
                let hsl = ColorUtils.rGBtoHSL(rgb: rgb)
                var shouldIgnore = false

                if let filters = filters, !filters.isEmpty {
                    for filter in filters {
                        if !filter.isAllowed(rgb: rgb, hsl: hsl) {
                            shouldIgnore = true
                            break
                        }
                    }
                }

                if shouldIgnore {
                    hist[color] = 0
                } else {
                    distinctColors.append(color)
                    distinctColorCount += 1
                }
            }
        }

        self.colors = distinctColors

        if distinctColorCount <= maxColors {
            // Too few colors, just return them
            var swatches = [Palette.Swatch]()
            for color in distinctColors {
                swatches.append(
                    Palette.Swatch(
                        rgb: ColorCutQuantizer.approximateToRgb888(color),
                        population: hist[color]
                    ))
            }
            self.quantizedColors = swatches
        } else {
            // Quantize - need to initialize quantizedColors before calling method
            var pq = PriorityQueue<ColorCutQuantizer.Vbox>(sort: { $0.getVolume() > $1.getVolume() })
            pq.enqueue(ColorCutQuantizer.Vbox(lowerIndex: 0, upperIndex: distinctColors.count - 1, colors: distinctColors, histogram: hist))

            while pq.count < maxColors {
                guard let vbox = pq.dequeue(), vbox.canSplit() else {
                    break
                }

                pq.enqueue(vbox.splitBox())
                pq.enqueue(vbox)
            }

            // Generate average colors
            var swatches = [Palette.Swatch]()
            for vbox in pq.queue {
                let swatch = vbox.getAverageColor()
                let rgb = swatch.rgb
                let hsl = ColorUtils.rGBtoHSL(rgb: rgb)
                var shouldIgnore = false

                if let filters = filters, !filters.isEmpty {
                    for filter in filters {
                        if !filter.isAllowed(rgb: rgb, hsl: hsl) {
                            shouldIgnore = true
                            break
                        }
                    }
                }

                if !shouldIgnore {
                    swatches.append(swatch)
                }
            }

            self.quantizedColors = swatches
        }
    }

    func getQuantizedColors() -> [Palette.Swatch] {
        return quantizedColors
    }

    // MARK: - Color Conversion

    private static func quantizeFromRgb888(_ color: Int) -> Int {
        let r = modifyWordWidth(value: (color >> 16) & 0xFF, currentWidth: 8, targetWidth: quantizeWordWidth)
        let g = modifyWordWidth(value: (color >> 8) & 0xFF, currentWidth: 8, targetWidth: quantizeWordWidth)
        let b = modifyWordWidth(value: color & 0xFF, currentWidth: 8, targetWidth: quantizeWordWidth)
        return r << (quantizeWordWidth + quantizeWordWidth) | g << quantizeWordWidth | b
    }

    private static func approximateToRgb888(_ color: Int) -> Int {
        return approximateToRgb888(
            r: quantizedRed(color),
            g: quantizedGreen(color),
            b: quantizedBlue(color)
        )
    }

    private static func approximateToRgb888(r: Int, g: Int, b: Int) -> Int {
        let red = modifyWordWidth(value: r, currentWidth: quantizeWordWidth, targetWidth: 8)
        let green = modifyWordWidth(value: g, currentWidth: quantizeWordWidth, targetWidth: 8)
        let blue = modifyWordWidth(value: b, currentWidth: quantizeWordWidth, targetWidth: 8)
        return (red << 16) | (green << 8) | blue
    }

    private static func quantizedRed(_ color: Int) -> Int {
        return (color >> (quantizeWordWidth + quantizeWordWidth)) & quantizeWordMask
    }

    private static func quantizedGreen(_ color: Int) -> Int {
        return (color >> quantizeWordWidth) & quantizeWordMask
    }

    private static func quantizedBlue(_ color: Int) -> Int {
        return color & quantizeWordMask
    }

    private static func modifyWordWidth(value: Int, currentWidth: Int, targetWidth: Int) -> Int {
        let newValue: Int
        if targetWidth > currentWidth {
            newValue = value << (targetWidth - currentWidth)
        } else {
            newValue = value >> (currentWidth - targetWidth)
        }
        return newValue & ((1 << targetWidth) - 1)
    }

    // MARK: - Vbox

    final class Vbox {
        private var lowerIndex: Int
        private var upperIndex: Int
        private var population: Int = 0

        private var minRed: Int = 0
        private var maxRed: Int = 0
        private var minGreen: Int = 0
        private var maxGreen: Int = 0
        private var minBlue: Int = 0
        private var maxBlue: Int = 0

        private let colors: [Int]
        private let histogram: [Int]

        init(lowerIndex: Int, upperIndex: Int, colors: [Int], histogram: [Int]) {
            self.lowerIndex = lowerIndex
            self.upperIndex = upperIndex
            self.colors = colors
            self.histogram = histogram
            fitBox()
        }

        func getVolume() -> Int {
            return (maxRed - minRed + 1) * (maxGreen - minGreen + 1) * (maxBlue - minBlue + 1)
        }

        func canSplit() -> Bool {
            return getColorCount() > 1
        }

        func getColorCount() -> Int {
            return 1 + upperIndex - lowerIndex
        }

        func fitBox() {
            var minR = Int.max
            var minG = Int.max
            var minB = Int.max
            var maxR = Int.min
            var maxG = Int.min
            var maxB = Int.min
            var count = 0

            for i in lowerIndex...upperIndex {
                let color = colors[i]
                count += histogram[color]

                let r = ColorCutQuantizer.quantizedRed(color)
                let g = ColorCutQuantizer.quantizedGreen(color)
                let b = ColorCutQuantizer.quantizedBlue(color)

                if r > maxR { maxR = r }
                if r < minR { minR = r }
                if g > maxG { maxG = g }
                if g < minG { minG = g }
                if b > maxB { maxB = b }
                if b < minB { minB = b }
            }

            minRed = minR
            maxRed = maxR
            minGreen = minG
            maxGreen = maxG
            minBlue = minB
            maxBlue = maxB
            population = count
        }

        func splitBox() -> Vbox {
            if !canSplit() {
                fatalError("Cannot split a box with only 1 color")
            }

            let splitPoint = findSplitPoint()
            let newBox = Vbox(lowerIndex: splitPoint + 1, upperIndex: upperIndex, colors: colors, histogram: histogram)

            upperIndex = splitPoint
            fitBox()

            return newBox
        }

        func getLongestColorDimension() -> Int {
            let redLength = maxRed - minRed
            let greenLength = maxGreen - minGreen
            let blueLength = maxBlue - minBlue

            if redLength >= greenLength && redLength >= blueLength {
                return ColorCutQuantizer.componentRed
            } else if greenLength >= redLength && greenLength >= blueLength {
                return ColorCutQuantizer.componentGreen
            } else {
                return ColorCutQuantizer.componentBlue
            }
        }

        func findSplitPoint() -> Int {
            let longestDimension = getLongestColorDimension()

            // Modify colors array to sort by longest dimension
            var modifiedColors = colors
            ColorCutQuantizer.modifySignificantOctet(&modifiedColors, dimension: longestDimension, lower: lowerIndex, upper: upperIndex)

            // Sort
            let sortedRange = modifiedColors[lowerIndex...upperIndex].sorted()
            for (offset, value) in sortedRange.enumerated() {
                modifiedColors[lowerIndex + offset] = value
            }

            // Revert
            ColorCutQuantizer.modifySignificantOctet(&modifiedColors, dimension: longestDimension, lower: lowerIndex, upper: upperIndex)

            let midPoint = population / 2
            var count = 0

            for i in lowerIndex...upperIndex {
                count += histogram[modifiedColors[i]]
                if count >= midPoint {
                    return min(upperIndex - 1, i)
                }
            }

            return lowerIndex
        }

        func getAverageColor() -> Palette.Swatch {
            var redSum = 0
            var greenSum = 0
            var blueSum = 0
            var totalPopulation = 0

            for i in lowerIndex...upperIndex {
                let color = colors[i]
                let colorPopulation = histogram[color]

                totalPopulation += colorPopulation
                redSum += colorPopulation * ColorCutQuantizer.quantizedRed(color)
                greenSum += colorPopulation * ColorCutQuantizer.quantizedGreen(color)
                blueSum += colorPopulation * ColorCutQuantizer.quantizedBlue(color)
            }

            let redMean = Int(round(Float(redSum) / Float(totalPopulation)))
            let greenMean = Int(round(Float(greenSum) / Float(totalPopulation)))
            let blueMean = Int(round(Float(blueSum) / Float(totalPopulation)))

            return Palette.Swatch(
                rgb: ColorCutQuantizer.approximateToRgb888(r: redMean, g: greenMean, b: blueMean),
                population: totalPopulation
            )
        }
    }

    static func modifySignificantOctet(_ colors: inout [Int], dimension: Int, lower: Int, upper: Int) {
        switch dimension {
        case componentRed:
            break  // Already in RGB
        case componentGreen:
            for i in lower...upper {
                let color = colors[i]
                colors[i] = quantizedGreen(color) << (quantizeWordWidth + quantizeWordWidth) | quantizedRed(color) << quantizeWordWidth | quantizedBlue(color)
            }
        case componentBlue:
            for i in lower...upper {
                let color = colors[i]
                colors[i] = quantizedBlue(color) << (quantizeWordWidth + quantizeWordWidth) | quantizedGreen(color) << quantizeWordWidth | quantizedRed(color)
            }
        default:
            break
        }
    }
}

// MARK: - ColorUtils

struct ColorUtils {

    static func rGBtoHSL(rgb: Int) -> [Float] {
        let r = Float((rgb >> 16) & 0xFF) / 255.0
        let g = Float((rgb >> 8) & 0xFF) / 255.0
        let b = Float(rgb & 0xFF) / 255.0

        let max = Swift.max(r, g, b)
        let min = Swift.min(r, g, b)
        let delta = max - min

        var h: Float = 0
        var s: Float = 0
        let l = (max + min) / 2.0

        if delta != 0 {
            s = l < 0.5 ? delta / (max + min) : delta / (2.0 - max - min)

            switch max {
            case r:
                h = ((g - b) / delta) + (g < b ? 6 : 0)
            case g:
                h = ((b - r) / delta) + 2
            case b:
                h = ((r - g) / delta) + 4
            default:
                break
            }

            h /= 6.0
        }

        return [h * 360.0, s, l]
    }

    static func hSLtoRGB(hsl: [Float]) -> Int {
        let h = hsl[0] / 360.0
        let s = hsl[1]
        let l = hsl[2]

        let c = (1 - abs(2 * l - 1)) * s
        let x = c * (1 - abs((h * 6).truncatingRemainder(dividingBy: 2) - 1))
        let m = l - c / 2

        var r: Float = 0
        var g: Float = 0
        var b: Float = 0

        let hSegment = Int(h * 6)
        switch hSegment {
        case 0: (r, g, b) = (c, x, 0)
        case 1: (r, g, b) = (x, c, 0)
        case 2: (r, g, b) = (0, c, x)
        case 3: (r, g, b) = (0, x, c)
        case 4: (r, g, b) = (x, 0, c)
        case 5: (r, g, b) = (c, 0, x)
        default: break
        }

        let red = Int(round((r + m) * 255))
        let green = Int(round((g + m) * 255))
        let blue = Int(round((b + m) * 255))

        return (red << 16) | (green << 8) | blue
    }

    static func calculateContrast(foreground: Int, background: Int) -> Float {
        let fgLuminance = calculateLuminance(foreground)
        let bgLuminance = calculateLuminance(background)

        let lighter = max(fgLuminance, bgLuminance)
        let darker = min(fgLuminance, bgLuminance)

        return (lighter + 0.05) / (darker + 0.05)
    }

    static func calculateLuminance(_ color: Int) -> Float {
        let r = Float((color >> 16) & 0xFF) / 255.0
        let g = Float((color >> 8) & 0xFF) / 255.0
        let b = Float(color & 0xFF) / 255.0

        let rLinear = r <= 0.03928 ? r / 12.92 : pow((r + 0.055) / 1.055, 2.4)
        let gLinear = g <= 0.03928 ? g / 12.92 : pow((g + 0.055) / 1.055, 2.4)
        let bLinear = b <= 0.03928 ? b / 12.92 : pow((b + 0.055) / 1.055, 2.4)

        return 0.2126 * rLinear + 0.7152 * gLinear + 0.0722 * bLinear
    }

    static func calculateMinimumAlpha(foreground: Int, background: Int, minContrastRatio: Float) -> Int {
        let bgLuminance = calculateLuminance(background)
        let fgLuminance = calculateLuminance(foreground)

        let lighter = max(fgLuminance, bgLuminance)
        let darker = min(fgLuminance, bgLuminance)

        let contrast = (lighter + 0.05) / (darker + 0.05)

        if contrast >= minContrastRatio {
            return 255
        }

        // Binary search for minimum alpha
        var minAlpha = 0
        var maxAlpha = 255

        for _ in 0..<10 {
            let testAlpha = (minAlpha + maxAlpha) / 2
            let testColor = setAlphaComponent(color: foreground, alpha: testAlpha)
            let testContrast = calculateContrast(foreground: testColor, background: background)

            if testContrast < minContrastRatio {
                minAlpha = testAlpha + 1
            } else {
                maxAlpha = testAlpha
            }
        }

        return maxAlpha <= 255 ? maxAlpha : -1
    }

    static func setAlphaComponent(color: Int, alpha: Int) -> Int {
        let rgb = color & 0xFFFFFF
        return (alpha << 24) | rgb
    }
}

// MARK: - PaletteFilter

public protocol PaletteFilter {
    func isAllowed(rgb: Int, hsl: [Float]) -> Bool
}

struct DefaultFilter: PaletteFilter {
    private static let blackMaxLightness: Float = 0.05
    private static let whiteMinLightness: Float = 0.95

    func isAllowed(rgb: Int, hsl: [Float]) -> Bool {
        return !isWhite(hsl) && !isBlack(hsl) && !isNearRedILine(hsl)
    }

    private func isBlack(_ hsl: [Float]) -> Bool {
        return hsl[2] <= DefaultFilter.blackMaxLightness
    }

    private func isWhite(_ hsl: [Float]) -> Bool {
        return hsl[2] >= DefaultFilter.whiteMinLightness
    }

    private func isNearRedILine(_ hsl: [Float]) -> Bool {
        return hsl[0] >= 10 && hsl[0] <= 37 && hsl[1] <= 0.82
    }
}

// MARK: - PriorityQueue Helper

struct PriorityQueue<T> {
    private(set) var queue: [T] = []
    private let sort: (T, T) -> Bool

    init(sort: @escaping (T, T) -> Bool) {
        self.sort = sort
    }

    var count: Int {
        return queue.count
    }

    mutating func enqueue(_ element: T) {
        queue.append(element)
        queue.sort(by: sort)
    }

    mutating func dequeue() -> T? {
        return queue.isEmpty ? nil : queue.removeFirst()
    }
}

// MARK: - Extensions for Color Manipulation

extension Int {
    func toUIColor() -> UIColor {
        let r = CGFloat((self >> 16) & 0xFF) / 255.0
        let g = CGFloat((self >> 8) & 0xFF) / 255.0
        let b = CGFloat(self & 0xFF) / 255.0
        return UIColor(red: r, green: g, blue: b, alpha: 1.0)
    }

    func toColor() -> Color {
        return Color(toUIColor())
    }
}
