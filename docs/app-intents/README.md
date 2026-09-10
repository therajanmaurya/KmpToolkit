# cmp-app-intents

Declare and register app intents (Siri Shortcuts / Android App Actions) from shared Kotlin code.

[![Maven Central](https://img.shields.io/maven-central/v/io.github.mobilebytelabs/cmp-app-intents.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/io.github.mobilebytelabs/cmp-app-intents)

---

## What It Does

`cmp-app-intents` lets you declare your app's Siri Shortcuts, Android App Actions, and Spotlight
search contributions from a single Kotlin DSL.  On iOS/macOS it generates the required
`AppIntents` manifest and bridges to Swift via `CmpAppIntentBridge`.  On Android it registers
intents with the on-device registry.  On JVM, JS, and wasmJs it provides `invokeForTesting`
so you can drive intent execution in tests or demos without a real device.

> **Stable API.** `@ExperimentalAppIntentsApi` is retained as a deprecated no-op so existing
> `@OptIn(...)` call sites keep compiling; it and the `-opt-in` flag are now redundant.

---

## Platform Support

**21 targets — the full KMP matrix.** Registration means two things at once: keeping the `perform`
lambdas addressable in-process, and publishing them where the OS can find them. Every target does
the first; the second is what varies, and `platformAppIntentsCapabilities` reports which you got.

| Target | reach | what `register()` does |
|---|---|---|
| Android | **os** | Shortcuts XML + Built-in Intent capability blocks |
| iOS ×3 | **os** | Manifest for the Swift bridge → Siri, Shortcuts, Spotlight |
| macOS ×2 | **os** | Manifest + App Shortcuts via the same bridge |
| Linux ×2 | **os** | Manifest + a `.desktop` action handler per intent |
| Windows | **os** | Manifest + shell registry entries |
| JVM | **os** on Linux, **manifest** elsewhere | Manifest to the OS data dir; `.desktop` files on Linux |
| JS / wasmJs | **manifest** | Web App Manifest `shortcuts` JSON for you to serve |
| wasmWasi | ⚙️ | Handed to the host, or echoed to stdout |
| watchOS ×5 / tvOS ×3 | **in-process** | Runtime registry only — no bridge surface exists |

⚙️ `manifest` until a `WasiAppIntents.onRegister` handler is set, `os` after.

Read the reach before promising anything to the user:

```kotlin
if (intents.reachesOs()) showVoiceOnboarding()   // "Try saying: add a task"
```

Showing that card where reach is `in-process` teaches a phrase that can never work.

### The rule this table follows

**A missing OS API is a design problem, not a verdict.** Three targets changed under it:

- **JVM was a deliberate no-op**, reasoning that "desktop has no canonical OS-level intent
  abstraction". True in the abstract — but the JVM runs on the same three operating systems whose
  Kotlin/Native actuals *in this very module* already write manifests and `.desktop` entries. A JVM
  desktop app got nothing while an identical Linux/K-N build got real GNOME Shell integration. It
  now writes to the OS-appropriate data directory, and `.desktop` handlers on Linux.
- **JS and wasmJs registered nothing**, because a page cannot rewrite its own manifest at runtime.
  It cannot — but a PWA manifest has a `shortcuts` member that an installed app surfaces in the
  launcher, which is exactly what registering an intent is for. `webAppManifestShortcuts()`
  generates that JSON from your definitions, so you serve it instead of hand-maintaining a second
  copy that drifts.
- **wasmWasi** publishes across a host bridge instead of refusing.

tvOS and watchOS stay `in-process`: the ObjC-bridged callback singleton is defined for iOS and
macOS only, and tvOS App Intents have no CoreSpotlight or AppShortcutsProvider to reach anyway.

### Web — serving the shortcuts

```kotlin
val config = appIntents {
    intent("add_task") { title = "Add task"; description = "Create a new task" }
}
AppIntents.register(config)

println(config.webAppManifestShortcuts(baseUrl = "/intent"))
// [{"name":"Add task","short_name":"Add task","description":"Create a new task",
//   "url":"/intent?id=add_task"}]
```

Splice that into your `manifest.webmanifest`, then handle `?id=…` on startup with
`AppIntentsRuntime.invoke(id, params)`.

---

## Injecting it — `AppIntentsManager`

`AppIntents` is an `expect object`, so code calling it cannot be faked or decorated.
`AppIntentsManager` is the same capability behind an injectable type plus the reach probe:

```kotlin
class AppStartup(private val intents: AppIntentsManager) {
    fun onCreate() {
        intents.register {
            intent("add_task") {
                title = "Add task"
                parameter("text", ParamType.Text)
                perform { p -> repo.add(p["text"] as String); AppIntentResult.Done }
            }
        }
        if (intents.reachesOs()) showVoiceOnboarding()
    }
}
```

With Koin: `startKoin { modules(appIntentsModule) }`. Without: construct `AppIntentsManagerImpl()`
and register it yourself.

`FakeAppIntentsManager` ships in the main artifact and does **not** touch the process-wide runtime,
so tests cannot leak registrations into one another:

```kotlin
val intents = FakeAppIntentsManager(capabilities = AppIntentsCapabilities.InProcessOnly)
AppStartup(intents).onCreate()
assertFalse(intents.reachesOs())
```

---

## Compose — `cmp-app-intents-compose`

```kotlin
if (rememberAppIntentsReachOs()) {
    VoiceOnboardingCard()
}
```

`LocalAppIntentsManager` / `ProvideAppIntentsManager` / `rememberAppIntentsManager()` follow the
same shape as the rest of the toolkit; reading without a provider works and does not throw.

---

## Quick Start## Quick Start

```kotlin
fun registerIntents() {
    AppIntents.register(
        appIntents {
            intent("search_products") {
                title("Search Products")
                param("query") { type = ParamType.String }
                handler { params ->
                    val query = params["query"] as? String ?: ""
                    AppIntentResult.Snippet("Results for: $query")
                }
            }
            intent("open_dashboard") {
                title("Open Dashboard")
                handler { AppIntentResult.Done }
            }
        }
    )
}
```

---

## API Reference

### `AppIntents` expect object

```kotlin
expect object AppIntents {
    fun register(config: AppIntentsConfig)
    suspend fun invokeForTesting(
        id: String,
        params: Map<String, Any> = emptyMap()
    ): AppIntentResult?
}
```

### Top-level DSL builder

```kotlin
fun appIntents(block: AppIntentsBuilder.() -> Unit): AppIntentsConfig
```

### `AppIntentsBuilder`

```kotlin
class AppIntentsBuilder {
    fun intent(id: String, block: AppIntentBuilder.() -> Unit)
}
```

### `AppIntentBuilder`

```kotlin
class AppIntentBuilder {
    fun title(text: String)
    fun param(name: String, block: ParamBuilder.() -> Unit)
    fun handler(block: suspend (params: Map<String, Any>) -> AppIntentResult)
}
```

### `ParamType`

```kotlin
enum class ParamType {
    String, Int, Boolean, Double, Entity
}
```

### `AppIntentResult` sealed class

```kotlin
sealed class AppIntentResult {
    data class Dialog(val message: String) : AppIntentResult()
    data class Snippet(val markdown: String) : AppIntentResult()
    object Done : AppIntentResult()
    data class Failed(val message: String) : AppIntentResult()
}
```

---

## Notes

- **iOS Spotlight**: `invokeForTesting` works on Simulator; Spotlight index surfacing requires
  verification on a real device with Siri enabled.
- **Android**: App Actions require `shortcuts.xml` in the app module — `cmp-app-intents`
  generates this at build time from your registered config.
- **Swift bridge**: copy `cmp-app-intents/swift/CmpAppIntentBridge.swift` into your `iosApp`
  Xcode target — see [SETUP.md](SETUP.md) Step 4.

---

## Docs

- [SETUP.md](SETUP.md) — Integration steps (includes iOS Swift bridge installation)
- [CLAUDE_AI_SETUP.md](CLAUDE_AI_SETUP.md) — AI-assisted setup with `/sync-app-intents`
