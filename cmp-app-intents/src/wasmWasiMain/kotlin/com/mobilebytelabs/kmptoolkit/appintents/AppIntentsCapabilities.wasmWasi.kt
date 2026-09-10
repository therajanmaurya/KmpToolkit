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
 * wasmWasi — **dynamic**.
 *
 * A WASI host that registers a [WasiAppIntents.onRegister] handler decides what registration
 * means, so reach is `os` once one exists. Without a handler the manifest still leaves the
 * process on stdout, which is `manifest` reach rather than nothing.
 */
public actual val platformAppIntentsCapabilities: AppIntentsCapabilities
    get() = if (WasiAppIntents.isConfigured) {
        AppIntentsCapabilities.OsIntegrated
    } else {
        AppIntentsCapabilities.ManifestOnly
    }
