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
 * wasmJs — the same browser surface as JS: `<input type=file>`, `window.open`, and
 * `showSaveFilePicker` where the File System Access API exists.
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
