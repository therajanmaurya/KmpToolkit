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
 * JVM Desktop — **depends on the host OS**, which is the honest answer for a target that runs on
 * three of them.
 *
 * On Linux the manifest is joined by `.desktop` action handlers that GNOME Shell and
 * xdg-desktop-portal actually surface, so reach is `os`. On macOS and Windows the manifest is
 * written but nothing consumes it automatically — registering an App Shortcut or a shell verb
 * needs a signed bundle or a registry write a plain JVM process should not perform unasked — so
 * reach is `manifest`, and `AppIntents.publishedManifestPath` tells installer tooling where to
 * pick it up.
 */
public actual val platformAppIntentsCapabilities: AppIntentsCapabilities
    get() = if (AppIntents.isLinuxHost()) {
        AppIntentsCapabilities.OsIntegrated
    } else {
        AppIntentsCapabilities.ManifestOnly
    }
