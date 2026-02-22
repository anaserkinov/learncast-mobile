# 📦 `shared` — LearnCast Shared Module

This module is a [Kotlin Multiplatform (KMP)](https://kotlinlang.org/docs/multiplatform.html) library targeting `androidLibrary` and `iosArm64`/`iosSimulatorArm64`. It exposes shared business logic, data, and presentation state to Android (Jetpack Compose) and iOS (SwiftUI) via a compiled framework (`Shared.framework`). Dependency injection is handled by Koin. The architecture follows a unidirectional MVI pattern with `BaseViewModel`.

---

## 📋 Table of Contents

1. [Module Overview](#1-module-overview)
2. [Tech Stack at a Glance](#2-tech-stack-at-a-glance)
3. [Local Database](#3-local-database)
4. [Preferences / Settings Storage](#4-preferences--settings-storage)
5. [File & Cache Storage](#5-file--cache-storage)
6. [Networking](#6-networking)
7. [Repositories](#7-repositories)
8. [Platform-Specific Implementations](#8-platform-specific-implementations)
9. [ViewModels](#9-viewmodels)
10. [Dependency Injection](#10-dependency-injection)
11. [Testing](#11-testing)
12. [Package Structure](#12-package-structure)

---

## 1. Module Overview

The `shared` module is the single source of truth for the entire LearnCast application. Rather than writing data access, networking, and UI-logic code twice — once for Android and once for iOS — everything lives here and is shared.

**What this module provides:**

- A structured, offline-first local database that caches all app content
- A secure preferences store for user credentials and settings
- A full networking layer that handles authentication, token refresh, and HTTP caching
- An outbox-based sync system that queues user actions (favourites, progress updates, snip edits) and retries them when connectivity is available
- Audio playback management for both Android (ExoPlayer) and iOS (AVPlayer)
- Download management for offline lesson audio
- All screen-level state and logic via a set of cross-platform ViewModels

**Main package root:** `me.anasmusa.learncast`

---

## 2. Tech Stack at a Glance

| Concern | Library / Tool | Version Alias |
|---|---|---|
| Multiplatform framework | Kotlin Multiplatform (KMP) | `kotlinMultiplatform` |
| Dependency Injection | [Koin](https://insert-koin.io/) | `koin.core` |
| Networking | [Ktor Client](https://ktor.io/docs/client-create-multiplatform-application.html) | `ktor.client.*` |
| JSON serialization | kotlinx.serialization | `kotlinx.serialization.json` |
| Local database | [Room (KMP)](https://developer.android.com/jetpack/androidx/releases/room) | `room.runtime` |
| Preferences | [DataStore (Okio)](https://developer.android.com/topic/libraries/architecture/datastore) + [Wire (Protobuf)](https://square.github.io/wire/) | `datastore.core.okio`, `squareupWire` |
| Paging | [Paging 3 (KMP)](https://developer.android.com/topic/libraries/architecture/paging/v3-overview) | `paging.common` |
| ViewModels | [AndroidX Lifecycle ViewModel (KMP)](https://developer.android.com/jetpack/androidx/releases/lifecycle) | `androidx.lifecycle.viewmodel` |
| Date/time | [kotlinx-datetime](https://github.com/Kotlin/kotlinx-datetime) | `kotlinx.datetime` |
| Logging | [Napier](https://github.com/AAkira/Napier) | `napier` |
| Android player | [Media3 ExoPlayer](https://developer.android.com/media/media3/exoplayer) | `androidx.media3.*` |
| iOS player | AVPlayer (via Kotlin/Native interop) | platform SDK |
| Push notifications | [Firebase Cloud Messaging](https://firebase.google.com/docs/cloud-messaging) | `firebase.messaging` |
| Google auth | [Credential Manager](https://developer.android.com/training/sign-in/credential-manager) | `androidx.credentials.*` |
| Background sync (Android) | [WorkManager](https://developer.android.com/topic/libraries/architecture/workmanager) | `androidx.work.runtime` |
| Swift interop | [SKIE](https://skie.touchlab.co/) | `skie` |
| KSP (code generation) | KSP | `devtools.ksp` |

---

## 3. Local Database

The database is built on **Room for KMP** (`androidx.room`), with shared entity/DAO definitions in `commonMain` and platform-specific `RoomDatabase.Builder` implementations in `androidMain` and `iosMain`. The DB schema version is `1`, with a schema snapshot in `shared/schemas/`.

**Main package:** `me.anasmusa.learncast.data.local.db`

**Database class:** `AppDatabase` (extends `RoomDatabase`)  
**DB file name:** `app.db`

### Tables

| Table | Entity Class | Purpose |
|---|---|---|
| `author` | `AuthorEntity` | Cached author profiles |
| `topic` | `TopicEntity` | Cached topic listings |
| `lesson` | `LessonEntity` | Cached lesson metadata |
| `lesson_state` | `LessonStateEntity` | User's playback progress per lesson |
| `snip` | `SnipEntity` | User-created audio clips (snips) |
| `queue_item` | `QueueItemEntity` | Current audio playback queue |
| `outbox` | `OutboxEntity` | Pending actions to be synced to the server |
| `lesson_outbox` | `LessonOutboxEntity` | Lesson-specific outbox payloads |
| `snip_outbox` | `SnipOutboxEntity` | Snip-specific outbox payloads |
| `listen_outbox` | `ListenOutboxEntity` | Listen-session outbox payloads |
| `paging_state` | `PagingStateEntity` | Tracks the last network page fetched, enabling the `RemoteMediator` pattern |
| `download_state` | `DownloadStateEntity` | Per-file download progress and state |

**Database view:** `queue_item_with_state` — a SQL view joining `queue_item` and `lesson_state` to expose the current playback item with progress in one query.

### Type Converters

Located in `Converters.kt`, two converters bridge Room's supported types to Kotlin types:

- `LocalDateTimeConverter` — encodes/decodes `kotlinx.datetime.LocalDateTime` as a `Long` (epoch milliseconds)
- `DurationConverter` — encodes/decodes `kotlin.time.Duration` as a `Long` (milliseconds)

### DAOs

Each table has a dedicated DAO:

| DAO | Key Operations |
|---|---|
| `AuthorDao` | Paginated query with search filter |
| `TopicDao` | Paginated query with search and `authorId` filter |
| `LessonDao` | Paginated query with multiple filters (search, author, topic, favourite, status, downloaded, sort/order); upsert progress; update snip count |
| `SnipDao` | Insert, get by `clientSnipId`, paginated query |
| `QueueItemDao` | Add first, move (reorder), remove, clear, observe count, get all |
| `OutboxDao` | Get next item to sync, observe for changes, insert/update/delete outbox entities with their typed join tables |
| `PagingStateDao` | Upsert and retrieve paging state keyed by query parameters |
| `DownloadDao` | Get, insert, delete download state entries |

### Offline-First Paging (RemoteMediator)

The module implements the **RemoteMediator** pattern from Paging 3. Each pageable resource (Lesson, Author, Topic, Snip) has a corresponding mediator:

- `LessonMediator`, `AuthorMediator`, `TopicMediator`, `SnipMediator`

These mediators intercept paging requests, fetch pages from the network API, store results in Room, and update `PagingStateDao`. The `PagingSource` always reads from Room — the network is only hit when the local data is stale or new pages are needed.

---

## 4. Preferences / Settings Storage

User preferences are stored using **AndroidX DataStore (Okio backend)** with a **Protocol Buffer** schema defined in `preference_data.proto` (compiled by the Wire Gradle plugin). The `PreferenceData` protobuf message holds: `accessToken`, `refreshToken`, `user` (nested message), and `lang`.

**Main package:** `me.anasmusa.learncast.data.local.preference`

**File name on disk:** `preference_data.pb`

**Interface:** `Preferences`

| Method | Description |
|---|---|
| `updateToken(refreshToken, accessToken)` | Persists a new token pair after login or token refresh |
| `getToken(): Flow<Pair<String, String>?>` | Reactive stream of the current token pair; emits `null` when logged out |
| `updateUser(user)` | Persists the logged-in user's profile |
| `getUser(): Flow<PreferenceData.User?>` | Reactive stream of the current user; emits `null` when not logged in |
| `setLang(lang)` | Persists the user's preferred UI language |
| `getLang(): Flow<String?>` | Reactive stream of the current language setting |
| `clear()` | Clears all auth-related data on logout |

**Implementation:** `PreferenceImpl` — reads/writes via `DataStore<PreferenceData>`.

**Serializer:** `PreferenceSerializer` — implements `OkioSerializer<PreferenceData>`, using the Wire-generated `PreferenceData.ADAPTER` for binary encode/decode.

**Platform `expect/actual`:** `getDataStore()` is declared as `expect fun` in `commonMain`. Both `androidMain` and `iosMain` provide `actual` implementations using `DataStoreFactory.create()` with `OkioStorage`, pointing to the platform-appropriate documents directory.

---

## 5. File & Cache Storage

Storage management is abstracted behind `StorageManager` with platform-specific implementations.

**Main package:** `me.anasmusa.learncast.data.local.storage`

**Interface:** `StorageManager`

| Method | Description |
|---|---|
| `getCacheSize(): Float` | Returns streaming cache size in MB |
| `getDownloadSize(): Float` | Returns downloaded audio size in MB |
| `clearCaches()` | Deletes the HTTP response cache and the player's streaming cache |
| `clearDownloads()` | Deletes downloaded audio files |

**Android (`AndroidStorageManager`):**
- Cache size is computed by walking `context.externalCacheDir`
- Download size is computed by walking `context.getExternalFilesDir(Environment.DIRECTORY_PODCASTS)`
- Clears ExoPlayer's `SimpleCache` for both the playback and download caches
- Also clears Ktor's HTTP response cache via `CachingCacheStorage`

**iOS (`IosStorageManager`):**
- Uses injected `CacheIndex` instances (keyed by `PlaybackCacheScope` and `DownloadCacheScope`) for size tracking
- Deletes the `audio/` subdirectory inside `NSCachesDirectory` (streaming) and `NSDocumentDirectory` (downloads) using `NSFileManager`

---

## 6. Networking

Networking is built on **Ktor Client** with platform-specific engines. The HTTP client is configured once in `commonMain` and instantiated via `expect/actual` in each platform target.

**Main package:** `me.anasmusa.learncast.data.network`

### HTTP Client Configuration

`HttpClient.kt` defines `createHttpClient(block)` as an `expect fun`. Both Android (`OkHttp` engine) and iOS (`Darwin` engine) provide `actual` implementations and install **`HttpCache`** using a shared `CachingCacheStorage`.

The shared `configure(getTokenManager, setExplicitNulls)` extension function installs:

| Plugin | Configuration |
|---|---|
| `Logging` | Full request/response logging via Napier |
| `HttpTimeout` | Connect: 10 s, Request: 60 s, Socket: 30 s |
| `ContentNegotiation` | JSON with `ignoreUnknownKeys`, `coerceInputValues`, `explicitNulls = false`, and a custom `InstantSerializer` |
| `Auth (Bearer)` | Loads tokens from `TokenManager`; calls `refreshToken()` on 401; skips auth header for the `/logout` endpoint |
| `defaultRequest` | Sets `Content-Type: application/json`, `Accept-Language: uz`, and the base URL from `appConfig.apiBaseUrl` |

### Token Management

`TokenManager` handles all authentication token lifecycle:

- `getTokens()` — reads the current token pair from `Preferences`
- `refreshToken(refreshToken)` — thread-safe token refresh using a `Mutex` and a coroutine `Job`. If a refresh is already in progress, new callers join the existing job rather than issuing a duplicate request. On a `401` response during refresh, it calls `authRepository.logout()` to clear local state.

### Services

Each domain entity has a dedicated internal service class:

| Service | Package | Endpoints |
|---|---|---|
| `AuthService` | `.network.auth` | `POST v1/user/auth/signin`, `POST v1/user/auth/refresh-token`, `POST v1/user/auth/logout` |
| `LessonService` | `.network.lesson` | `GET v1/user/lesson` (paginated), `GET v1/user/lesson/deleted`, `PATCH v1/user/lesson/{id}/progress`, `POST v1/user/lesson/{id}/listen`, `POST/DELETE v1/user/lesson/{id}/favourite` |
| `SnipService` | `.network.snip` | `GET v1/user/lesson/snip` (paginated), `GET v1/user/lesson/{id}/snip/count`, `POST v1/user/lesson/{id}/snip`, `PUT v1/user/lesson/snip/{id}`, `DELETE v1/user/lesson/snip/{id}`, `GET v1/user/lesson/snip/deleted` |
| `AuthorService` | `.network.author` | `GET v1/user/author` (paginated), `GET v1/user/author/deleted` |
| `TopicService` | `.network.topic` | `GET v1/user/topic` (paginated), `GET v1/user/topic/deleted` |

All paginated endpoints use `PageRequestQuery` (which serializes query params onto the URL) and return `BaseResponse<PageResponse<T>>`. Deleted-entity endpoints use `DeletedRequestQuery` for incremental sync.

---

## 7. Repositories

Repositories are defined as interfaces in `.data.repository.abstraction` and implemented in `.data.repository.implementation`. They are injected via Koin and used exclusively by ViewModels.

**Main packages:**
- Abstractions: `me.anasmusa.learncast.data.repository.abstraction`
- Implementations: `me.anasmusa.learncast.data.repository.implementation`

---

### `AuthRepository` / `AuthRepositoryImpl`
Manages user authentication state.
- `loginWithTelegram(hash)` — posts a Telegram WebApp hash to `AuthService.login()`, persists credentials and user data, subscribes to push notifications, and starts the player service
- `loginWithGoogle()` — obtains an ID token from `GoogleAuthManager`, then follows the same flow as Telegram login
- `logout()` — clears the queue, stops the player service, cancels any in-flight token refresh, calls the logout API, clears all caches and downloads, unsubscribes from push notifications, wipes the database, and clears preferences
- `isLoggedIn(): Flow<Boolean>` — derived from `Preferences.getUser()` as a reactive stream

### `LessonRepository` / `LessonRepositoryImpl`
Returns paginated `Flow<PagingData<Lesson>>` via `CommonPager` backed by `LessonMediator` (network) and `LessonDao` (local). Supports filtering by: `search`, `authorId`, `topicId`, `isFavourite`, `status` (`UserProgressStatus`), `isDownloaded`, `sort`, and `order`.

### `AuthorRepository` / `AuthorRepositoryImpl`
Returns paginated `Flow<PagingData<Author>>` with optional `search`. Uses `AuthorMediator` and `AuthorDao`.

### `TopicRepository` / `TopicRepositoryImpl`
Returns paginated `Flow<PagingData<Topic>>` with optional `search` and `authorId`. Uses `TopicMediator` and `TopicDao`.

### `SnipRepository` / `SnipRepositoryImpl`
- `save(clientSnipId, queueItemId, startMs, endMs, note)` — creates a new `SnipEntity` locally and queues a `CREATE` outbox entry, or queues an `UPDATE` entry for an existing snip
- `get(clientSnipId)` — reads a snip from `SnipDao`
- `delete(clientSnipId)` — queues a `DELETE` outbox entry via `OutboxRepository`
- `page(...)` — paginated listing via `SnipMediator` and `SnipDao`
- `getSnipCount(lessonId)` — fetches the count from the API (updating the local DB) and returns the local value

### `OutboxRepository` / `OutboxRepositoryImpl`
The outbox is the engine behind the app's **offline-first sync**. Any user action that must reach the server (progress update, favourite toggle, snip CRUD, listen session) is written to the outbox database table first. The outbox then drains asynchronously.

- `getToSync()` — returns the next pending outbox item (mutex-protected)
- `onOutboxSynced(...)` — handles post-sync cleanup: deletes the entry on success, retries on network error, handles action-type transitions (e.g., a `CREATE` that succeeded becomes an `UPDATE` for future edits)
- `listen(lessonId)` — inserts a `LISTEN` outbox entry with a UUID session ID
- `setFavourite(lessonId, isFavourite)` — inserts or merges a `FAVOURITE`/`REMOVE_FAVOURITE` entry (cancels opposite pending entries)
- `updateLessonProgress(...)` — upserts a `UPDATE` outbox entry with latest position, status, and timestamps (mutex + DB transaction)
- `createSnip/updateSnip/deleteSnip` — insert typed outbox entries for snip CRUD

### `SyncRepository` / `SyncRepositoryImpl`
Drains the outbox by processing one item at a time:
- `sync(finishWhenDrained = false)` — observes the outbox in a `Flow` (using `CONFLATED` buffering to avoid backlogs) and calls the appropriate API endpoint for each item. On network error, delays 2 minutes before retrying
- `sync(finishWhenDrained = true)` — processes all pending items and returns, used for foreground sync on WorkManager (Android) or app foreground (iOS)

### `PlayerRepository` / `PlayerRepositoryImpl`
The bridge between the abstract `PlayerController` (platform player) and the `QueueRepository`. Exposes:
- `currentQueueItem: StateFlow<QueueItem?>` — flat-maps the player's current item ID through `QueueRepository.observe()`
- `playbackPositionMs: StateFlow<Long>` — polls `PlayerController.getCurrentPositionMs()` every second while playing, with exponential back-off while paused
- `playbackState: StateFlow<Int>` — forwarded from `PlayerController`
- `events: Channel<Int>` — one-shot events (e.g., `EVENT_SHOW_PLAYER`)
- Queue manipulation: `addToQueue`, `setToQueue`, `move`, `removeFromQueue`, `clearQueue`
- Service lifecycle: `startService`, `stopService`, `destroy`

### `QueueRepository` / `QueueRepositoryImpl`
Manages the persistent playback queue backed by `QueueItemDao`.
- `addToQueue(item)` — checks for duplicates; if the item is already queued, moves it to front; otherwise inserts it
- `addToQueue(topicId, authorId)` — replaces the entire queue with all lessons from a topic
- `move(from, to)` — reorders items using DAO-level position management
- `refreshQueueItem(id, referenceUuid)` — re-fetches a snip from `SnipDao` and updates the queue item's metadata (used when a snip is edited)

### `DownloadRepository` / `DownloadRepositoryImpl`
Manages offline audio downloads.
- `download(...)` — checks `DownloadDao` for existing state; resumes stopped downloads or starts new ones via `DownloadManager`
- `remove(...)` — removes the entry and deletes the file only if no other queue item shares the same audio path
- `removeAllDownloads()` — clears the entire `DownloadDao` and invokes `DownloadManager.clear()`

### `StorageRepository` / `StorageRepositoryImpl`
Thin wrapper over `StorageManager` that adds `DownloadRepository.removeAllDownloads()` coordination on `clearDownloads()`.

### `UserRepository` / `UserRepositoryImpl`
Reads the current user from `Preferences` and maps to the domain `User` model.

### `AppRepository` / `AppRepositoryImpl`
Exposes `getLang(): Flow<String?>` from `Preferences`.

---

## 8. Platform-Specific Implementations

KMP's `expect/actual` mechanism separates platform concerns. `commonMain` declares `expect` declarations; `androidMain` and `iosMain` provide `actual` implementations.

### Audio Player

**Interface:** `AudioPlayer` (`me.anasmusa.learncast.core.player`)  
**Factory:** `expect fun createAudioPlayer(audioPath: String, startPosition: Long): AudioPlayer`

| Aspect | Android | iOS |
|---|---|---|
| Class | `AndroidAudioPlayer` | `IosAudioPlayer` |
| Underlying engine | ExoPlayer (`Media3`) | AVPlayer via `AVPlayerDelegate` (Swift, bridged via KN interop) |
| Data source | `ProgressiveMediaSource.Factory` with a `CacheDataSourceFactory` that reads from both the playback and download `SimpleCache` instances | Custom `ResourceLoaderDelegate` in Swift that intercepts URL requests for caching and token injection |
| State mapping | `Player.Listener.onEvents` → `STATE_LOADING`, `STATE_PLAYING`, `STATE_PAUSED` | `AVPlayerCallback` → same constants |

### Player Controller (Full Playback Queue)

**Interface:** `PlayerController` (`me.anasmusa.learncast.core.player`)  
Implementations: `AndroidPlayerController`, `IosPlayerController`

This is a higher-level controller that manages a multi-item queue (not just a single audio file). On Android, it drives a foreground `PlaybackService` (Media3 `MediaSessionService`). On iOS, it wraps `IosAudioPlayer` and `AVPlayerDelegate`.

### Database Builder

| Platform | How `getDatabaseBuilder()` is implemented |
|---|---|
| Android | `Room.databaseBuilder()` with a custom `SQLiteDriver` that wraps a `FrameworkSQLiteDatabase`, giving the app control over the underlying `SQLiteDatabase` instance (injected via Koin) |
| iOS | `Room.databaseBuilder()` with `BundledSQLiteDriver` (from `sqlite-bundled`), pointing to the app's `NSDocumentDirectory` |

### DataStore (Preferences)

Both platforms use `DataStoreFactory.create()` with `OkioStorage` and `PreferenceSerializer`, but resolve the file path using their respective platform APIs (`context.filesDir` on Android, `NSDocumentDirectory` on iOS).

### HTTP Client Engine

| Platform | Engine | Notes |
|---|---|---|
| Android | `OkHttp` | Standard OkHttp engine with `HttpCache` using `CachingCacheStorage` |
| iOS | `Darwin` | NSURLSession-based Ktor engine with the same `HttpCache` setup |

### Notification Manager

**Interface:** `NotificationManager` (`me.anasmusa.learncast.core.notification`)

| Platform | Implementation | Mechanism |
|---|---|---|
| Android | `AndroidNotificationManager` | Subscribes/unsubscribes to a `"news"` topic via `FirebaseMessaging` |
| iOS | `NotificationManager.ios.kt` | iOS-specific push notification handling |

### Storage Manager

See [Section 5 — File & Cache Storage](#5-file--cache-storage) for the full breakdown.

### Download Manager

**Interface:** `DownloadManager` (`me.anasmusa.learncast.core.download`)

| Platform | Implementation |
|---|---|
| Android | `AndroidDownloadManager` — uses Media3's built-in download infrastructure with `DownloadService` and a `SimpleCache` backed by the `DIRECTORY_PODCASTS` external directory |
| iOS | Delegated to Swift's `IosDownloadManager` in the iOS lib target |

### Google Authentication

**Interface:** `GoogleAuthManager` (`me.anasmusa.learncast.core.google`)

| Platform | Implementation |
|---|---|
| Android | `AndroidGoogleAuthManager` — uses AndroidX `CredentialManager` with `GetGoogleIdOption` to obtain an ID token |
| iOS | Delegated to `IosGoogleAuthManager` in the iOS lib target |

---

## 9. ViewModels

All ViewModels extend `BaseViewModel<State, Intent, Event>`, which extends `androidx.lifecycle.ViewModel` (KMP). The pattern is **MVI** (Model-View-Intent): the UI sends `Intent`s in; the ViewModel updates `StateFlow<State>` and optionally emits one-shot `Event`s via a `Channel`. Platform UIs observe the `StateFlow` and collect the `eventsFlow`.

**Main package:** `me.anasmusa.learncast.ui`

### `BaseViewModel`

The base class provides:
- `abstract val state: StateFlow<State>` — current screen state
- `open fun handle(intent: Intent)` — entry point for user actions
- `protected suspend fun send(event: Event)` — emits a one-shot event on `Dispatchers.Main.immediate`
- `fun subscribe(scope, onEvent)` — convenience collector for platforms

---

### `AppViewModel`
**State:** `AppState(isLoggedIn: Boolean?)`

Bootstraps the app by observing `AuthRepository.isLoggedIn()`. On first login, starts the player service. Continuously runs `SyncRepository.sync(finishWhenDrained = false)` in a background coroutine for the lifetime of the app.

**Events:** `ShowHomeScreen`, `ShowLoginScreen`

---

### `LoginViewModel`
**State:** `LoginState(isLoading: Boolean)`  
**Intents:** `LoginWithTelegram(hash)`, `LoginWithGoogle`  
**Events:** `ShowError(message)`

Delegates to `AuthRepository` for both login methods and surfaces errors as one-shot events.

---

### `HomeViewModel`
**State:** `HomeState(searchQuery, inSearchMode, selectedFilter, lessons: Flow<PagingData<Lesson>>)`  
**Intents:** `UpdateSearchQuery`, `SelectFilter(Filters)`, `AddToQueue(Lesson)`

Debounces search and filter changes (500 ms) and reloads `LessonRepository.page(...)` with the appropriate parameters. Supports filters: `Latest`, `InProgress`, `Downloads`, `MostSnipped`, `Favourite`.

---

### `AuthorListViewModel`
**State:** `AuthorListState(searchQuery, inSearchMode, authors: Flow<PagingData<Author>>)`  
**Intents:** `UpdateSearchQuery(query, inSearchMode)`

Debounces search input (500 ms) and reloads `AuthorRepository.page(query)`.

---

### `AuthorViewModel`
**State:** `AuthorState(selectedTabIndex, lessons: Flow<PagingData<Lesson>>, topics: Flow<PagingData<Topic>>)`  
**Intents:** `SelectTab(index)`, `LoadLessons(authorId)`, `LoadTopics(authorId)`, `AddToQueue(Lesson)`

Lazily loads lessons and topics for a specific author (only fetches each list once). Delegates queue add to `PlayerRepository`.

---

### `TopicListViewModel`
**State:** `TopicListState(searchQuery, inSearchMode, topics: Flow<PagingData<Topic>>)`  
**Intents:** `UpdateSearchQuery(query, inSearchMode)`

Debounced search feeding `TopicRepository.page(query, null)`.

---

### `TopicViewModel`
**State:** `TopicState(isLoading, lessons: Flow<PagingData<Lesson>>)`  
**Intents:** `Load(topicId, authorId)`, `PlayAll(topicId, authorId)`, `AddToQueue(Lesson)`

`PlayAll` loads all lessons for the topic from `QueueRepository`, then calls `PlayerRepository.setToQueue(items, playWhenReady = true)`.

---

### `SearchViewModel`
**State:** `SearchState(searchQuery, selectedTab, lessons, topics)`  
**Intents:** `Load(authorId, topicId)`, `UpdateSearchQuery(query)`, `SelectTab(value)`, `AddToQueue(Lesson)`

Handles a dual-tab search (Lessons / Topics), debouncing the query and scoping the search to an optional author or topic context.

---

### `PlayerViewModel`
**State:** `PlayerState(isLoading, currentPlaying: QueueItem?, playbackState, currentPositionMs, queuedCount, snipCount)`  
**Intents:** `TogglePlaybackState`, `Pause`, `SeekTo(value)`, `Seek(forward)`, `Download`, `RemoveDownload`, `ToggleCompletedState`, `ToggleFavourite`, `LoadSnipCount`, `DeleteSnip`, `Refresh`  
**Events:** `ShowPlayer`

Central ViewModel for the audio player. Observes `PlayerRepository` streams for current item, playback state, position, and queue count. Handles all player actions and user progress mutations (via `OutboxRepository`). `ToggleCompletedState` automatically advances to the next queue item when marking a lesson complete.

---

### `QueueViewModel`
**Package:** `me.anasmusa.learncast.ui.player.queue`

Manages the playback queue screen — reordering items, removing items, and observing the current queue state via `PlayerRepository`.

---

### `PlayerSnipViewModel`
**Package:** `me.anasmusa.learncast.ui.player.snip`

Handles snip-related actions while the player is open: displaying the snip list for the current lesson and providing navigation context.

---

### `SnipListViewModel`
**State:** `SnipListState(searchQuery, inSearchMode, snips: Flow<PagingData<Snip>>)`  
**Intents:** `UpdateSearchQuery`, `AddToQueue(Snip)`

Debounced search over all snips. Adding a snip to the queue maps it to a `QueueItem` and calls `PlayerRepository.addToQueue()`.

---

### `SnipEditViewModel`
**State:** `SnipEditState(playbackState, isLoading, currentPositionMs)`  
**Intents:** `Init(clientSnipId, audioPath, startPosition)`, `Start(from, to)`, `Stop`, `Save(...)`  
**Events:** `ShowError`, `OnSnipLoaded(note)`, `Finish`

Creates a standalone `AudioPlayer` instance (separate from the main player) for previewing the snip range. Polls position every second while playing and pauses at the `to` timestamp. Saves via `SnipRepository.save()`.

---

### `ProfileViewModel`
**State:** `ProfileState(isLoading, user: User?, isQueueEmpty)`  
**Intents:** `Logout`  
**Events:** `ShowError`

Loads the user from `UserRepository` and observes whether the player queue is empty (used to show/hide the bottom player bar).

---

### `StorageViewModel`
**State:** `StorageState(isLoading, cacheSize: String?, downloadSize: String?)`  
**Intents:** `ClearCache`, `ClearDownloads`

Shows human-readable storage sizes (MB/GB). Clearing stops the player service first, invokes the appropriate `StorageRepository` method, then restarts the service.

---

## 10. Dependency Injection

**Library:** [Koin](https://insert-koin.io/) — `koin.core` in `commonMain`, `koin.android` in `androidMain`  
**Main package:** `me.anasmusa.learncast`

The DI graph is organised into layered modules and scopes:

| Module | File | Provides |
|---|---|---|
| Core module (shared) | `core/KoinModules.shared.kt` | `NotificationManager`, `DownloadManager`, `PlayerController` |
| Core module (Android) | `core/KoinModules.android.kt` | ExoPlayer-specific bindings, `DatabaseProvider`, `SimpleCache` instances for playback and download, `GoogleAuthManager` |
| Core module (iOS) | `core/KoinModules.ios.kt` | iOS-specific core bindings |
| Data module (shared) | `data/KoinModules.shared.kt` | `Preferences`, `AppDatabase`, all DAOs, `DBConnection`, `StorageManager`, `HttpClient`, `TokenManager`, all services, all repositories |
| Data module (Android) | `data/KoinModules.android.kt` | `SQLiteDatabase`, Android-specific `StorageManager`, `CachingCacheStorage` |
| Data module (iOS) | `data/KoinModules.ios.kt` | iOS cache indexes and storage manager |
| UI module | `ui/KoinModules.kt` | All ViewModels |

**Scopes used:**

- `AppScope` — application-level singleton scope
- `AuthorizedUserScope` — created on login, closed on logout; scopes `HomeViewModel`, `SnipListViewModel`, `ProfileViewModel` so they are recreated on each login
- `PlaybackCacheScope` / `DownloadCacheScope` — control lifecycle of the ExoPlayer cache instances

---

## 11. Testing

**Framework:** [Kotest](https://kotest.io/) (multiplatform) with `kotest-engine` and `kotest-assertions`  
**Network mocking:** `ktor-client-mock`

**Test source sets:**

| Source Set | Scope |
|---|---|
| `commonTest` | Shared DAO tests and service tests |
| `androidHostTest` | JVM-based tests (Kotest + JUnit 5) |
| `androidDeviceTest` | On-device instrumented tests (Pixel 2, API 30 managed device) |
| `iosTest` | iOS-specific DB tests |

**What is tested:**

- All DAOs (`AuthorDaoTest`, `LessonDaoTest`, `SnipDaoTest`, `QueueItemDaoTest`, `OutboxDaoTest`, `DownloadDaoTest`, `PagingStateDaoTest`, `TopicDaoTest`) — via an in-memory Room database helper in `TestDatabase.kt`
- All network services (`AuthServiceTest`, `LessonServiceTest`, `SnipServiceTest`, `AuthorServiceTest`, `TopicServiceTest`) — using `MockEngine` from `ktor-client-mock`
- `TokenManagerTest` — covers the concurrent refresh / mutex behaviour

---

## 12. Package Structure

```
shared/src/
├── commonMain/kotlin/me/anasmusa/learncast/
│   ├── core/
│   │   ├── download/           # DownloadManager interface
│   │   ├── google/             # GoogleAuthManager interface
│   │   ├── notification/       # NotificationManager interface
│   │   ├── paging/             # CommonPager, PagingConfig
│   │   ├── platform/           # Platform utility (isAndroid)
│   │   ├── player/             # AudioPlayer, PlayerController interfaces
│   │   └── resource/           # String resource abstraction
│   ├── data/
│   │   ├── local/
│   │   │   ├── db/             # Room: AppDatabase, entities, DAOs
│   │   │   ├── preference/     # DataStore + Protobuf preferences
│   │   │   └── storage/        # StorageManager interface
│   │   ├── mapper/             # Entity ↔ domain model mappers
│   │   ├── model/              # Domain data classes
│   │   ├── network/            # Ktor client, services, token management
│   │   ├── paging/             # RemoteMediator implementations
│   │   └── repository/
│   │       ├── abstraction/    # Repository interfaces
│   │       └── implementation/ # Repository implementations
│   └── ui/
│       ├── auth/               # LoginViewModel
│       ├── author/             # AuthorListViewModel, AuthorViewModel
│       ├── home/               # HomeViewModel
│       ├── player/             # PlayerViewModel, QueueViewModel, PlayerSnipViewModel
│       ├── profile/            # ProfileViewModel, StorageViewModel
│       ├── snip/               # SnipListViewModel, SnipEditViewModel
│       ├── topic/              # TopicListViewModel, TopicViewModel
│       ├── AppViewModel.kt
│       ├── BaseViewModel.kt
│       └── SearchViewModel.kt
├── androidMain/kotlin/         # Android actual implementations
│   └── me/anasmusa/learncast/
│       ├── core/               # AndroidAudioPlayer, AndroidPlayerController, PlaybackService
│       └── data/               # Room DB builder, DataStore, AndroidStorageManager, OkHttp client
└── iosMain/kotlin/             # iOS actual implementations
    └── me/anasmusa/learncast/
        ├── core/               # IosAudioPlayer, IosPlayerController, AVPlayer bridge, cache
        └── data/               # Room DB builder (BundledSQLite), DataStore, IosStorageManager, Darwin client
```