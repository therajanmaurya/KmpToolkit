/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
package com.mobilebytelabs.kmptoolkit.intentlauncher

/**
 * wasmWasi — **dynamic**, like cmp-share's tvOS descriptor.
 *
 * A WASI sandbox has no picker of its own, but the host that embedded it usually does. So what is
 * supported depends entirely on whether the host registered a [WasiIntents.handler]:
 * [IntentCapabilities.None] until it does, [IntentCapabilities.Full] after, since a handler may
 * serve any request. A `get()` rather than a constant so `supports()` tracks registration at
 * runtime and a UI hides affordances the host cannot honour.
 */
public actual val platformIntentCapabilities: IntentCapabilities
    get() = if (WasiIntents.isConfigured) IntentCapabilities.Full else IntentCapabilities.None
