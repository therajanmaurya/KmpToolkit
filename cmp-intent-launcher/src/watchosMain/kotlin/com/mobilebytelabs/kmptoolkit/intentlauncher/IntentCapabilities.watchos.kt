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
 * watchOS — `WKExtension.openSystemURL` genuinely launches http/https/mailto from the watch.
 *
 * Nothing else: watchOS ships no Photos picker, no `UIDocumentPicker` and no `CNContactPicker`,
 * and there is no programmatic settings deep-link. Apps needing picker UX delegate to the paired
 * iPhone over `WCSession`, the same route cmp-share takes.
 */
public actual val platformIntentCapabilities: IntentCapabilities
    get() = IntentCapabilities(
        viewUri = true,
        pickImage = false,
        pickMultipleImages = false,
        pickDocument = false,
        pickContact = false,
        openAppSettings = false,
        createDocument = false,
    )
