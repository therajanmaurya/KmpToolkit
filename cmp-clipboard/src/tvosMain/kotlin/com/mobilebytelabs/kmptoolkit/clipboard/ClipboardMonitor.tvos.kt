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

/**
 * tvOS has no system clipboard to poll, so the monitor watches [InAppClipboard] instead — see
 * [InAppClipboardMonitor], which drives the same state machine as every other platform.
 *
 * The previous `TvosClipboardMonitor` was a no-op whose `start()` did nothing, leaving the state stuck
 * on Idle and failing the shared monitor tests in commonTest. It has been deleted rather than left
 * beside its replacement: an unused class beside a working one reads as a live alternative.
 */
actual fun createClipboardMonitor(): ClipboardMonitor = InAppClipboardMonitor()
