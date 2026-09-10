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

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
private data class WebShortcut(val name: String, val short_name: String, val description: String, val url: String)

/**
 * Render these intents as a Web App Manifest [`shortcuts`](https://w3c.github.io/manifest/#shortcuts-member)
 * array.
 *
 * ## Why this exists
 * The web actual used to register nothing, reasoning that "web has no canonical OS-level intent
 * registration API". A page indeed cannot rewrite its own manifest at runtime — but a PWA's
 * manifest *does* have a shortcuts member, and an installed PWA surfaces those in the OS launcher
 * and taskbar. That is precisely what registering an app intent is for. What the library can do is
 * generate the JSON, so you serve it instead of hand-maintaining a second copy that drifts from
 * your intent definitions.
 *
 * ```kotlin
 * val config = appIntents {
 *     intent("add_task") { title = "Add task"; description = "Create a new task" }
 * }
 * AppIntents.register(config)
 *
 * println(config.webAppManifestShortcuts(baseUrl = "/intent"))
 * // [{"name":"Add task","short_name":"Add task","description":"Create a new task",
 * //   "url":"/intent?id=add_task"}]
 * ```
 *
 * Paste that into your `manifest.webmanifest`, or serve the manifest from a route that splices it
 * in. Your app then handles `[baseUrl]?id=…` on startup by calling
 * `AppIntentsRuntime.invoke(id, params)`.
 *
 * @param baseUrl path the shortcut navigates to; the intent id is appended as `?id=`.
 * @param shortNameMaxLength truncation for `short_name`, which launchers display in tight space.
 */
public fun AppIntentsConfig.webAppManifestShortcuts(baseUrl: String = "/", shortNameMaxLength: Int = 12): String {
    val shortcuts = intents.map { def ->
        val name = def.title.ifBlank { def.id }
        WebShortcut(
            name = name,
            short_name = name.take(shortNameMaxLength),
            description = def.description,
            url = baseUrl + (if (baseUrl.contains('?')) "&" else "?") + "id=" + def.id,
        )
    }
    return Json.encodeToString(shortcuts)
}
