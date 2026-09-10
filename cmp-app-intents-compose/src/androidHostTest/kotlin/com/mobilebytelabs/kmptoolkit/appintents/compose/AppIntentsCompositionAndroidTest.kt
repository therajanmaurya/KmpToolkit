/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
package com.mobilebytelabs.kmptoolkit.appintents.compose

import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import robolectric.ROBOLECTRIC_SDK

/**
 * Android host run of the shared scenarios. Robolectric supplies a real `android.os.Build`
 * (FINGERPRINT = "robolectric"), which Compose's test environment reads to pick an idling
 * strategy — null under the plain android.jar stub, which kills every composition.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [ROBOLECTRIC_SDK])
class AppIntentsCompositionAndroidTest : AppIntentsCompositionScenarios()
