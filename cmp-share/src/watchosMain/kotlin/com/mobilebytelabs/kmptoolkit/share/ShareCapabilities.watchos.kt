/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
package com.mobilebytelabs.kmptoolkit.share

/**
 * watchOS — Text and Url are handed to the paired iPhone via `WCSession.transferUserInfo`, whose
 * companion app presents the real share sheet. Image and File are not carried: `transferUserInfo`
 * takes only primitives here, deliberately, because writing the temp file needed for binary
 * handoff goes through `NSData`/`fwrite` signatures whose bit width differs between the 32-bit
 * `watchosArm32` and the 64-bit watch targets, and K/N refuses a single source set spanning both.
 *
 * A bundle still works, carrying its text and link items (first success wins); binary items in a
 * bundle are skipped, which is why `supports()` rejects a bundle that contains one.
 */
public actual val platformShareCapabilities: ShareCapabilities = ShareCapabilities(
    text = true,
    url = true,
    image = false,
    file = false,
    multi = true,
)
