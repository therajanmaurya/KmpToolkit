# cmp-open-url

Cross-platform URL opening for Kotlin Multiplatform — browser, email, maps, phone, SMS, and custom URI schemes across all 14 KMP targets.

[![Maven Central](https://img.shields.io/maven-central/v/io.github.mobilebytelabs/kmp-open-url)](https://central.sonatype.com/artifact/io.github.mobilebytelabs/kmp-open-url)

---

## Installation

```kotlin
// build.gradle.kts
implementation("io.github.mobilebytelabs:kmp-open-url:<version>")
```

No additional setup required on Android — the library auto-initialises via a `ContentProvider` that is merged into your manifest automatically.

---

## Quick Start

```kotlin
import com.mobilebytelabs.kmptoolkit.openurl.*

// Open any URL with the default system handler
openUrl("https://github.com/MobileByteLabs")      // → browser

// Force browser (bypass app-association rules)
openInBrowser("https://accounts.google.com/...")  // → always browser

// Open with a specific app category
openWithApp("mailto:hello@example.com", AppHint.EMAIL)   // → Gmail / Apple Mail
openWithApp("geo:37.7749,-122.4194", AppHint.MAPS)       // → Google Maps / Apple Maps
openWithApp("tel:+1-800-555-0100", AppHint.PHONE)        // → phone dialer
openWithApp("sms:+1-800-555-0100", AppHint.SMS)          // → messaging app

// Android-only: explicit package name
openWithApp("https://maps.google.com", AppHint.Custom("com.google.android.apps.maps"))

// Check before opening
if (canOpen("myapp://deep-link/home")) {
    openUrl("myapp://deep-link/home")
}
```

---

## API Reference

### `openUrl(url: String): Boolean`

Opens the URL using the platform's default handler. Returns `true` if accepted, `false` otherwise. **Never throws.**

### `openInBrowser(url: String): Boolean`

Forces the URL to open in the system web browser, bypassing app-association rules. **Never throws.**

### `openWithApp(url: String, appHint: AppHint): OpenUrlResult`

Opens the URL with a preferred app category hint. Returns an `OpenUrlResult` for richer error handling. **Never throws.**

### `canOpen(url: String): Boolean`

Returns `true` if the platform can handle the URL without actually opening it. **Never throws.**

---

## AppHint

| Value | Effect |
|-------|--------|
| `AppHint.DEFAULT` | Let the OS choose the best handler |
| `AppHint.BROWSER` | Force system browser |
| `AppHint.EMAIL` | Prefer email client (`mailto:`) |
| `AppHint.MAPS` | Prefer maps app (`geo://`, `maps:`) |
| `AppHint.PHONE` | Prefer phone dialer (`tel:`) |
| `AppHint.SMS` | Prefer SMS app (`sms:`) |
| `AppHint.Custom(pkg)` | Android only — explicit package name |

---

## OpenUrlResult

```kotlin
sealed class OpenUrlResult {
    object Success                      // URL was opened
    object NoHandler                    // No app can handle it
    data class Error(val message: String) // Unexpected error
}

// Exhaustive matching
when (result) {
    OpenUrlResult.Success    -> showConfirmation()
    OpenUrlResult.NoHandler  -> showFallbackUI()
    is OpenUrlResult.Error   -> log(result.message)
}
```

---

## Platform Support

| Platform | `openUrl` | `openInBrowser` | `openWithApp` | `canOpen` |
|----------|:---------:|:---------------:|:-------------:|:---------:|
| Android | ✅ | ✅ | ✅ (all hints) | ✅ |
| iOS | ✅ | ✅ | ✅ | ✅ |
| macOS | ✅ | ✅ | ✅ | ✅ |
| tvOS | ✅¹ | ✅¹ | ✅¹ | ✅ (asks the OS) |
| watchOS | ✅² | ✅² | ✅² | ✅ (scheme list) |
| JVM Desktop | ✅ | ✅ | ✅ | ✅ |
| JS Browser | ✅ | ✅ | ✅ | ✅ |
| JS Node | ✅ (via `open`) | ✅ | ✅ | ⚠️ |
| Linux Native | ✅ (xdg-open) | ✅ | ✅ | ✅ |
| Windows Native | ✅ (ShellExecuteW) | ✅ | ✅ | ✅ |
| wasmJs | ✅ | ✅ | ✅ | ✅ |
| wasmWasi | ✅³ | ✅³ | ✅³ | ✅ |


¹ **tvOS** — `UIApplication.openURL`. tvOS ships no *web browser*, so `https://` links usually have
no handler, but App Store links, other apps' custom schemes and system URLs open fine. `canOpen`
asks the OS per URL rather than assuming.
² **watchOS** — `WKExtension.openSystemURL`. `http`/`https` hand off to the paired iPhone; `tel:`,
`sms:` and `mailto:` are handled on the watch. Other schemes have no handler and `canOpen` says so.
³ **wasmWasi** — passed to the embedding host through `WasiUrlOpener`, or echoed to stdout when no
handler is registered. See below.

### These three used to be blanket ❌

All three returned `false` / `NoHandler` unconditionally, above comments asserting the platform
"has no browser or URL-opening concept". That was wrong on all counts:

- **watchOS** — the sibling `cmp-intent-launcher` module had been calling
  `WKExtension.openSystemURL` on watchOS the whole time.
- **tvOS** — `UIApplication.canOpenURL` and `openURL` both exist there (verified by compiling
  against them). No browser ≠ no URL handling.
- **wasmWasi** — no display, but "open this URL" is a *request*, and a sandbox can always pass a
  request to its host.

```kotlin
WasiUrlOpener.handler = { url, hint -> myHost.openInSystemBrowser(url); true }

// or with no setup at all — buffered and echoed to stdout:
UrlLauncherImpl().open("https://example.com")
WasiUrlOpener.drain()   // ["https://example.com"]
```

---

## Injecting it — `UrlLauncher`

`openUrl(...)` and friends are top-level `expect fun`s, so code calling them cannot be faked or
decorated. `UrlLauncher` is the same capability behind an injectable type:

```kotlin
class ArticleViewModel(private val urls: UrlLauncher) : ViewModel() {
    fun openSource(url: String) {
        if (urls.canOpen(url)) urls.open(url)   // ask before offering
    }
}
```

With Koin: `startKoin { modules(openUrlModule) }`. Without: construct `UrlLauncherImpl()` and
register it against `UrlLauncher` yourself.

`FakeUrlLauncher` ships in the main artifact, and can simulate a platform that refuses links so a
test proves the UI hides the affordance rather than rendering a dead one:

```kotlin
val locked = FakeUrlLauncher(canOpenPredicate = { false })
ArticleViewModel(locked).openSource("https://example.com")
assertTrue(locked.opened.isEmpty())
assertEquals(listOf("https://example.com"), locked.refused)
```

---

## Important Notes

- **Never throws** — all functions catch exceptions internally and return `false` / `NoHandler`.
- **No URL encoding** — the library passes URLs as-is; encoding is the caller's responsibility.
- **Outgoing only** — this library does not handle incoming deep links into your app.
- **`AppHint.Custom` fallback** — if the specified Android package is not installed, the library retries with `AppHint.DEFAULT` before returning `NoHandler`.
