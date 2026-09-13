# Consuming `cmp-app-review`

Integration guide for an app adopting the module — written against the shape a KMP app with a
`platformModule` and an app-profile already has.

---

## 1. Dependency

```toml
# gradle/libs.versions.toml
cmp-app-review = { module = "io.github.mobilebytelabs:cmp-app-review", version.ref = "kmptoolkit" }
```

```kotlin
commonMain.dependencies { implementation(libs.cmp.app.review) }
```

`cmp-open-url` comes with it as an `api` dependency — the store fallback is built on it — so you do
not declare it separately. If you already depend on it, nothing changes.

---

## 2. Where the store ids live

They are deployment data, not code: they differ per flavour and per white-label fork, and a fork
should change them without touching Kotlin. Keep them wherever your build already keeps app identity.

With an app-profile YAML as the source of truth:

```yaml
# app-profile/app.yaml
identity:
  app_id: com.example.app          # already the SoT for the Android package

store:
  app_store_id: "1234567890"       # Apple numeric id — iOS, macOS, tvOS, watchOS
  microsoft_store_id: "9NBLGGH4NNS1"
  web_url: https://example.com/app # desktop, Linux, browser
```

Surface those as build constants the way your project already does for other profile values (a
`BuildKonfig` field, a generated object, `syncForkConfig`), then read them in DI.

**`playStorePackage` is optional** — Android reads the running application's package, which your
`identity.app_id` already drives. Only set it to point at a *different* listing.

---

## 3. DI setup

Registering the module **is** the configuration step. There is nothing to call at startup.

```kotlin
startKoin {
    modules(
        platformModule,
        appReviewModule(
            StoreListing(
                appStoreId = BuildKonfig.APP_STORE_ID,
                microsoftStoreProductId = BuildKonfig.MS_STORE_ID,
                webUrl = BuildKonfig.APP_WEB_URL,
            ),
        ),
    )
}
```

Or keep every capability in one module — `module { }` bodies run eagerly, so this is equivalent:

```kotlin
val platformModule = module {
    AppReview.configure(StoreListing(appStoreId = BuildKonfig.APP_STORE_ID))

    single<UrlLauncher> { UrlLauncherImpl() }
    single<ShareManager> { ShareManagerImpl() }
    single<IntentManager> { IntentManagerImpl() }
    single<AppUpdateManager> { AppUpdateManagerImpl() }
    single<AppReviewManager> { AppReviewManagerImpl() }
}
```

`AppReviewManagerImpl()` takes **no Activity and no listing**. Order does not matter either: the
listing is resolved when a review is requested, not when the manager is constructed.

> ⚠️ `appReviewModule()` with **no** listing applies `StoreListing.None`, clearing anything configured
> earlier. DI setup is the source of truth by design. Pass the listing to the module, or configure
> inside your own module — not one then the other.

---

## 4. Calling it

```kotlin
class SettingsViewModel(private val review: AppReviewManager) {
    fun onRateTapped() = viewModelScope.launch { review.requestReview() }
}
```

or from anywhere in shared code, with no injection:

```kotlin
AppReview.requestReview()
```

Branch on capability if you gate behind your own pre-prompt — worth showing one where a native prompt
exists (the OS quota is precious), pointless where only the store listing does:

```kotlin
if (AppReview.capabilities.nativeInAppReview) showEnjoyingTheAppSheet() else AppReview.openStoreListing()
```

---

## 5. Replacing a hand-written wrapper

If you already have a `core-base/platform/review/AppReviewManager` taking an `Activity`, it can become
a delegate or be deleted:

| Yours | This module |
|---|---|
| `promptForReview()` | `requestReview()` — native flow, falls back to the store |
| `promptForCustomReview()` | `openStoreListing()` — always navigates |
| `AppReviewManagerImpl(activity)` | `AppReviewManagerImpl()` — the Activity is tracked internally |

That Activity parameter is usually why the binding sits outside the platform module. It no longer has
to: a `ContentProvider` captures the application context and an `ActivityLifecycleCallbacks` tracks the
resumed Activity, held weakly and cleared on pause.

---

## 6. What each target does

| Target | Route |
|---|---|
| Android | Play In-App Review → `market://details?id=…` |
| iOS / macOS | `SKStoreReviewController` → store review composer |
| tvOS / watchOS | store listing (watchOS hands it to the paired iPhone) |
| Windows | `ms-windows-store://review/` → web listing |
| JVM · Linux · JS · wasmJs | web listing in the browser |
| wasmWasi | `NoStoreConfigured` — no store, no browser, no user |

---

## 7. Things that will bite you

**`NativeFlowRequested` does not mean a prompt appeared.** Neither Play nor StoreKit tells the app
whether anything rendered or what the user did, and both silently do nothing once their quota is
spent. Never gate a reward on it and never show "thanks for rating!" in response. If you need a
control that reliably goes somewhere, call `openStoreListing()`.

**Play In-App Review only works in a Play-installed build.** Sideloaded and debug builds get a failure
from the API, which falls through to `market://`. Testing the real prompt requires an internal-testing
track build.

**A missing listing is a configuration gap, not a platform limit.** `NoStoreConfigured` means this
target has no native API *and* nothing in `StoreListing` applies to it. Add a `webUrl` at minimum — it
covers every fallback target.

---

## 8. Testing

Your gating logic is the part worth testing; the prompt itself is unobservable by design.

```kotlin
val review = FakeAppReviewManager()
repeat(3) { viewModel.onExportSucceeded() }

assertEquals(1, review.requestCount)   // asked once, not three times
```

`FakeAppReviewManager` ships in the main artifact — no extra test dependency. Set
`capabilities = AppReviewCapabilities.StoreOnly` to exercise the branch a desktop or tvOS build takes.
For the global entry point, `AppReview.configure(fake)` then `AppReview.reset()` in teardown.
