# cmp-share

Cross-platform native share sheet for Kotlin Multiplatform — one API, every target.

[![Maven Central](https://img.shields.io/maven-central/v/io.github.mobilebytelabs/cmp-share.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/io.github.mobilebytelabs/cmp-share)

---

## What It Does

`cmp-share` exposes a single suspending `Share` object that invokes the native share sheet
on Android/iOS/macOS, `navigator.share` on JS/wasmJs, `xdg-open`/`xclip` on Linux, `ShellExecuteW`
on Windows, and `UIPasteboard` on tvOS. Share text, URLs, images, arbitrary files, or a mixed
multi-payload — through one unified API, imperatively via `Share` or injected via `ShareManager`.

> **Stable API.** `@ExperimentalShareApi` is retained as a deprecated no-op so existing
> `@OptIn(...)` call sites keep compiling; both the annotation and the `-opt-in` compiler flag are
> now redundant and can be deleted.

---

## Platform Support

**21 targets — the full KMP matrix.** This table is mirrored in code as
`platformShareCapabilities`, so it cannot drift from behaviour:

| Target | text | url | image | file | multi | How |
|---|:--:|:--:|:--:|:--:|:--:|---|
| Android | ✅ | ✅ | ✅ | ✅ | ✅ | `ACTION_SEND` / `ACTION_SEND_MULTIPLE` chooser; FileProvider grants the URI |
| iOS ×3 | ✅ | ✅ | ✅ | ✅ | ✅ | `UIActivityViewController` |
| macOS ×2 | ✅ | ✅ | ✅ | ✅ | ✅ | `NSSharingServicePicker` |
| JVM | ✅ | ✅ | ✅ | ✅ | ✅ | Native `open`/`xdg-open`/`cmd /c start`, else AWT clipboard + save dialog |
| Linux ×2 | ✅ | ✅ | ✅ | ✅ | ✅ | `xdg-open` for links/files, `xclip` for text, temp file for images |
| Windows | ✅ | ✅ | ✅¹ | ✅¹ | ✅ | `ShellExecuteW` for links; clipboard for text, file paths and temp-written images |
| JS / wasmJs | ✅ | ✅ | ✅² | ✅¹ | ✅ | `navigator.share`; clipboard fallback carries text, links and file URIs |
| watchOS ×5 | ✅³ | ✅³ | ❌ | ❌ | ✅ | `WCSession` handoff — the paired iPhone raises the real share sheet |
| tvOS ×3 | ⚙️ | ⚙️ | ⚙️ | ⚙️ | ⚙️ | App-supplied `TvosShare.handler` — see below |
| wasmWasi | ✅ | ✅ | ✅ | ✅ | ✅ | `WasiShare` host bridge, stdout by default — see below |

¹ Degrades to the clipboard: the path or URI is copied so you can paste it anywhere. Windows
images are written to `%TEMP%` first.
² Web Share Level 2, checked with `canShare` per call; older browsers report `NoHandler`.
³ Delivered to the paired iPhone; `NoHandler` when `WCSession` is unreachable.
⚙️ Depends on registration — `None` until the app sets a handler, then `Full`.

### The rule this table follows

**A missing OS API is a design problem, not a verdict.** Where a platform has no share sheet, the
library degrades to the best real mechanism it has — clipboard, temp file, host bridge — and says
which. What it never does is silently succeed while doing nothing.

That rule caught a live bug: tvOS used to probe for an Objective-C bridge class and return
`Completed` **without dispatching anything**, so callers were told the share worked while nothing
happened. It now routes to a handler whose return value is the truth.

### tvOS — you decide what sharing means

tvOS ships **no** `UIActivityViewController` and **no** `UIPasteboard`. That is not a
Kotlin/Native binding gap; Apple does not include them in the tvOS SDK (verified by compiling
against it). There is no OS-level "hand this to another app" on tvOS at all.

What a tvOS app *can* do is act on the content: show a QR code, display a pairing code, push to a
companion phone app, call a backend. So cmp-share hands it to you:

```kotlin
TvosShare.handler = { item ->
    when (item.kind) {
        "url", "text" -> { showQrCodeOverlay(item.value!!); true }
        else -> false                     // -> ShareResult.Failed(NoHandler)
    }
}
```

Capabilities are **dynamic** here: `None` until a handler is registered, `Full` after — so
`supports()` is honest and a UI can hide its share button until the app can actually honour it.

### wasmWasi — the host is the destination

WASI is a sandboxed, headless system interface: no DOM, no clipboard, no window manager, no user
to present a chooser to. It does not follow that sharing is impossible, only that **the sandbox is
not the destination**. The one reachable "somewhere else" is whatever embedded the module, and
WASI can always reach that.

Register a handler, or use the zero-config default — every item is buffered in `outbox` and echoed
to stdout as one parseable line, so a host that only reads process output still receives it:

```kotlin
WasiShare.handler = { item -> hostQueue.publish(item.kind, item.value, item.bytes); true }

// or, with no setup at all:
Share.text("hello")
WasiShare.drain().single().value          // "hello"
```

Both tvOS and wasmWasi deliver `HostShareItem`s — bundles arrive flattened, so a host never
unpacks a nested structure.

### watchOS

All five architectures build. Text and links are handed to the paired iPhone via
`WCSession.transferUserInfo`; your companion iOS app reads the `{kind, type, value}` dictionary and
presents `UIActivityViewController`.

Binary payloads are not carried. The temp-file write that binary handoff needs goes through
`NSData`/`fwrite` signatures whose bit width differs between 32-bit `watchosArm32` and the 64-bit
watch targets, and Kotlin/Native will not compile one source set spanning both. Dropping
`watchosArm32` would unblock it — a trade against legacy Apple Watch support, not an oversight.

### One DI caveat

`shareModule` ships on **20 of 21** targets: koin-core publishes no wasmWasi variant.
`ShareManager`, `ShareManagerImpl` and `ShareCapabilities` are on all 21 — only the Koin binding is
absent there, and you can construct `ShareManagerImpl()` directly.

---

## Quick Start

No opt-in required — cmp-share is a stable API.

```kotlin
suspend fun onShareClicked() {
    when (val result = Share.text("Check out KMP Toolkit!")) {
        is ShareResult.Completed -> println("Shared")
        is ShareResult.Cancelled -> println("User cancelled")
        is ShareResult.Failed -> println("Error: ${result.cause}")
    }
}
```

---

## Injecting it — `ShareManager`

`Share` is an `expect object`, which means code calling it directly cannot be tested without a real
share sheet and cannot be decorated. `ShareManager` is the same capability behind an injectable
type:

```kotlin
class ReportViewModel(private val share: ShareManager) : ViewModel() {
    fun export(uri: String) = viewModelScope.launch {
        share.shareFile(uri, "application/pdf", message = "Latest report")
    }
}
```

`shareFile(..., message = ...)` is the call that used to force callers to assemble a
`SharePayload.Multi` by hand.

### With Koin

```kotlin
startKoin { modules(shareModule, appModule) }
```

`shareModule` binds `ShareManager` to `ShareManagerImpl` as a `single` — it is stateless.

### With anything else

Nothing in cmp-share requires Koin except `di/ShareModule.kt`. Construct `ShareManagerImpl()` and
register it against `ShareManager` in Hilt, Kodein or your own container.

---

## Compose — `cmp-share-compose`

```kotlin
val share = rememberShareManager()
val scope = rememberCoroutineScope()

Button(onClick = { scope.launch { share.shareUrl(article.url) } }) { Text("Share") }
```

`rememberShareManager()` works with **no setup** — unlike `LocalNetworkMonitor`, reading
`LocalShareManager` without a provider returns a real `ShareManagerImpl` rather than throwing,
because sharing is stateless and zero-config everywhere. Provide your own to substitute one:

```kotlin
val share: ShareManager = koinInject()
ProvideShareManager(share) { App() }
```

### Sharing what is on screen

```kotlin
val graphicsLayer = rememberGraphicsLayer()
scope.launch { share.shareImage("sales-q3", graphicsLayer.toImageBitmap()) }
```

`ImageBitmap` encoding lives here rather than in the headless artifact — Android uses its own PNG
codec, every other Compose target goes through Skia.

---

## Asking before you offer

Rendering a share button that fails once tapped is worse than not rendering it:

```kotlin
if (rememberShareCapabilities().file) {
    IconButton(onClick = ::exportPdf) { Icon(Icons.Default.Share, null) }
}
```

Outside Compose, `manager.supports(payload)` answers the same question for a specific payload,
including bundles — a `SharePayload.Multi` is supported only when bundling *and* every item in it
is.

---

## Testing

`FakeShareManager` ships in the main artifact, so no extra test dependency is needed:

```kotlin
val share = FakeShareManager()
ReportViewModel(share).export("file:///report.pdf")

assertIs<SharePayload.Multi>(share.recorded.single().payload)
```

Pin the platform to assert capability-dependent UI without running on that platform:

```kotlin
val tv = FakeShareManager(capabilities = ShareCapabilities.TextAndUrlOnly)
assertFalse(tv.supports(SharePayload.File("file:///a.pdf", "application/pdf")))
```

Drive failure paths with `scriptError(ShareError.UserGestureMissing)`.

---

## API Reference

### `Share` expect object

```kotlin
expect object Share {
    suspend fun share(
        payload: SharePayload,
        options: ShareOptions = ShareOptions()
    ): ShareResult
}
```

### Extension functions

```kotlin
suspend fun Share.text(content: String, options: ShareOptions = ShareOptions()): ShareResult
suspend fun Share.url(href: String, options: ShareOptions = ShareOptions()): ShareResult
suspend fun Share.image(
    bytes: ByteArray,
    mimeType: String,
    filename: String? = null,
    options: ShareOptions = ShareOptions()
): ShareResult
suspend fun Share.file(
    bytes: ByteArray,
    mimeType: String,
    filename: String,
    options: ShareOptions = ShareOptions()
): ShareResult
suspend fun Share.multi(
    payloads: List<SharePayload>,
    options: ShareOptions = ShareOptions()
): ShareResult
```

### `SharePayload` sealed class

```kotlin
sealed class SharePayload {
    data class Text(val content: String) : SharePayload()
    data class Url(val href: String) : SharePayload()
    data class Image(val bytes: ByteArray, val mimeType: String, val filename: String?) : SharePayload()
    data class File(val bytes: ByteArray, val mimeType: String, val filename: String) : SharePayload()
    data class Multi(val payloads: List<SharePayload>) : SharePayload()
}
```

### `ShareOptions`

```kotlin
data class ShareOptions(
    val chooserTitle: String? = null,          // Android only — Chooser dialog title
    val excludedActivities: List<String> = emptyList(), // Android only — package names to exclude
    val presentingController: Any? = null      // iOS/macOS — UIViewController / NSViewController
)
```

### `ShareResult` sealed class

```kotlin
sealed class ShareResult {
    object Completed : ShareResult()
    object Cancelled : ShareResult()
    data class Failed(val cause: ShareError) : ShareResult()
}
```

### `ShareError`

```kotlin
sealed class ShareError {
    object UnsupportedPlatform : ShareError()
    object NoHandler : ShareError()
    object UserGestureMissing : ShareError()   // JS/wasmJs only
    data class Unknown(val message: String) : ShareError()
}
```

---

## Notes

- **Android file shares — `file://` is resolved for you (since v3.5.20).** `ACTION_SEND` rejects a
  `file://` URI on Android 7+ (`FileUriExposedException`). `SharePayload.File` previously passed the
  caller's URI to `EXTRA_STREAM` verbatim, so every `file://` share failed; and because `share()`
  wraps everything in `try/catch → ShareResult.Failed`, any caller that ignored the returned
  `ShareResult` saw the share button silently do nothing. Now:
  - a `content://` (or `http(s)://`) URI is passed through **unchanged** — no behaviour change for
    payloads that already worked;
  - a `file://` URI (or a bare path) is wrapped through this module's own FileProvider into a
    `content://` URI, which — with the `FLAG_GRANT_READ_URI_PERMISSION` already set — gives the
    receiving app scoped, per-URI read access and no storage permission;
  - a file outside the paths in `cmp_share_paths.xml` is staged into the module's cache dir first
    (one copy) and shared from there, so consumers don't have to widen their own FileProvider paths;
  - if all of that fails the original URI is used, so behaviour is never worse than before.

  **Still prefer passing a `content://` URI** when you already have one (e.g. a MediaStore insert) —
  it skips the wrap and any staging copy entirely.
- **JS / wasmJs**: `navigator.share()` requires HTTPS and a user gesture — call from a click
  handler, not a coroutine launched in `LaunchedEffect` without user interaction.
- **JVM**: best-effort — opens the default handler registered by the OS; file-type support
  depends on installed applications.
- **Multi-payload on iOS**: wraps items in a single `UIActivityViewController` activity items
  array.

---

## Docs

- [SETUP.md](SETUP.md) — Integration steps
- [CLAUDE_AI_SETUP.md](CLAUDE_AI_SETUP.md) — AI-assisted setup with `/sync-share`
