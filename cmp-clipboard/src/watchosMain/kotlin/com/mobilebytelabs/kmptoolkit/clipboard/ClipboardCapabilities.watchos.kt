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

/** watchOS — Apple ships no `UIPasteboard` here either; copies round-trip through
 * [InAppClipboard]. Bridge to the paired phone over `WCSession` to reach further. */
public actual val platformClipboardCapabilities: ClipboardCapabilities =
    ClipboardCapabilities.InApp
