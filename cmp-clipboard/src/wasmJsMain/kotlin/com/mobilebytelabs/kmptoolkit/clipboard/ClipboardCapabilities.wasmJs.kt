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

/** wasmJs — the same `navigator.clipboard` contract as JS, reads included. */
public actual val platformClipboardCapabilities: ClipboardCapabilities =
    ClipboardCapabilities.SystemPermissioned
