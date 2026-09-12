/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
package com.mobilebytelabs.kmptoolkit.clipboard

// Apple ships no UIPasteboard to tvos — verified by compiling against it, not assumed. These used
// to return false/null unconditionally, so an in-app copy could never be pasted back even within
// the same screen. They now round-trip through [InAppClipboard], which is app-scoped and says so
// via platformClipboardCapabilities.systemWide == false. Install InAppClipboard.onCopy to forward
// the copy somewhere real (a paired phone, a companion app).

actual fun copyToClipboard(text: String): Boolean = InAppClipboard.copy(text)

actual fun getFromClipboard(): String? = InAppClipboard.read()

actual fun hasClipboardText(): Boolean = InAppClipboard.has()

actual fun clearClipboard() {
    InAppClipboard.clear()
}
