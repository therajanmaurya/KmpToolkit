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

/** tvOS — Apple ships no `UIPasteboard` here, so copies round-trip through [InAppClipboard]
 * and never leave the app unless you install a bridge. */
public actual val platformClipboardCapabilities: ClipboardCapabilities =
    ClipboardCapabilities.InApp
