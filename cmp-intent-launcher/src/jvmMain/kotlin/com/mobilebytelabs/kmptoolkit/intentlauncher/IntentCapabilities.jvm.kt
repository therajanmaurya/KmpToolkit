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
 * JVM desktop — AWT `FileDialog` for pickers, `JFileChooser` for saving, `Desktop.browse` for URIs.
 *
 * `pickMultipleImages` is false because the actual never wires `FileDialog.setMultipleMode`; the
 * native desktop targets do manage it. A JDK running headless reports `NoHandler` per call.
 */
public actual val platformIntentCapabilities: IntentCapabilities = IntentCapabilities.desktop(
    pickMultipleImages = false,
)
