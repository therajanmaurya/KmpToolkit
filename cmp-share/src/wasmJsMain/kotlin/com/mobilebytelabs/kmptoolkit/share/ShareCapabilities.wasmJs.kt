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
 * wasmJs — the same `navigator.share` contract as JS, with image bytes bridged through base64
 * and a file's URI falling back to the clipboard.
 */
public actual val platformShareCapabilities: ShareCapabilities = ShareCapabilities.Full
