/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
package com.mobilebytelabs.kmptoolkit.intentlauncher.compose

import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import robolectric.ROBOLECTRIC_SDK

/**
 * Android host run of the shared composition scenarios.
 *
 * Robolectric supplies a real `android.os.Build` (FINGERPRINT = "robolectric"), which Compose's
 * `AndroidComposeUiTestEnvironment` reads to pick an idling strategy. Without it the field is null
 * under the android.jar stub and every composition fails before rendering.
 *
 * This is also the only target where the no-provider default is the Activity-less
 * `IntentManagerImpl` — so it is the one that proves the narrowed-capability path composes.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [ROBOLECTRIC_SDK])
class IntentManagerCompositionAndroidTest : IntentManagerCompositionScenarios()
