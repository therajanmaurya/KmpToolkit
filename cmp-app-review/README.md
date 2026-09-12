# cmp-app-review

Ask the user to review your app, from `commonMain`, on every Kotlin Multiplatform target.

> **Stable API.** No opt-in annotation.

One call in shared code resolves to the right thing per platform — a native in-app prompt where the
OS has one, your store listing everywhere else, opened through [`cmp-open-url`](../cmp-open-url).

## Install

```toml
cmp-app-review = { module = "io.github.mobilebytelabs:cmp-app-review", version.ref = "kmptoolkit" }
```

```kotlin
commonMain.dependencies { implementation(libs.cmp.app.review) }
```

`cmp-open-url` comes with it — the store fallback is built on it, so you do not add it separately.

## Use

```kotlin
// once at startup, in commonMain
AppReview.configure(
    StoreListing(
        appStoreId = "1234567890",                  // iOS, macOS, tvOS, watchOS
        microsoftStoreProductId = "9NBLGGH4NNS1",   // Windows
        webUrl = "https://example.com/app",         // desktop, Linux, browser
    ),
)

// anywhere in shared code
when (val result = AppReview.requestReview()) {
    is AppReviewResult.NativeFlowRequested -> Unit            // the OS took it from here
    is AppReviewResult.StoreOpened         -> Unit            // sent to the listing
    is AppReviewResult.NoStoreConfigured   -> Unit            // you have a config gap
    is AppReviewResult.Failed              -> log(result.message)
}
```

`playStorePackage` is optional — Android reads the running app's package when you omit it.

## Dependency injection

Binds like every other platform capability — no Activity, no arguments:

```kotlin
val platformModule = module {
    single<UrlLauncher> { UrlLauncherImpl() }
    single<ShareManager> { ShareManagerImpl() }
    single<AppReviewManager> { AppReviewManagerImpl() }   // ← here
}
```

or register the module directly: `startKoin { modules(appReviewModule) }`.

Store ids do **not** go through DI. They are deployment data that differ per flavour and per
white-label fork, so they live wherever your build already keeps app identity — a generated
BuildKonfig constant, an app-profile YAML — and are applied once:

```kotlin
AppReview.configure(
    StoreListing(appStoreId = BuildKonfig.APP_STORE_ID, webUrl = BuildKonfig.APP_WEB_URL),
)
```

Order does not matter: the manager resolves the listing when a review is requested, not when it is
constructed, so registering the module before `configure()` is fine.

## What each target does

| Target | Route |
|---|---|
| Android | Play In-App Review → `market://details?id=…` |
| iOS / macOS | `SKStoreReviewController` → store review composer |
| tvOS / watchOS | store listing (watchOS hands it to the paired iPhone) |
| Windows | `ms-windows-store://review/` → web listing |
| JVM · Linux · JS · wasmJs | web listing in the browser |
| wasmWasi | `NoStoreConfigured` — no store, no browser, no user |

**`NativeFlowRequested` does not mean a prompt appeared.** Neither Play nor StoreKit tells the app
whether anything rendered or what the user did, and both silently no-op once their quota is spent.
Never gate a reward on it, and never show "thanks for rating!" in response. If you need a button that
reliably goes somewhere, call `openStoreListing()`.

## Testing

```kotlin
val review = FakeAppReviewManager()
repeat(3) { viewModel.onExportSucceeded() }
assertEquals(1, review.requestCount)   // asked once, not three times
```

The gating logic is yours and is the part worth testing; the prompt itself is unobservable by design.
Inject `AppReviewManager` where you want that, or `AppReview.configure(fake)` for the global path.

## Requirements

- **Android** — Play In-App Review works only in builds installed by Google Play. Sideloaded and
  debug builds get a failure, which falls through to `market://`. No manual setup: a `ContentProvider`
  captures the app context and an `ActivityLifecycleCallbacks` tracks the Activity the flow needs.
- **iOS 10.3+ / macOS 10.14+** for `SKStoreReviewController`.
