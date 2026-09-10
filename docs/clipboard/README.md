# cmp-clipboard

Zero-configuration cross-platform clipboard for Kotlin Multiplatform.

[![Maven Central](https://img.shields.io/maven-central/v/io.github.mobilebytelabs/kmp-clipboard.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/io.github.mobilebytelabs/kmp-clipboard)

---

## What It Does

Copy, paste, check, and observe the system clipboard across all KMP platforms — no setup, no
config class. Import and use, or bind `ClipboardManager` through Koin — see Dependency injection.

---

## Platform Support

**21 targets.** Every one can copy and read back; what varies is whether the text leaves your app.
`platformClipboardCapabilities` reports it, so a toast can say "Copied to clipboard" only where
that is true.

| Platform | Copy | Read | Observe | Clipboard is |
|----------|:----:|:----:|:-------:|---|
| Android | ✅ | ✅ | ✅ lifecycle-aware | system |
| iOS | ✅ | ✅ | ✅ foreground change detection | system |
| macOS | ✅ | ✅ | ✅ polling + changeCount | system |
| JVM | ✅ | ✅ | ✅ FlavorListener | system |
| Linux | ✅ | ✅ | polling (needs `xclip`/`xsel`) | system |
| Windows | ✅ | ✅ | polling | system |
| JS / wasmJs | ✅ | ✅¹ | limited (visibility change) | system |
| tvOS ×3 | ✅² | ✅² | ✅² | **app-scoped** |
| watchOS ×5 | ✅² | ✅² | ✅² | **app-scoped** |
| wasmWasi | ✅² | ✅² | ✅² | **app-scoped** |

¹ `navigator.clipboard.readText()` may raise a permission prompt — flagged as
`readNeedsPermission`.
² Via `InAppClipboard` — see below.

### This table used to be wrong in both directions

tvOS and watchOS were listed as fully ✅ while **all three operations were hardcoded to
`false` / `null`** — the docs advertised a working clipboard on platforms that had none. WASI was
listed ❌, which was pessimistic rather than false. Both are now accurate, and all three targets
genuinely work.

### App-scoped clipboards — tvOS, watchOS, wasmWasi

Apple ships no `UIPasteboard` to tvOS or watchOS (verified by compiling against it), and WASI is
sandboxed and headless. But a clipboard is, at bottom, *somewhere to put text and get it back* —
and that round-trips perfectly well inside an app, which is what most in-app copy flows need:
copy this code, paste it into that field two screens later.

So these three route through `InAppClipboard` instead of refusing. What they do **not** claim is
system reach — `systemWide` is `false`, so nothing outside your app sees it.

To go further, bridge it:

```kotlin
// watchOS — forward every copy to the paired iPhone
InAppClipboard.onCopy = { text -> session.transferUserInfo(mapOf("clip" to text)); true }
InAppClipboard.onRead = { lastReceivedFromPhone }
```

The buffer is updated even when the bridge reports failure, so an in-app paste keeps working when
the remote side is unreachable.

Monitoring works on these targets too: `InAppClipboardMonitor` observes `InAppClipboard` and drives
the same state machine as every other platform. Previously their monitors' `start()` did nothing,
leaving the state stuck on `Idle` and failing the shared `commonTest` suite — silently, because no
CI job runs those targets' test tasks.

---

## Dependency injection

The README used to say "no DI, just import and use". `ClipboardManager` is now bindable:

```kotlin
startKoin { modules(clipboardModule(), appModule) }

// or with a custom config
modules(clipboardModule(ClipboardManagerConfig(historySize = 50)))
```

Bound as a `single` deliberately: a second `ClipboardManager` means a second monitor watching the
same clipboard and a history split across two objects.

Not using Koin? Construct `ClipboardManager()` once and share that instance yourself. The
top-level functions (`copyToClipboard`, `getFromClipboard`, …) remain available and unchanged.

---

## Quick Start

```kotlin
import com.mobilebytelabs.kmptoolkit.clipboard.copyToClipboard
import com.mobilebytelabs.kmptoolkit.clipboard.getFromClipboard
import com.mobilebytelabs.kmptoolkit.clipboard.hasClipboardText
import com.mobilebytelabs.kmptoolkit.clipboard.clearClipboard

// Copy
val success = copyToClipboard("Hello, World!")

// Read
val text: String? = getFromClipboard()

// Check
if (hasClipboardText()) { /* enable paste button */ }

// Clear
clearClipboard()
```

---

## API Reference

### Core Functions (top-level)

```kotlin
// Copy text — returns true if initiated successfully
fun copyToClipboard(text: String): Boolean

// Read text — returns null if empty, unsupported, or non-text
fun getFromClipboard(): String?

// Check if clipboard has text content
fun hasClipboardText(): Boolean

// Clear clipboard
fun clearClipboard()
```

### Reactive Observer (optional)

```kotlin
import com.mobilebytelabs.kmptoolkit.clipboard.createClipboardObserver
import com.mobilebytelabs.kmptoolkit.clipboard.rememberClipboardObserver

// In a ViewModel / coroutine scope
val observer = createClipboardObserver()
observer.startObserving()
observer.clipboardContent.collect { content ->
    println("Clipboard changed: $content")
}
observer.stopObserving()

// In Compose (lifecycle-managed)
@Composable
fun MyScreen() {
    val observer = rememberClipboardObserver()
    val content by observer.clipboardContent.collectAsState()
    Text("Clipboard: $content")
}
```

### ClipboardObserver interface

```kotlin
interface ClipboardObserver {
    val clipboardContent: StateFlow<String?>
    val isObserving: Boolean
    fun startObserving()
    fun stopObserving()
}
```

---

## Notes

- **JS**: `copyToClipboard` is fire-and-forget (async write) — always returns `true`
- **JS / Wasm**: `getFromClipboard()` returns `null` (async API, read not supported synchronously)
- **Linux**: requires `xclip` or `xsel` installed on the system
- **WASI**: no clipboard access available in runtime

---

## Docs

- [SETUP.md](SETUP.md) — Integration steps
- [CLAUDE_AI_SETUP.md](CLAUDE_AI_SETUP.md) — AI-assisted setup with `/sync-clipboard`
