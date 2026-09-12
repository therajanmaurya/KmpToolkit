# cmp-share — Integration Guide

> `io.github.mobilebytelabs:cmp-share:3.2.11`

`cmp-share` is **zero-configuration** — no init call, no DI module, no nav destinations.
3 steps: add dependency, opt in, use.

---

## Step 1 — Add Gradle Dependency

### `gradle/libs.versions.toml`

```toml
[versions]
cmp-share = "3.2.11"

[libraries]
cmp-share = { module = "io.github.mobilebytelabs:cmp-share", version.ref = "cmp-share" }
```

### `shared/build.gradle.kts`

```kotlin
commonMain.dependencies {
    implementation(libs.cmp.share)
}
```

---

## Step 2 — Choose imperative or injected

No opt-in is required — cmp-share is stable. `@ExperimentalShareApi` survives as a deprecated
no-op purely so older call sites still compile; delete any `@OptIn(ExperimentalShareApi::class)`
and any `-opt-in=...ExperimentalShareApi` compiler flag you already have.

### Imperative — reach for `Share` directly

Fine for a one-off call site.

```kotlin
suspend fun onShare() {
    Share.text("Hello from KMP!")
}
```

### Injected — depend on `ShareManager` (recommended)

`Share` is an `expect object`: code calling it cannot be tested without a real share sheet, and
cannot be decorated. Depend on the interface instead.

```kotlin
// With Koin — shareModule ships with the library
startKoin { modules(shareModule, appModule) }

class ReportViewModel(private val share: ShareManager) : ViewModel() {
    fun export(uri: String) = viewModelScope.launch {
        share.shareFile(uri, "application/pdf", message = "Latest report")
    }
}
```

Not using Koin? Nothing else in cmp-share touches it — construct `ShareManagerImpl()` and register
it against `ShareManager` in your own container.

In tests, `FakeShareManager` ships in the main artifact:

```kotlin
val share = FakeShareManager()
ReportViewModel(share).export("file:///report.pdf")
assertIs<SharePayload.Multi>(share.recorded.single().payload)
```

---

## Step 3 — Use the API

### Share plain text

```kotlin
suspend fun shareText() {
    val result = Share.text("Check out KMP Toolkit!")
    if (result is ShareResult.Failed) logError(result.cause)
}
```

### Share a URL

```kotlin
val result = Share.url("https://github.com/mobilebytelabs/kmp-toolkit")
```

### Share an image

```kotlin
val result = Share.image(
    bytes    = pngBytes,
    mimeType = "image/png",
    filename = "screenshot.png"
)
```

### Share a file

```kotlin
val result = Share.file(
    bytes    = pdfBytes,
    mimeType = "application/pdf",
    filename = "report.pdf"
)
```

### Share multiple payloads (Android / iOS / macOS)

```kotlin
val result = Share.multi(
    listOf(
        SharePayload.Text("Here's the file:"),
        SharePayload.File(pdfBytes, "application/pdf", "invoice.pdf")
    )
)
```

### Custom options

```kotlin
val result = Share.share(
    payload = SharePayload.Text("Hello!"),
    options = ShareOptions(
        chooserTitle = "Share via",            // Android Chooser title
        excludedActivities = listOf("com.instagram.android")
    )
)
```

---

## Platform Notes

| Platform | Behaviour |
|----------|-----------|
| Android  | Native ACTION_SEND Chooser — `chooserTitle` and `excludedActivities` apply |
| iOS      | UIActivityViewController — pass `presentingController` if not in a window |
| macOS    | NSSharingServicePicker — pass `presentingController` (NSView anchor) |
| JVM      | `Desktop.open()` / `mailto:` — best-effort, no result confirmation |
| JS / wasmJs | `navigator.share()` — must be called from a user gesture (e.g., click handler) |

---

## AI-Assisted Setup

```
/sync-share           # Verify Gradle dependency (only gate needed)
/sync-share --check   # Dry run — show status, no writes
```

See [CLAUDE_AI_SETUP.md](CLAUDE_AI_SETUP.md) for full docs.
