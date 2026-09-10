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
 * tvOS — runtime registry only.
 *
 * `AppIntentsCallback`, the ObjC-bridged singleton the Swift layer consumes, is defined for iOS
 * and macOS only, and tvOS App Intents have no CoreSpotlight or AppShortcutsProvider to reach
 * anyway. An app that wants tvOS integration ships its own bridge calling
 * `AppIntentsRuntime.invoke(id, params)`.
 */
public actual val platformAppIntentsCapabilities: AppIntentsCapabilities =
    AppIntentsCapabilities.InProcessOnly
