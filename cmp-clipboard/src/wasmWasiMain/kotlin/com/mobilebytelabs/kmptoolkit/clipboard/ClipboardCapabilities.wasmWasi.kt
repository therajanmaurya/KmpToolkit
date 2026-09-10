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

/** wasmWasi — sandboxed and headless, so copies round-trip through [InAppClipboard]; a host
 * installs a bridge to reach a real clipboard. */
public actual val platformClipboardCapabilities: ClipboardCapabilities =
    ClipboardCapabilities.InApp
