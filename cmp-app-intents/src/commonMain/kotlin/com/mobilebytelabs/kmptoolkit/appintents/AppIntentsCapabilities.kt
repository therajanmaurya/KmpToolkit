/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
package com.mobilebytelabs.kmptoolkit.appintents

/**
 * How far a registered intent actually reaches on this target.
 *
 * ## Why this is a spectrum, not a boolean
 * Registering an app intent means two different things at once: keeping the `perform` lambdas
 * addressable in-process, and publishing them somewhere the OS or an assistant can find them.
 * Every target does the first. What varies is the second — and a caller that wants to say "Hey
 * Siri, add a task" needs to know which it got, because in-process registration alone will never
 * produce that.
 *
 * Read [assistantReach] before promising a voice affordance in your UI.
 */
public data class AppIntentsCapabilities(
    /**
     * Intents are addressable in-process and can be performed via
     * [AppIntents.invokeForTesting]. True everywhere — it is what the runtime registry does.
     */
    public val inProcess: Boolean,

    /** A manifest describing the intents is written somewhere outside the process. */
    public val publishesManifest: Boolean,

    /**
     * The OS itself can surface and launch these intents — a launcher shortcut, a `.desktop`
     * action, an assistant phrase.
     */
    public val osIntegration: Boolean,
) {
    /** A one-line summary for logs and docs. */
    public val assistantReach: String
        get() = when {
            osIntegration -> "os"
            publishesManifest -> "manifest"
            else -> "in-process"
        }

    public companion object {
        /** Registered with the OS — Android, iOS, macOS, Linux, Windows. */
        public val OsIntegrated: AppIntentsCapabilities =
            AppIntentsCapabilities(inProcess = true, publishesManifest = true, osIntegration = true)

        /** A manifest is published, but nothing consumes it automatically. */
        public val ManifestOnly: AppIntentsCapabilities =
            AppIntentsCapabilities(inProcess = true, publishesManifest = true, osIntegration = false)

        /** The runtime registry only — no manifest leaves the process. */
        public val InProcessOnly: AppIntentsCapabilities =
            AppIntentsCapabilities(inProcess = true, publishesManifest = false, osIntegration = false)
    }
}

/**
 * How far registration reaches on THIS target.
 *
 * | Target | reach | what happens on `register()` |
 * |---|---|---|
 * | Android | os | Shortcuts XML + Built-in Intent capabilities |
 * | iOS | os | Manifest for the Swift `AppIntentsProvider` bridge |
 * | macOS, Linux, Windows | os | Manifest plus `.desktop` / registry entries |
 * | JVM | manifest | Manifest written to the OS-appropriate data directory |
 * | JS, wasmJs | manifest | Web App Manifest `shortcuts` JSON, for you to serve |
 * | wasmWasi | ⚙️ | Handed to the host, or echoed to stdout |
 * | tvOS, watchOS | in-process | Runtime registry only — no bridge surface exists |
 *
 * ⚙️ Dynamic: `manifest` until a `WasiAppIntents.onRegister` handler is set, `os` after — the host
 * decides what registration means.
 */
public expect val platformAppIntentsCapabilities: AppIntentsCapabilities
