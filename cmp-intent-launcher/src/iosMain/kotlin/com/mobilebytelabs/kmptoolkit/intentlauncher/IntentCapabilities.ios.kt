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
 * iOS — `PHPickerViewController`, `UIDocumentPickerViewController`, `CNContactPickerViewController`
 * and `UIApplication.openURL` between them cover the whole surface.
 */
public actual val platformIntentCapabilities: IntentCapabilities = IntentCapabilities.Full
