# 📱 `ios` — LearnCast iOS Application

This is the iOS-specific part of the LearnCast project. It contains a single Xcode project (`ios.xcodeproj`) with two source groups: `learncast` (the application entry point) and `lib` (the shared iOS UI library). All business logic, data, and ViewModels live in the `shared` KMP module — this layer is purely concerned with presenting that logic on iOS using SwiftUI.

**Main bundle identifier:** `me.anasmusa.learncast`  
**Swift format config:** `.swift-format` at the repo root of `ios/`

---

## 📋 Table of Contents

1. [Source Group Overview](#1-source-group-overview)
2. [Tech Stack at a Glance](#2-tech-stack-at-a-glance)
3. [Group: `learncast` (App)](#3-group-learncast-app)
4. [Group: `lib` (UI Library)](#4-group-lib-ui-library)
5. [Application Bootstrapping](#5-application-bootstrapping)
6. [Koin Integration](#6-koin-integration)
7. [Navigation](#7-navigation)
8. [Theme & Design System](#8-theme--design-system)
9. [Screens](#9-screens)
10. [Reusable Components](#10-reusable-components)
11. [Audio Playback Engine](#11-audio-playback-engine)
12. [Download Manager](#12-download-manager)
13. [Localization (String Resources)](#13-localization-string-resources)
14. [Build Configuration](#14-build-configuration)
15. [Package Structure](#15-package-structure)

---

## 1. Source Group Overview

| Group | Responsibility |
|---|---|
| `ios/learncast` | Entry point — configures `AppConfig`, sets up the SwiftUI scene, and provides gradient colors |
| `ios/lib` | All UI: screens, components, navigation, theme, player engine, download manager |

The `learncast` group is intentionally thin. It declares the `@main` `App` struct and `AppDelegate`, then hands off to `AppView` from `lib`. The `lib` group depends on the `shared` KMP framework, which is imported as `internal import Shared` to avoid re-exporting KMP types to consumers.

---

## 2. Tech Stack at a Glance

| Concern | Library / Tool |
|---|---|
| UI framework | SwiftUI |
| Navigation | SwiftUI `NavigationStack` with `NavigationDestination` |
| Image loading | Kingfisher 8 |
| Dependency injection | Koin (via `shared` module's `KoinUtils`) |
| Audio playback | `AVPlayer` + `AVAssetResourceLoader` (custom streaming) |
| Background downloads | `URLSession` background session |
| Lock screen / Control Center | `MediaPlayer` (`MPNowPlayingInfoCenter`, `MPRemoteCommandCenter`) |
| Push notifications | Firebase Cloud Messaging (via `firebase-ios-sdk`) |
| Analytics / Crash reporting | Firebase Analytics + Firebase Crashlytics |
| Google Sign-In | GoogleSignIn-iOS 9 |
| Color palette extraction | Custom `Palette` class (port of Android's `androidx.palette`) |
| Haptic feedback | `UIImpactFeedbackGenerator` / `UISelectionFeedbackGenerator` |
| Audio session management | `AVAudioSession` (`.playback` category) |
| Paging | Custom `ObservablePagingState<T>` wrapping the `shared` Paging 3 flow |

---

## 3. Group: `learncast` (App)

**Bundle ID:** `me.anasmusa.learncast`  
**Deployment target / team:** configured in `Configuration/Config.xcconfig`

This group contains the minimum code needed to launch the app. It has two responsibilities: configure `AppConfig` and supply the two gradient color arrays that define the app's visual identity.

### `LearncastApp`

**File:** `learncast/LearncastApp.swift`

The `@main` `App` struct. Creates an `AppEnvironment` with `backgroundColors` and `playerBackgroundColors`, injects it into the environment via `.environment(\.env, env)`, and attaches an `.onOpenURL` handler that forwards deep links to `AppInitializer.handle(url:)` (used for Google Sign-In callbacks).

### `AppDelegate`

**File:** `learncast/AppDelegate.swift`  
**Adopts:** `UIApplicationDelegate` via `@UIApplicationDelegateAdaptor`

Calls `AppConfig.companion.update(...)` with all environment-specific values, then calls `AppInitializer.initialize()`:

| Parameter | Value |
|---|---|
| `appName` | `"LearnCast"` |
| `apiBaseUrl` | `https://api.anasmusa.me/learncast/` |
| `publicBaseUrl` | `https://learncast.anasmusa.me` |
| `telegramBotId` | `8_538_344_134` |
| `googleClientId` | Google OAuth web client ID |
| `mainLogo` | `"MainLogo"` (asset catalog name) |
| `transparentLogo` | `"TransparentLogo"` (asset catalog name) |

### Resources (`learncast/Resources/`)

- **`Assets.xcassets`** — App icon (`AppIcon`), `MainLogo`, `TransparentLogo`, `Google` and `Telegram` sign-in button images
- **`Colors.xcassets`** — Full Material3 color token set as named colors (all dark-mode values, no light appearance variant). Tokens: `Primary`, `PrimaryContainer`, `Secondary`, `SecondaryContainer`, `Surface`, `SurfaceDim`, `SurfaceBright`, `SurfaceContainer*`, `Background`, `OnSurface`, `OnSurfaceVariant`, `Outline`, `OutlineVariant`, `Error`, `ErrorContainer`, `InverseOnSurface`, `InversePrimary`, `InverseSurface`, `Scrim`, `Tertiary`, `TertiaryContainer`, and all `On*` counterparts
- **`Fonts/`** — `FontRegular.ttf`, `FontMedium.ttf`, `FontBold.ttf` (Montserrat)

---

## 4. Group: `lib` (UI Library)

All SwiftUI screens, components, navigation, theme utilities, the `AVPlayer` engine, and the download service live here. The group is structured to mirror the `android/lib` module closely so that both platforms implement identical features with platform-native idioms.

The `lib` group also ships a DocC documentation target (`lib.docc/lib.md`).

---

## 5. Application Bootstrapping

### `AppInitializer`

**File:** `lib/AppInitializer.swift`  
**Access:** `public`

Performs all one-time app setup before the first frame renders:

1. Calls `FirebaseApp.configure()` to initialise Firebase (Analytics, Crashlytics, FCM)
2. Assigns `googleAuthManagerFactory` with an `IosGoogleAuthManager` instance so the `shared` module can trigger Google Sign-In
3. Assigns `AVPlayerDelegateCompanion.shared.factory` with an `AVPlayerDelegateImpl` instance — the full AVFoundation player
4. Assigns `downloadManagerFactory` with `IosDownloadManager.shared` — the `URLSession` background download service
5. Calls `Initializer.shared.doInitApp(debug:)` which starts Koin and runs all `shared` module setup
6. `handle(url:)` — forwards URL callbacks to `GIDSignIn.sharedInstance` for Google OAuth deep links

### `AppView`

**File:** `lib/AppView.swift`

The root SwiftUI `View`. Manages the top-level state machine:

- Observes `AppViewModel` (via `ObservableViewModel`) for `isLoggedIn` events (`AppEvent.ShowLoginScreen`, `AppEvent.ShowHomeScreen`)
- Maintains `selectedTab` (`Screen`) and three independent `NavController` instances — one per tab — so each tab remembers its own back stack
- Uses a SwiftUI `TabView` with three tabs: Home (`house`), Snips (`scissors`), Profile (`person`). Tab items render localized strings via `Strings.shared.HOME.string()` etc.
- `PlayerScreen` is rendered on top of the `TabView` in a `ZStack(alignment: .bottom)`, always present and overlaying the tab content
- Loads localized strings via `Resource.shared.setLocale(locale: "uz")` on first appear
- Applies `.preferredColorScheme(.dark)` globally — the app is dark-only

`PreviewRoot` and `PreviewSetup` are provided for SwiftUI Previews: `PreviewSetup.setup()` calls `AppConfig.companion.update(...)` and `Resource.shared.setLocale(...)` exactly once (guarded by a static flag) so previews work without a running simulator.

### `AppEnvironment`

**File:** `lib/core/AppEnvironment.swift`  
**Observation:** `@Observable`

Carries `backgroundColors: [Color]` and `playerBackgroundColors: [Color]` through the composition tree. The `backgroundGradient()` method returns a `LinearGradient` capped at 25% of the screen height (stop at `location: 0.25`), used as every screen's background.

Injected once at `LearncastApp` via `.environment(\.env, env)`. Consumed by any view via `@Environment(\.env) private var env`.

### `Environment.swift`

**File:** `lib/core/Environment.swift`

Declares three `@Entry` `EnvironmentValues` extensions so custom values propagate cleanly:

- `\.env` — `AppEnvironment`
- `\.navController` — `NavController`
- `\.navigationAnimation` — `Namespace.ID?` (used for matched geometry effects)

---

## 6. Koin Integration

**File:** `lib/koin.swift`

Bridges Swift's type system to Koin's Kotlin reflection API:

- `SwiftKClass<T>` — `NSObject` implementing `KotlinKClass`. Reports `isInstance`, `qualifiedName`, and `simpleName` from Swift's `Mirror` system
- `kClass<T>(for:)` — wraps a `SwiftKClass` in a `SwiftType` and calls `.getClazz()` to produce a `KotlinKClass` Koin can resolve against
- `named(name:)` — thin wrapper around `KoinUtils.shared.named(name:)` for qualifier-based injection
- `inject<T>(qualifier:parameters:)` — global free function; calls `KoinUtils.shared.koinGet(...)` and force-casts to `T`. Used throughout `lib` wherever a Koin-managed dependency is needed (e.g. `let downloadDao: DownloadDao = inject()`)

---

## 7. Navigation

**Package:** `lib/nav/`

### `Screen`

An `enum` conforming to `Hashable`. Each destination is a case:

| Case | Parameters |
|---|---|
| `entrance` | — (loading placeholder) |
| `login` | — |
| `home` | — |
| `snips` | — |
| `profile` | — |
| `topicList` | — |
| `topic` | `topic: Topic` |
| `authorList` | — |
| `author` | `author: Author` |
| `search` | `authorId: Int64`, `topicId: Int64?`, `selectedTab: Int = 0` |
| `storageUsage` | — |

### `NavController`

**`@Observable class`**

Wraps `backStack: [Screen]` (a plain `Array`) and exposes `navigate(screen:)`, `popBack()`, and `removeAll()`. The `backStack` array is bound directly to `NavigationStack(path:)` in `AppView`.

Provided via `\.navController` environment key and consumed by any view via `@Environment(\.navController)`.

### `EntryProvider` (`getView(screen:)`)

A `@ViewBuilder` free function that `switch`es over all `Screen` cases and returns the appropriate view. Registered as the `navigationDestination(for: Screen.self)` handler inside each tab's `NavigationStack`.

---

## 8. Theme & Design System

**Package:** `lib/theme/`

### Color System (`Colors.swift`)

`enum Colors` with `static let` properties, each backed by a named color from `Colors.xcassets`. Covers the full Material3 token set. Example usage: `Colors.primaryContainer`, `Colors.onSurface`.

### Typography (`Type.swift`)

`enum Typography` with `static let` properties returning `Font.custom("Montserrat-*", size:)` values for all 30 Material3 text styles (15 standard + 15 emphasized):

| Role | Regular weight | Emphasized weight |
|---|---|---|
| `displayLarge` | Montserrat-Regular, 57 | Montserrat-Medium, 57 |
| `titleMedium` | Montserrat-Medium, 16 | Montserrat-Bold, 16 |
| `bodyMedium` | Montserrat-Regular, 14 | Montserrat-Medium, 14 |
| `labelSmall` | Montserrat-Medium, 11 | Montserrat-Bold, 11 |
| … | … | … |

The root `AppView` sets `.font(Typography.bodyMedium)` so all views inherit Montserrat-Regular/14 as the default body font.

### Color Utilities (`core/Color.swift`)

- `lerpColor(_ color1: Color, _ color2: Color, _ t: CGFloat) -> Color` — linearly interpolates between two SwiftUI colors in RGB space via `UIColor` component extraction. Used to produce the mini-player background color from the album art palette
- `Int.darken(amount: Float) -> Color` — HSV manipulation: increases saturation by `amount`, decreases brightness by `amount * 0.6`. Exact match to the Android `Int.darken()` implementation
- `Int.lighten(amount: Float) -> Color` — HSV manipulation: increases brightness by `hsv[2] * amount`. Exact match to Android `Int.lighten()`
- `Color.init(_ rgbInt: Int)` — initializes a SwiftUI `Color` from a packed RGB integer (matching Jetpack Compose's `Color(Int)` constructor)

### Utility Extensions (`core/Utils.swift`)

- `Utils.bottomPadding` — `PlayerConstants.collapsedHeight + 16` dp, the standard content bottom padding used by all scrollable screens
- `View.applyIf(_:transform:)` — conditionally applies a view modifier without breaking the `some View` return type
- `Int.formatTime() -> String` — formats seconds as `MM:SS` or `H:MM:SS`
- `Int64.formatTimeFromMillis() -> String` — formats milliseconds as `MM:SS` or `HH:MM:SS`
- `Int64.formatTimeFromDuration() -> String` — formats Kotlin `Duration` nanosecond ticks (divides by `2_000_000_000`)
- `Int64.millis() -> Int64` — converts Kotlin `Duration` ticks to milliseconds (divides by `2_000_000`)
- `String.string() -> String` — calls `Resource.shared.string(self)`, the primary way all UI strings are resolved from localized XML
- `String.string(_:) -> String` / `string(_:[AnyObject?]) -> String` — overloads for printf-style format args
- `String.quantityString(_:) -> String` — plural-aware string resolution

### Haptic Feedback (`core/HapticFeedback.swift`)

`struct HapticFeedback` with three static methods: `light()`, `medium()`, and `selection()`. Used in the player controls and `TimeRangeSelector` drag interactions.

### Color Palette Extraction (`core/Palette.swift`)

A complete Swift port of Android's `androidx.palette.graphics.Palette` library, producing identical color targets. Key types:

- `Palette` — main class; exposes `getVibrantColor`, `getDarkVibrantColor`, `getLightVibrantSwatch`, `getMutedColor`, `getDominantColor`, and the full target-based API
- `Palette.Builder` — fluent builder; accepts a `UIImage`, downscales it to ≤ 112 × 112 px via `UIGraphicsImageRenderer`, extracts pixels from a `CGContext`, and runs `ColorCutQuantizer`
- `Palette.Swatch` — a quantized color with its pixel population, HSL values, and accessible title/body text colors (computed via WCAG contrast ratios)
- `Target` — describes a desired color profile (vibrant, muted, dark, light) with saturation/lightness ranges and scoring weights
- `ColorCutQuantizer` — median-cut color quantizer; reduces the image to up to 16 representative colors using a priority-queue split algorithm
- `ColorUtils` — HSL ↔ RGB conversion, WCAG luminance, contrast ratio, and minimum-alpha calculation
- `DefaultFilter` — discards near-black, near-white, and near-red-I-line colors (matches Android's default)

Used in `PlayerScreen` and `BottomPlayer` to extract a vibrant color from album art and build the player gradient: `vibrant.lighten(amount: 0.3)` for the top stop, `vibrant.darken(amount: 0.6)` for the bottom stop.

---

## 9. Screens

All screens follow the same pattern: a single View struct obtains its ObservableViewModel via inject(), collects state and events in .task { } blocks, and renders inline. PreviewRoot is used for all #Preview blocks.

**Package root:** `lib/screen/`

### Auth

**`LoginScreen`** — Full-screen login page with gradient background, large app logo, and two sign-in buttons (Telegram, Google). Errors are shown via `.snackbar(...)` modifier. Tapping "Continue with Telegram" presents `TelegramLoginScreen` as a sheet.

**`TelegramLoginScreen`** — A `WKWebView` embedded in SwiftUI via `UIViewRepresentable`. Loads the Telegram OAuth widget at `https://oauth.telegram.org/auth?bot_id=...`. A `WKScriptMessageHandler` named `iosHandler` receives the auth result from a JavaScript message posted when the Telegram callback fires. A cancel handler detects when the user taps the Telegram cancel button.

### Home

**`HomeScreen`** — `PagingList<Lesson>` with a `LazyVStack` header containing: Authors and Topics navigation buttons (`PrimaryButton`), a `SearchButton`, and a horizontal `ScrollView` of `FilterChip` components for the `Filters` enum (`Latest`, `In Progress`, `Favourite`, `Downloads`, `Most Snipped`). Pull-to-refresh is handled by `PagingList`'s `.refreshable` block.

### Author

**`AuthorListScreen`** — Paginated list of authors with search, navigates to `AuthorScreen` on tap.

**`AuthorScreen`** — Detail view for a single author, showing their topics and lessons. Supports a "Play all" action that fills the playback queue.

### Topic

**`TopicListScreen`** — Paginated list of topics with optional author filter.

**`TopicScreen`** — Detail view for a single topic showing filtered lessons, with "Play all" support.

### Search

**`SearchScreen`** — Tab-based search across authors, topics, and lessons. Launched from `Home` or `AuthorScreen` with a pre-filled `authorId` or `topicId` filter. Backed by `SearchViewModel` from the `shared` module.

### Player

**`PlayerScreen`** — The main audio player, always present in the `AppView` `ZStack` on top of the `TabView`. Implemented with a manual `DragGesture` and `yOffset` state.

Key details:

- `collapsedOffset = screenHeight - miniPlayerBottomPadding - collapsedHeight` — the resting y-offset when collapsed
- `ratio = yOffset / collapsedOffset` — drives all size/opacity transitions
- `expandedPlayerContent` is shown at `ratio < 0.8`, fading out as `opacity = 1.0 - ratio / 0.8`
- `collapsedPlayerContent` is shown at `ratio >= 0.8`, fading in as `opacity = 1 - (1 - ratio) / 0.2`
- Snap animation uses `.spring(response: 0.35, dampingFraction: 0.8)` based on `predictedEndTranslation`
- Album art is loaded via `KingfisherManager` at 112 × 112 px, fed into `Palette.Builder`, and the extracted vibrant color produces `backgroundColors` for the gradient. `lerpColor` at `t = 0.7` produces the collapsed player's solid background color
- `QueueScreen` and `PlayerSnipScreen` overlay the expanded player with `.transition(.move(edge: .bottom))`
- `SnipEditScreen` and `PlayerActionSheet` are presented as sheets
- `PlayerSlider` — a fully custom SwiftUI seek slider. Expands from 8 dp to 16 dp height while dragging. Uses `DragGesture(minimumDistance: 0)` and ignores state updates within 3 s of a completed seek to avoid jumpy playback position feedback
- `PlayerConstants` — `collapsedHeight = 64`, `miniPlayerBottomPadding = 90` (tab bar height)

**`BottomPlayer`** — Standalone mini-player composable used in other contexts where only a compact now-playing row is needed.

**`QueueScreen`** — Reorderable queue list. Drag-to-reorder is implemented with SwiftUI's `onDrag` / `onDrop` or a custom gesture (depending on the iOS version). Each item shows a swipe action for removal and opens `QueueActionSheet` on long press.

**`PlayerSnipScreen`** — Lists all snips for the currently playing lesson. Accessible via the snip count badge in the expanded player.

### Snip

**`SnipListScreen`** — Paginated list of all the user's snips across all lessons, with debounced search. Tapping a snip adds it to the queue.

**`SnipEditScreen`** — Presented as a `.sheet` with a custom `presentationDetents` height. Contains:
- A `TimeRangeSelector` for picking start and end points
- A `TextField(axis: .vertical)` for an optional note (max 128 characters, enforced via `onChange`)
- Play / Stop preview controls backed by `SnipEditViewModel`; start/end changes are debounced 1 s via `Combine`'s `PassthroughSubject` before dispatching `SnipEdiIntentStart` so the preview player doesn't stutter
- A Save button that dispatches `SnipEdiIntentSave`
- `presentationBackground` uses `LinearGradient` from the parent player's extracted `colors` array

### Profile

**`ProfileScreen`** — Shows the logged-in user's avatar (loaded with Kingfisher), name, and username. Contains a Storage Usage button and a Sign Out button with a `ConfirmationBottomSheet`.

**`StorageUsageScreen`** — Shows cache and download storage sizes from `StorageViewModel`. Provides "Clear Cache" and "Clear Downloads" actions, each guarded by a `ConfirmationBottomSheet`.

---

## 10. Reusable Components

**Package:** `lib/component/`

| Component | Description |
|---|---|
| `PrimaryButton` | Icon + label button using SF Symbols or a custom `Image`. Used for Authors/Topics/Storage navigation in `HomeScreen` |
| `SearchButton` | Inline search field toggling between a compact icon and an expanded `TextField`. Used in `HomeScreen` and `AuthorScreen` |
| `QueueButton` | Circular button with a badge count overlay. Used in the full and collapsed player |
| `SheetMenuButton` | Full-width tappable row for bottom sheet menus (player action sheet, queue action sheet) |
| `ConfirmationBottomSheet` | Generic confirmation sheet with title, message, positive button, and dismiss. Used for logout, clear-cache, and clear-downloads |
| `LoaderView` | Full-screen semi-transparent overlay with a `ProgressView`. Shown during async operations |
| `SnackbarModifier` | `ViewModifier` that overlays a timed snackbar message at the bottom of the view. Applied via `.snackbar(message:)` |
| `FilterChip` | Selectable chip with selected/unselected visual states. Used in `HomeScreen` for the lesson filter row |
| `MarqueeView` | Scrolling text container for long titles. Renders the content twice in an `HStack` with a 32 pt gap, animates `offset` with `.linear(duration: 35).repeatForever(autoreverses: false)`, and masks the edges with `LinearGradient` for a fade-in/fade-out effect. Stops scrolling if content fits without overflow |
| `PagingList<T, Content, Header>` | Generic paginated list backed by `ObservablePagingState<T>`. Accepts a `SkieSwiftFlow<ListPagingState<T>>`, an optional header builder, and a cell builder. Calls `pagingState.notify(index:)` in `.onAppear` to trigger next-page loads and `.refreshable` for pull-to-refresh |
| `TimeRangeSelector` | Custom seek range control for the snip editor. Combines a `CustomRangeSlider` (minute-resolution, `RangeSlider` with two drag handles) with two `TimeSpinner` `LazyVStack` scroll pickers (second-resolution). Changes from the slider update the spinners via `@Observable` `TimeRangeSelectorState`, and vice versa. A red dot indicates the current playback position |

### List Cell Components (`component/cell/`)

| Cell | Usage |
|---|---|
| `LessonCell` | Thumbnail, author name, title, date, and duration |
| `AuthorCell` | Author avatar, name, and lesson/topic counts |
| `TopicCell` | Topic thumbnail and title |
| `SnipCell` | Timestamp range, note preview, and action overflow |
| `QueueItemCell` | Thumbnail, title, drag handle, and swipe-to-remove target |

---

## 11. Audio Playback Engine

**Package:** `lib/core/player/`

The iOS audio stack is built entirely on `AVFoundation` without Media3, matching the `shared` module's `AVPlayerDelegate` protocol.

### `AVPlayerDelegateImpl`

**File:** `lib/core/player/AVPlayerDelegateImpl.swift`  
**Implements:** `AVPlayerDelegate` (from `shared`)

A full `AVPlayer` controller that satisfies the `shared` module's playback abstraction. Key design points:

- Maintains an internal `queue: [QueueItem]` and `currentIndex`. All queue mutations (`add`, `move`, `replace`, `remove`, `setItems`) are mirrored onto this array
- Uses `AVPlayer.publisher(for: \.timeControlStatus)` (Combine) to detect play/pause transitions and dispatch `onIsPlayingChanged` and `onPlaybackStateChanged` to the `AVPlayerCallback`
- A periodic time observer (1 s interval) updates `MPNowPlayingInfoCenter` with the current elapsed time
- Audio interruptions (phone calls, Siri) are handled via `AVAudioSession.interruptionNotification`; the player resumes automatically if `shouldResume` is set
- `AVAudioSession` is configured with `.playback` category so audio continues in the background
- `MPRemoteCommandCenter` registers Play, Pause, Skip Backward (−10 s), and Skip Forward (+30 s) commands for lock-screen and Control Center controls
- `createAVPlayerItem(item:)` rewrites the audio URL scheme to `learncast://` and attaches `ResourceLoaderDelegate` as the `AVAssetResourceLoader` delegate, enabling the custom cache/streaming layer. For snip items, `forwardPlaybackEndTime` and `reversePlaybackEndTime` are set on the `AVPlayerItem` to enforce the snip boundaries
- Album art is loaded via Kingfisher and set on `MPMediaItemPropertyArtwork` asynchronously after the now-playing info is first posted

### `ResourceLoaderDelegate`

**File:** `lib/core/player/ResourceLoaderDelegate.swift`  
**Implements:** `AVAssetResourceLoaderDelegate`

Intercepts all `AVPlayer` loading requests and serves data from three sources in priority order:

1. **Complete download** — if `StorageManager.getDownload(forKey:)` returns a `CacheSpan`, the entire file is streamed from the `Documents/audio/` directory in 256 KB chunks
2. **Cache segments** — `StorageManager.getSpans(forKey:intersecting:)` returns a list of cached `CacheSpan`s; `RangeCalculator.mergeSegments` interleaves them with remote gaps to produce a `[DataSegment]` work list
3. **Remote** — missing byte ranges are fetched via a `URLSession` (no-redirect delegate) using HTTP `Range` headers against a presigned Cloudflare URL; received bytes are streamed directly to the `AVAssetResourceLoadingDataRequest` in 256 KB chunks and simultaneously cached via `StorageManager.saveCacheSpan`

Presigned URL lifecycle: the delegate first fetches the presigned URL from the `apiBaseUrl` audio endpoint (attaching the bearer token from `TokenProvider`); the server responds with HTTP 307, and the `Location` header is cached. On 401 responses, `TokenProvider.refreshTokens` is called and the fetch is retried once.

### `StorageManager`

**File:** `lib/core/player/cache/StorageManager.swift`

Unified on-device audio storage with LRU eviction:

- **Cache directory** — `Library/Caches/audio/` — temporary, OS-evictable
- **Download directory** — `Documents/audio/` — permanent user downloads
- Stores audio in **5 MB chunk files** (e.g. `audio_<sha256>_chunk_0`, `…_chunk_1`). Chunk file paths are derived from a SHA-256 hash of the resource key, making filenames safe and deterministic
- `saveCacheSpan(forKey:data:range:)` splits incoming data across the appropriate chunk files and inserts `CacheSpan` records into `CacheIndex` (a Koin-managed Kotlin DAO)
- Cache eviction is triggered before each write if the projected size exceeds 200 MB. The 100 oldest `CacheSpan` entries (by `lastAccessedAt`) are deleted until the limit is met
- `MetadataIndex` stores `contentLength` and `contentType` per resource key so `ResourceLoaderDelegate` can fill `AVAssetResourceLoadingContentInformationRequest` without a network round-trip on repeat plays

### `RangeCalculator`

**File:** `lib/core/player/cache/RangeCalculator.swift`

Computes a `[DataSegment]` work list from a requested byte range and the available `CacheSpan`s, classifying each sub-range as `.cache(span:offset:length:)`, `.download(span:offset:length:)`, or `.remote(startOffset:endOffset:)`.

### `NoRedirectSessionDelegate`

**File:** `lib/core/player/NoRedirectSessionDelegate.swift`

An `URLSessionTaskDelegate` that returns `nil` from `willPerformHTTPRedirection` to prevent `URLSession` from automatically following the 307 redirect to the Cloudflare presigned URL. `ResourceLoaderDelegate` extracts the `Location` header manually instead.

### `IosGoogleAuthManager`

**File:** `lib/core/google/IosGoogleAuthManager.swift`  
**Implements:** `GoogleAuthManager` (from `shared`)

Wraps `GIDSignIn.sharedInstance.signIn(withPresenting:)`. Retrieves the root `UIViewController` from `UIApplication.shared.keyWindow` and returns the Google ID token string on success, or `nil` on cancellation.

---

## 12. Download Manager

**File:** `lib/data/service/IosDownloadManager.swift`  
**Implements:** `DownloadManager` (from `shared`)  
**Session:** `URLSessionConfiguration.background(withIdentifier: "com.learncast.media-download")`

Manages background audio downloads using a `URLSession` background session so downloads complete even when the app is suspended or killed.

Key design points:

- Singleton (`shared`) — the `URLSession` delegate must be retained for the lifetime of the app
- `maxParallelDownloads = 1` — downloads are queued; only one runs at a time. Queued items are tracked in `downloadQueue: [DownloadItem]`
- **Presigned URL flow** — the initial request carries a bearer token. The server responds with HTTP 307; the `URLSessionTaskDelegate` intercepts `willPerformHTTPRedirection` and strips the `Authorization` header before following the redirect to the Cloudflare URL, caching the presigned URL for retries
- **Token refresh** — on 401 responses, `TokenProvider.refreshTokens(refreshToken:)` is called and the download is retried up to `maxRetryAttempts = 2` times
- On completion, `URLSessionDownloadDelegate.urlSession(_:downloadTask:didFinishDownloadingTo:)` moves the temp file to `Documents/audio/<filename>`, writes a `CacheSpan` to `downloadIndex`, and saves content metadata to `MetadataIndex`
- Progress is reported via `downloadDao.update(id:state:percentDownloaded:)`

---

## 13. Localization (String Resources)

The `lib` group reads string content from XML files in `ios/Resources/`:

- `strings.xml` — English strings
- `strings-uz.xml` — Uzbek strings

These are the same files that live in `shared/src/commonMain/resources/` — on iOS they are bundled as-is and loaded by the `shared` module's `Resource` class. `Resource.shared.setLocale(locale:onLoad:)` is called once in `AppView.onAppear`. All UI strings are then resolved via `Strings.shared.HOME.string()` or the `String.string()` Swift extension.

---
## 14. Build Configuration

**File:** `ios/Configuration/Config.xcconfig`

### Swift Package Dependencies (`Package.resolved`)

| Package | Version |
|---|---|
| `firebase-ios-sdk` | 12.9.0 |
| `googlesignin-ios` | 9.1.0 |
| `kingfisher` | 8.6.2 |
| `appauth-ios` | 2.0.0 |
| `gtmappauth` | 5.0.0 |
| `gtm-session-fetcher` | 3.5.0 |
| `googleappmeasurement` | 12.8.0 |
| `googledatatransport` | 10.1.0 |
| `googleutilities` | 8.1.0 |
| `promises` | 2.4.0 |
| `app-check` | 11.2.0 |
| _(gRPC/Abseil/LevelDB/nanopb)_ | Firebase transitive deps |

---

## 15. Package Structure

```
ios/
├── Configuration/
├── Resources/                   # English + Uzbek string XMLs (copied from shared/commonMain)
├── ios.xcodeproj/
│   └── project.xcworkspace/
│       └── xcshareddata/
│           └── swiftpm/         # SPM lockfile
├── learncast/                   # App target
│   └── Resources/
│       ├── Assets.xcassets/     # App icon, logos, sign-in button images
│       ├── Colors.xcassets/     # Full Material3 named color token set
│       └── Fonts/               # Montserrat font variants (Regular, Medium, Bold)
└── lib/                         # UI library group
    ├── lib.docc/
    ├── core/
    │   ├── google/
    │   └── player/
    │       └── cache/
    ├── data/
    │   ├── model/               # Swift extensions on shared KMP models (Identifiable conformances)
    │   └── service/
    ├── nav/
    ├── theme/
    ├── component/
    │   └── cell/
    └── screen/
        ├── auth/
        ├── author/
        ├── home/
        ├── player/
        │   ├── queue/
        │   └── snip/
        ├── profile/
        ├── snip/
        └── topic/
```
