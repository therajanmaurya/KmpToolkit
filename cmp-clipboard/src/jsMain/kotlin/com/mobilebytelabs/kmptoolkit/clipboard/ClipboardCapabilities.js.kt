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

/** JS — `navigator.clipboard`. Writes are allowed inside a user gesture; READS may raise a
 * permission prompt, which is why this is `SystemPermissioned` rather than `System`. */
public actual val platformClipboardCapabilities: ClipboardCapabilities =
    ClipboardCapabilities.SystemPermissioned
