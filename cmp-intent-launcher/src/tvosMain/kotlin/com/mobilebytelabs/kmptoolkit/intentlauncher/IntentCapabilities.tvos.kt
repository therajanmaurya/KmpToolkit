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
 * tvOS — nothing. Apple ships no `PHPicker`, no `UIDocumentPicker` and no `CNContactPicker` to
 * tvOS, and arbitrary `openURL` is gated too. Unlike cmp-share there is no useful host-bridge
 * degradation here: a picker is a UI the OS must own, and an app-supplied replacement is a
 * different feature rather than the same one delivered differently.
 *
 * Per-call `onUnsupported { }` remains the escape hatch for callers that have their own answer.
 */
public actual val platformIntentCapabilities: IntentCapabilities = IntentCapabilities.None
