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
 * Windows — PowerShell `OpenFileDialog` / `SaveFileDialog` over `_popen`, and `cmd /c start` for
 * URIs. Multi-select comes from `OpenFileDialog.Multiselect`.
 *
 * These were all `UnsupportedPlatform` until the PowerShell route replaced the blocked
 * `GetOpenFileNameW` cinterop — see `IntentLauncher.mingw.kt`. Windows has no ambient contact
 * picker to shell out to, so that one is genuinely absent rather than merely unimplemented.
 */
public actual val platformIntentCapabilities: IntentCapabilities = IntentCapabilities.desktop(pickMultipleImages = true)
