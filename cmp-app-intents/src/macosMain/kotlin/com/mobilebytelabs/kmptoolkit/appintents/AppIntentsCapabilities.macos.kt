/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
package com.mobilebytelabs.kmptoolkit.appintents

/**
 * macOS — manifest plus App Shortcuts registration through the same Swift bridge as iOS.
 */
public actual val platformAppIntentsCapabilities: AppIntentsCapabilities =
    AppIntentsCapabilities.OsIntegrated
