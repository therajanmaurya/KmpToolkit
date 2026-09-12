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
 * JS — a hidden `<input type=file>` for pickers, `window.open` for URIs and
 * `showSaveFilePicker` for saving.
 *
 * No app settings: a page has no app-scoped settings screen to deep-link to. No contact picker
 * (the Contact Picker API is Chromium-on-Android only, so relying on it would misreport support
 * on every other browser).
 */
public actual val platformIntentCapabilities: IntentCapabilities
    get() = IntentCapabilities(
        viewUri = true,
        pickImage = true,
        pickMultipleImages = true,
        pickDocument = true,
        pickContact = false,
        openAppSettings = false,
        createDocument = true,
    )
