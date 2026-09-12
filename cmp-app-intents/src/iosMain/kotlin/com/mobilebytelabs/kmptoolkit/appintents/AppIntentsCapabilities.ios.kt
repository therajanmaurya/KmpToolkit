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
 * iOS — the manifest is written for the Swift `CmpAppIntentBridge`, which exposes the intents to
 * Siri, Shortcuts and Spotlight.
 */
public actual val platformAppIntentsCapabilities: AppIntentsCapabilities =
    AppIntentsCapabilities.OsIntegrated
