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

// WASI is sandboxed and headless, so there is no system clipboard to reach — but a copy can still
// round-trip inside the module, and a host that wants the real thing installs InAppClipboard.onCopy
// / onRead to bridge it. These used to return false/null unconditionally, which was
// indistinguishable from a broken implementation.

actual fun copyToClipboard(text: String): Boolean = InAppClipboard.copy(text)

actual fun getFromClipboard(): String? = InAppClipboard.read()

actual fun hasClipboardText(): Boolean = InAppClipboard.has()

actual fun clearClipboard() {
    InAppClipboard.clear()
}
