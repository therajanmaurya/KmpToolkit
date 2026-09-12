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

import java.io.File
import java.util.Locale

/**
 * JVM Desktop `AppIntents` — publishes the manifest to the OS-appropriate location, and on Linux
 * emits `.desktop` action handlers too.
 *
 * ## What changed and why
 * This used to be a deliberate no-op, on the reasoning that "desktop has no canonical OS-level
 * intent abstraction". True in the abstract — but the JVM runs on the same three operating systems
 * whose Kotlin/Native actuals in this very module already write manifests and `.desktop` entries.
 * Refusing here meant a JVM desktop app got nothing while an identical Linux/K-N build got real
 * GNOME Shell integration. The mechanism was already proven; only this target was not using it.
 *
 * | Host OS | Written to | Reach |
 * |---|---|---|
 * | Linux | `$XDG_DATA_HOME/cmp-app-intents/manifest.json` + `applications/cmp-{id}.desktop` | os |
 * | macOS | `~/Library/Application Support/cmp-app-intents/manifest.json` | manifest |
 * | Windows | `%APPDATA%\cmp-app-intents\manifest.json` | manifest |
 *
 * macOS and Windows stop at the manifest: registering an App Shortcut or a shell verb needs a
 * signed app bundle or a registry write that a plain JVM process has no business performing
 * unasked. [publishedManifestPath] tells you where it landed so installer tooling can finish
 * the job.
 *
 * `register()` is best-effort — filesystem errors are swallowed, matching the Linux and Windows
 * K/N actuals, so registration never throws into app startup.
 */
public actual object AppIntents {

    /** Where the last [register] call wrote its manifest, or `null` if it could not write one. */
    public var publishedManifestPath: String? = null
        private set

    public actual fun register(config: AppIntentsConfig) {
        AppIntentsRuntime.register(config)
        publishedManifestPath = runCatching { publish(config) }.getOrNull()
    }

    public actual suspend fun invokeForTesting(id: String, params: Map<String, Any>): AppIntentResult? =
        AppIntentsRuntime.invoke(id, params)

    private fun publish(config: AppIntentsConfig): String {
        val dir = File(dataDirectory(), "cmp-app-intents").apply { mkdirs() }
        val manifest = File(dir, "manifest.json")
        manifest.writeText(config.serializeManifest())

        if (hostOs() == HostOs.Linux) {
            writeDesktopEntries(config)
        }
        return manifest.absolutePath
    }

    /**
     * Emit one `.desktop` action handler per intent, matching what the Linux K/N actual writes so
     * both builds surface identically in GNOME Shell.
     */
    private fun writeDesktopEntries(config: AppIntentsConfig) {
        val appsDir = File(dataDirectory(), "applications").apply { mkdirs() }
        for (def in config.intents) {
            val safeId = def.id.replace(Regex("[^A-Za-z0-9._-]"), "_")
            File(appsDir, "cmp-$safeId.desktop").writeText(
                buildString {
                    appendLine("[Desktop Entry]")
                    appendLine("Type=Application")
                    appendLine("Name=" + def.title.ifBlank { def.id })
                    appendLine("Comment=" + def.description)
                    appendLine("NoDisplay=true")
                    appendLine("X-CmpAppIntentId=" + def.id)
                },
            )
        }
    }

    private enum class HostOs { Linux, MacOs, Windows, Other }

    private fun hostOs(): HostOs {
        val name = System.getProperty("os.name").orEmpty().lowercase(Locale.ROOT)
        return when {
            name.contains("linux") || name.contains("nix") || name.contains("nux") -> HostOs.Linux
            name.contains("mac") || name.contains("darwin") -> HostOs.MacOs
            name.contains("win") -> HostOs.Windows
            else -> HostOs.Other
        }
    }

    /** The per-user data directory this OS expects, following each platform's own convention. */
    private fun dataDirectory(): File {
        val home = System.getProperty("user.home").orEmpty()
        return when (hostOs()) {
            HostOs.Linux -> System.getenv("XDG_DATA_HOME")
                ?.takeIf { it.isNotBlank() }
                ?.let(::File)
                ?: File(home, ".local/share")

            HostOs.MacOs -> File(home, "Library/Application Support")

            HostOs.Windows -> System.getenv("APPDATA")
                ?.takeIf { it.isNotBlank() }
                ?.let(::File)
                ?: File(home, "AppData/Roaming")

            HostOs.Other -> File(home, ".cmp-app-intents")
        }
    }

    /** Which OS this JVM is on — exposed so [platformAppIntentsCapabilities] can be honest. */
    internal fun isLinuxHost(): Boolean = hostOs() == HostOs.Linux
}
