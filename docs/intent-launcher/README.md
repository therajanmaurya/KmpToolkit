# cmp-intent-launcher

Type-safe Android Intent launcher with graceful `onUnsupported` fallback for all other KMP targets.

[![Maven Central](https://img.shields.io/maven-central/v/io.github.mobilebytelabs/cmp-intent-launcher.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/io.github.mobilebytelabs/cmp-intent-launcher)

---

## What It Does

`cmp-intent-launcher` wraps Android's `ActivityResultLauncher` pattern in a coroutine-friendly,
Compose-ready `IntentLauncher` class.  Build intents via a Kotlin DSL, `await` the result, and
handle the typed `IntentResult` — no callbacks, no `onActivityResult`.  Picker contracts
(`PickImage`, `PickMultipleImages`, `PickDocument`, `PickContact`) are wired to native UIKit pickers
on iOS (`PHPickerViewController`, `UIDocumentPickerViewController`, `CNContactPickerViewController`);
unsupported actions on any platform route to the caller's `onUnsupported` lambda.

> **Stable API.** `@ExperimentalIntentLauncherApi` is retained as a deprecated no-op so existing
> `@OptIn(...)` call sites keep compiling; both it and the `-opt-in` compiler flag are now
> redundant and can be deleted.

---

## Platform Support

**21 targets — the full KMP matrix.** This table is mirrored in code as
`platformIntentCapabilities`, so it cannot drift from behaviour:

| Target | view URI | pick image | multi | document | contact | app settings | create doc |
|---|:--:|:--:|:--:|:--:|:--:|:--:|:--:|
| Android | ✅¹ | ✅¹ | ✅¹ | ✅¹ | ✅¹ | ✅ | ✅ |
| iOS ×3 | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| macOS ×2 | ✅ | ✅ | ✅ | ✅ | ❌ | ✅ | ✅ |
| Linux ×2 | ✅ | ✅ | ✅ | ✅ | ❌ | ✅ | ✅ |
| Windows | ✅ | ✅ | ✅ | ✅ | ❌ | ✅ | ✅ |
| JVM | ✅ | ✅ | ❌ | ✅ | ❌ | ✅ | ✅ |
| JS / wasmJs | ✅ | ✅ | ✅ | ✅ | ❌ | ❌ | ✅ |
| watchOS ×5 | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ |
| tvOS ×3 | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ |
| wasmWasi | ⚙️ | ⚙️ | ⚙️ | ⚙️ | ⚙️ | ⚙️ | ⚙️ |

¹ Needs an Activity-scoped launcher — see **Android** below.
⚙️ Dynamic: nothing until a `WasiIntents.handler` is registered, everything after.

How each does it: Android system Intents · iOS `PHPicker`/`UIDocumentPicker`/`CNContactPicker` ·
macOS `NSOpenPanel`/`NSSavePanel` · Linux `zenity` · Windows PowerShell dialogs · JVM AWT
`FileDialog` + `Desktop.browse` · web `<input type=file>` + `window.open` · watchOS
`WKExtension.openSystemURL`.

### The rule this table follows

**A missing OS API is a design problem, not a verdict.** Where a platform has no native route, the
library uses the best real mechanism it has and says which. Three cells changed under that rule:

- **Windows had no pickers at all.** They were blocked on a `GetOpenFileNameW` cinterop that cannot
  work — K/N's parser drops Win32 SDK struct types unless `windows.h` resolves natively, which
  needs a Windows build host. PowerShell's `OpenFileDialog`/`SaveFileDialog` over `_popen` needs
  neither, and mirrors how the Linux actual shells out to `zenity`.
- **JS and wasmJs could not open a URL** — the commonest intent there is fell through to
  `UnsupportedPlatform` on a platform that obviously handles it. Now `window.open`.
- **wasmWasi** routes to a host bridge instead of refusing.

### Android

`IntentLauncher` wraps an `ActivityResultLauncher`, so it is **Activity-scoped** — there is no
context-free default. Rather than fail at the call site, a manager built without one reports the
pickers as unsupported and keeps `openAppSettings` and `createDocument`, which the library's own
init provider services without an Activity. `supports()` stays truthful either way.

```kotlin
// inside a ComponentActivity — full surface
val manager = IntentManagerImpl(intentLauncher())

// or in Compose
ProvideIntentManager(rememberIntentManagerFromLauncher()) { App() }
```

### tvOS

The one target with nothing. Apple ships no `PHPicker`, no `UIDocumentPicker` and no
`CNContactPicker` to tvOS, and arbitrary `openURL` is gated too. Unlike sharing there is no useful
degradation: a picker is UI the OS must own, and an app-supplied replacement is a different feature
rather than the same one delivered differently. Per-call `onUnsupported { }` remains the escape
hatch.

### wasmWasi

A sandbox has no picker, but the host that embedded it usually does:

```kotlin
WasiIntents.handler = { request ->
    when (request.action) {
        HostIntentRequest.ACTION_CREATE_DOCUMENT ->
            hostSavePanel(request.extras[HostIntentRequest.EXTRA_SUGGESTED_NAME] as? String)
                ?.let { IntentResult.Ok(IntentData(uri = it)) } ?: IntentResult.Cancelled
        else -> IntentResult.Failed(IntentError.NoHandler)
    }
}
```

`SystemIntents` calls arrive here too, under the synthetic `cmp.action.*` actions, so a host
implements one function rather than three.

---

## Injecting it — `IntentManager`

`IntentLauncher` is an `expect class`: code calling it directly cannot be faked in tests or
decorated. `IntentManager` is the same capability behind an injectable type, plus the capability
probe:

```kotlin
class AvatarViewModel(private val intents: IntentManager) : ViewModel() {
    val canPick = intents.supports(IntentOperation.PickImage)

    fun choose() = viewModelScope.launch {
        when (val r = intents.pickImage()) {
            is IntentResult.Ok -> setAvatar(r.data?.uri)
            is IntentResult.Cancelled -> Unit
            is IntentResult.Failed -> showError(r.cause)
        }
    }
}
```

With Koin: `startKoin { modules(intentLauncherModule) }`. Without: construct `IntentManagerImpl()`
and register it against `IntentManager` in your own container — nothing else here needs Koin.

`FakeIntentManager` ships in the main artifact:

```kotlin
val intents = FakeIntentManager()
AvatarViewModel(intents).choose()
assertTrue(intents.recorded.single().expectsResult)
```

Unscripted calls return `Cancelled` — what a real picker gives when the user backs out, and the
branch call sites most often forget.

---

## Compose — `cmp-intent-launcher-compose`

```kotlin
val intents = rememberIntentManager()
val scope = rememberCoroutineScope()

if (rememberSupportsIntent(IntentOperation.PickImage)) {
    Button(onClick = { scope.launch { intents.pickImage() } }) { Text("Choose photo") }
}
```

Reading `LocalIntentManager` without a provider does not throw. On Android, provide
`rememberIntentManagerFromLauncher()` to get the pickers.

---

## Quick Start## Quick Start

```kotlin
@Composable
fun PickFileScreen() {
    val launcher = rememberIntentLauncher()

    Button(onClick = {
        scope.launch {
            val result = launcher.launch {
                action = Intent.ACTION_GET_CONTENT
                type = "*/*"
            }
            when (result) {
                is IntentResult.Ok        -> handleData(result.data)
                is IntentResult.Cancelled -> { /* user dismissed */ }
                is IntentResult.Failed    -> logError(result.cause)
            }
        }
    }) {
        Text("Pick File")
    }
}
```

---

## API Reference

### `IntentLauncher` expect class

```kotlin
expect class IntentLauncher {
    suspend fun launch(block: IntentBuilder.() -> Unit): IntentResult
}
```

### Compose helper

```kotlin
@Composable
fun rememberIntentLauncher(): IntentLauncher
```

### `IntentBuilder` DSL

```kotlin
class IntentBuilder {
    var action: String?
    var type: String?
    fun data(uri: String)
    fun extra(key: String, value: Any)
    var flags: Int
    fun targetPackage(packageName: String)  // Android only — targeted launch
}
```

### `IntentResult` sealed class

```kotlin
sealed class IntentResult {
    data class Ok(val data: IntentData?) : IntentResult()
    object Cancelled : IntentResult()
    data class Failed(val cause: IntentError) : IntentResult()
}
```

### `IntentError`

```kotlin
sealed class IntentError {
    object UnsupportedPlatform : IntentError()
    object NoHandler : IntentError()
    object UserGestureMissing : IntentError()
    data class Unknown(val message: String) : IntentError()
}
```

### `ResultContracts` object

```kotlin
object ResultContracts {
    val PICK: String          // ACTION_PICK
    val GET_CONTENT: String   // ACTION_GET_CONTENT
    val OPEN_DOCUMENT: String // ACTION_OPEN_DOCUMENT
    val CREATE_DOCUMENT: String
    val SEND: String          // ACTION_SEND
}
```

---

## Notes

- `rememberIntentLauncher()` **must be called at Compose composition time** — not inside a
  `LaunchedEffect` or `onClick` lambda.  It registers the `ActivityResultLauncher` during
  composition.
- **JVM partial**: `Desktop.open()` / `Desktop.browse()` runs but returns
  `IntentResult.Ok(null)` — no result data is propagated back.
- On non-Android targets, `onUnsupported` surfaces as `IntentResult.Failed(IntentError.UnsupportedPlatform)`.

---

## Docs

- [SETUP.md](SETUP.md) — Integration steps (includes Android Activity wiring)
- [CLAUDE_AI_SETUP.md](CLAUDE_AI_SETUP.md) — AI-assisted setup with `/sync-intent-launcher`
