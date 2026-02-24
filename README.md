<p align="center">
  <img src="/assets/images/logo.png" alt="App Logo" width="120" />
</p>

<h1 align="center">LearnCast</h1>

<p align="center">
  <b>LearnCast</b> is an audio learning platform — a podcast-style app for structured educational content. Users can browse lessons by author and topic, build a playback queue, create timestamped snips from any lesson, and listen offline with downloaded audio.
</p>

<p align="center">
  We built this app solely for learning purposes. All podcast materials in the app are sourced from the <a href="https://www.kaggle.com/datasets/listennotes/all-podcast-episodes-published-in-december-2017" target="_blank">ListenNotes Kaggle dataset</a>.
</p>

<p align="center">
  The project is built with <b>Kotlin Multiplatform (KMP)</b>. Business logic, data, networking, and ViewModels are written once in the `shared` module and consumed by native Android (Jetpack Compose) and iOS (SwiftUI) UIs.
</p>

### Android App
<div>
  <img src="/assets/images/android_home.webp" width="18%" alt="Home"/>
  <img src="/assets/images/android_player.webp" width="18%" alt="Player"/>
  <img src="/assets/images/android_queue.webp" width="18%" alt="Queue"/>
  <img src="/assets/images/android_snip_create.webp" width="18%" alt="Snip Create"/>
  <img src="/assets/images/android_snip_list.webp" width="18%" alt="Snips"/>
</div>

[See All](android/README.md#app)

### iOS App
<div>
  <img src="/assets/images/ios_home.webp" width="18%" alt="Home"/>
  <img src="/assets/images/ios_player.webp" width="18%" alt="Player"/>
  <img src="/assets/images/ios_queue.webp" width="18%" alt="Queue"/>
  <img src="/assets/images/ios_snip_create.webp" width="18%" alt="Snip Create"/>
  <img src="/assets/images/ios_snip_list.webp" width="18%" alt="Snips"/>
</div>

[See All](ios/README.md#app)

---

## 📋 Table of Contents

1. [Project Structure](#1-project-structure)
2. [Kotlin Versions & Key Dependencies](#2-kotlin-versions--key-dependencies)
3. [Modules at a Glance](#3-modules-at-a-glance)
4. [String Resources & Localization](#4-string-resources--localization)
5. [CI / Fastlane](#5-ci--fastlane)
6. [Code Quality & Git Hooks](#6-code-quality--git-hooks)
7. [`shared/` — KMP Business Logic → README](shared/README.md)
   - [1. Module Overview](shared/README.md#1-module-overview)
   - [2. Tech Stack at a Glance](shared/README.md#2-tech-stack-at-a-glance)
   - [3. Local Database](shared/README.md#3-local-database)
   - [4. Preferences / Settings Storage](shared/README.md#4-preferences--settings-storage)
   - [5. File & Cache Storage](shared/README.md#5-file--cache-storage)
   - [6. Networking](shared/README.md#6-networking)
   - [7. Repositories](shared/README.md#7-repositories)
   - [8. Platform-Specific Implementations](shared/README.md#8-platform-specific-implementations)
   - [9. ViewModels](shared/README.md#9-viewmodels)
   - [10. Dependency Injection](shared/README.md#10-dependency-injection)
   - [11. Testing](shared/README.md#11-testing)
   - [12. Package Structure](shared/README.md#12-package-structure)
8. [`android/` — Android UI → README](android/README.md)
   - [1. Module Overview](android/README.md#1-module-overview)
   - [2. Tech Stack at a Glance](android/README.md#2-tech-stack-at-a-glance)
   - [3. Module: `learncast` (App)](android/README.md#3-module-learncast-app)
   - [4. Module: `lib` (UI Library)](android/README.md#4-module-lib-ui-library)
   - [5. Application Bootstrapping](android/README.md#5-application-bootstrapping)
   - [6. Navigation](android/README.md#6-navigation)
   - [7. Theme & Design System](android/README.md#7-theme--design-system)
   - [8. Screens](android/README.md#8-screens)
   - [9. Reusable Components](android/README.md#9-reusable-components)
   - [10. Notifications & Background Services](android/README.md#10-notifications--background-services)
   - [11. Localization (String Resources)](android/README.md#11-localization-string-resources)
   - [12. Build Configuration & Variants](android/README.md#12-build-configuration--variants)
   - [13. Package Structure](android/README.md#13-package-structure)
9. [`ios/` — iOS UI → README](ios/README.md)
   - [1. Source Group Overview](ios/README.md#1-source-group-overview)
   - [2. Tech Stack at a Glance](ios/README.md#2-tech-stack-at-a-glance)
   - [3. Group: `learncast` (App)](ios/README.md#3-group-learncast-app)
   - [4. Group: `lib` (UI Library)](ios/README.md#4-group-lib-ui-library)
   - [5. Application Bootstrapping](ios/README.md#5-application-bootstrapping)
   - [6. Koin Integration](ios/README.md#6-koin-integration)
   - [7. Navigation](ios/README.md#7-navigation)
   - [8. Theme & Design System](ios/README.md#8-theme--design-system)
   - [9. Screens](ios/README.md#9-screens)
   - [10. Reusable Components](ios/README.md#10-reusable-components)
   - [11. Audio Playback Engine](ios/README.md#11-audio-playback-engine)
   - [12. Download Manager](ios/README.md#12-download-manager)
   - [13. Localization (String Resources)](ios/README.md#13-localization-string-resources)
   - [14. Build Configuration](ios/README.md#14-build-configuration)
   - [15. Package Structure](ios/README.md#15-package-structure)

---

## 1. Project Structure

```
learncast/
├── shared/          # KMP module — business logic, data, ViewModels (Android + iOS)
├── android/
│   ├── learncast/   # Android application entry point
│   └── lib/         # Android UI library (Jetpack Compose screens, components, theme)
├── ios/
│   ├── learncast/   # iOS application entry point
│   └── lib/         # iOS UI library (SwiftUI screens, components, theme)
├── buildSrc/        # Shared build logic and convention plugins
├── gradle/
│   └── libs.versions.toml   # Version catalog — single source of truth for all dependency versions
└── fastlane/        # CI automation (Firebase Distribution + Play Store deployment)
```

### Gradle modules

| Module | Plugin | Description |
|---|---|---|
| `:shared` | `kotlin.multiplatform` | All KMP source sets: `commonMain`, `androidMain`, `iosMain` |
| `:android:learncast` | `com.android.application` | Android app entry point |
| `:android:lib` | `com.android.library` | Android UI library |

The iOS source groups (`ios/learncast`, `ios/lib`) are not Gradle modules — they are compiled by Xcode using the `Shared.framework` produced by `:shared`.

---

## 2. Kotlin Versions & Key Dependencies

| Dependency | Version |
|---|---|
| Kotlin | 2.3.0 |
| AGP (Android Gradle Plugin) | 9.0.0 |
| KSP | 2.3.4 |
| SKIE | 0.10.9 |
| Ktor | 3.4.0 |
| Koin | 4.1.1 |
| Room | 2.8.4 |
| Paging | 3.4.1 |
| Media3 | 1.9.0 |
| Jetpack Compose BOM | 2026.01.00 |
| Material3 | 1.5.0-alpha12 |
| Coil | 3.3.0 |
| Haze | 1.7.1 |
| Firebase BOM | 34.8.0 |
| Coroutines | 1.10.2 |
| kotlinx-datetime | 0.7.1 |
| kotlinx-serialization-json | 1.9.0 |
| Kotest | 6.1.1 |
| Napier (logging) | 2.7.1 |

**Android SDK targets:** min 26 · compile 36 · target 36

All versions are declared in `gradle/libs.versions.toml` (Gradle version catalog) and referenced throughout all `build.gradle.kts` files via the `libs.*` type-safe accessor.

---

## 3. Modules at a Glance

### `shared`

The heart of the project. Contains all source sets compiled for Android and iOS:

- `commonMain` — ViewModels, repositories, use cases, network (Ktor), database (Room / SQLite), paging (Paging 3), DI graph (Koin), domain models, and string resources
- `androidMain` — Android-specific implementations (ExoPlayer delegate, `AndroidDownloadService`, WorkManager sync worker)
- `iosMain` — iOS-specific Ktor engine (`Darwin`), SQLite bundled driver

SKIE (`co.touchlab.skie`) is applied to the `:shared` module to generate Swift-friendly wrappers for Kotlin flows, sealed classes, and suspend functions, making the shared API ergonomic to consume from SwiftUI.

### `android/learncast` + `android/lib`

Native Android UI built with Jetpack Compose. `learncast` is the thin application module; `lib` contains all screens, components, navigation, and the theme. See the [Android README](android/README.md) for full details.

### `ios/learncast` + `ios/lib`

Native iOS UI built with SwiftUI. `learncast` is the thin app target; `lib` contains all screens, components, the AVFoundation player engine, the background download manager, and the theme. See the [iOS README](ios/README.md) for full details.

---

## 4. String Resources & Localization

String resources are defined once in `shared/src/commonMain/resources/` and shared across all three modules:

| File | Content |
|---|---|
| `strings.xml` | English strings |
| `strings-uz.xml` | Uzbek strings |

Two Gradle tasks copy these files into the platform-specific locations before each build:

| Task | Defined in | Copies to | Triggered by |
|---|---|---|---|
| `copyStringsToAndroid` | `android/lib/build.gradle.kts` | `android/lib/src/main/assets/` | every Android build (`preBuild`) |
| `copyStringsToIos` | `shared/build.gradle.kts` | `ios/Resources/` | every `embedAndSign` step |

At runtime, `Resource.shared.setLocale(locale:)` loads the appropriate XML file, and all UI strings are resolved via the `Strings` constants object (e.g. `Strings.shared.HOME.string()`).

---

## 5. CI / Fastlane

**File:** `fastlane/Fastfile`

Two lanes are defined for the Android platform:

| Lane | Command | Description |
|---|---|---|
| `distribute` | `bundle exec fastlane distribute` | Assembles a `release` APK with `environment=dev` and uploads it to Firebase App Distribution. Automatically builds a changelog from PR commits (on `pull_request` events) or push commits (on `push` events) using the GitHub API |
| `deploy` | `bundle exec fastlane deploy` | Assembles a release APK and uploads it to Google Play Store via `upload_to_play_store` |

The `versionNameSuffix` Gradle property is set by the `distribute` lane to the sanitised branch name (for PRs) or `"dev"` (for push events), so testers can identify builds by branch.

Firebase App Distribution credentials are read from `firebase_distribution.json` (not committed) and `APP_ID` / `GITHUB_TOKEN` environment variables.

---

## 6. Code Quality & Git Hooks

A `pre-commit` hook is installed automatically before every Android build via the `installGitHook` Gradle task registered in the root `build.gradle.kts`. It runs on every `git commit` and enforces formatting and linting for both Kotlin and Swift files in the staged changeset.

### Kotlin (ktlint)

1. Runs `./gradlew ktlintFormat -Pincludes=<changed files>` — auto-formats staged Kotlin/KTS files
2. Re-stages the formatted files
3. Runs `./gradlew ktlintCheck -Pincludes=<changed files>` — fails the commit if any lint errors remain

### Swift (swift-format)

1. Runs `swift-format format <changed files> -i --configuration ios/.swift-format` — auto-formats staged Swift files
2. Re-stages the formatted files
3. Runs `swift-format lint <changed files> -s --configuration ios/.swift-format` — fails the commit if any lint violations remain

The hook exits with code `1` on any failure, blocking the commit until errors are resolved.

---

### 7. [`shared/` — KMP Business Logic → README](shared/README.md)

### 8. [`android/` — Android UI → README](android/README.md)

### 9. [`ios/` — iOS UI → README](ios/README.md)
