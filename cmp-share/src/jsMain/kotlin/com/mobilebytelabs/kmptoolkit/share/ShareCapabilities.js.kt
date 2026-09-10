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
 * JS — `navigator.share` with a clipboard fallback.
 *
 * Image goes through Web Share Level 2 (`files:`), checked with `canShare` at call time so a
 * browser without it reports [ShareError.NoHandler] rather than failing silently. A File cannot
 * ride the Web Share API — it wants `File` objects and fetching the URI would need CORS
 * permission the library cannot assume — so its URI goes on the clipboard instead, which is
 * still something the user can paste anywhere.
 */
public actual val platformShareCapabilities: ShareCapabilities = ShareCapabilities.Full
