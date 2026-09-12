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
 * macOS — `NSOpenPanel` / `NSSavePanel` for files and `NSWorkspace.openURL` for links.
 * `CNContactPicker` is not cleanly exposed to K/N on macOS (ADR-09), so contacts stay out.
 */
public actual val platformIntentCapabilities: IntentCapabilities = IntentCapabilities.desktop(pickMultipleImages = true)
