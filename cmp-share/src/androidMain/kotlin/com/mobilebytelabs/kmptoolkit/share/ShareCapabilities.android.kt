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
 * Android — `Intent.ACTION_SEND` carries every payload kind, and `ACTION_SEND_MULTIPLE`
 * carries bundles. FileProvider handles the URI grant for file and image payloads.
 */
public actual val platformShareCapabilities: ShareCapabilities = ShareCapabilities.Full
