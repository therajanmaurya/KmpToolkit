/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
package com.mobilebytelabs.kmptoolkit.share

/**
 * tvOS — **dynamic**, unlike every other target.
 *
 * tvOS has no share sheet and no pasteboard, so what can be shared depends entirely on whether
 * the app registered a [TvosShare.handler]: [ShareCapabilities.None] until it does, then
 * [ShareCapabilities.Full], since a handler may accept any kind. A `get()` rather than a constant
 * so `supports()` tracks registration at runtime and a UI hides its share affordance until the
 * app is actually able to honour it.
 */
public actual val platformShareCapabilities: ShareCapabilities
    get() = if (TvosShare.isConfigured) ShareCapabilities.Full else ShareCapabilities.None
