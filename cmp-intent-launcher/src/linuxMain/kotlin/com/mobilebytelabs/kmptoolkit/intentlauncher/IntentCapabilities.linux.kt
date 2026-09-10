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
 * Linux — `zenity --file-selection` for pickers (including `--multiple`) and `xdg-open` for URIs.
 * Requires `xdg-utils` and zenity. No desktop-agnostic contact picker exists, so contacts stay out.
 */
public actual val platformIntentCapabilities: IntentCapabilities = IntentCapabilities.desktop(pickMultipleImages = true)
