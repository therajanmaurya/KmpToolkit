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
 * watchOS — runtime registry only, for the same reason as tvOS: no Swift bridge surface is
 * defined for this platform, so nothing carries the manifest out of the process.
 */
public actual val platformAppIntentsCapabilities: AppIntentsCapabilities =
    AppIntentsCapabilities.InProcessOnly
