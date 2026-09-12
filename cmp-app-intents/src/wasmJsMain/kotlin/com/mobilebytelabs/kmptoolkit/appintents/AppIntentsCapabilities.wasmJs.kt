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
 * wasmJs — a page cannot rewrite its own manifest at runtime, so the OS never picks these up on its
 * own. What it can do is hand you the Web App Manifest `shortcuts` JSON to serve — see
 * [webAppManifestShortcuts]. An installed PWA then surfaces them in the launcher and taskbar,
 * which is real reach; it just needs one build-time step from you, so `manifest` rather than
 * `os`.
 */
public actual val platformAppIntentsCapabilities: AppIntentsCapabilities =
    AppIntentsCapabilities.ManifestOnly
