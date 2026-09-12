/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
package com.mobilebytelabs.kmptoolkit.share.compose

import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import robolectric.ROBOLECTRIC_SDK

/**
 * Android host run of the shared composition scenarios.
 *
 * Robolectric supplies a real `android.os.Build` (FINGERPRINT = "robolectric"), which is what
 * Compose's `AndroidComposeUiTestEnvironment` reads to pick an idling strategy. Without it the
 * field is null under the android.jar stub and every composition fails before rendering.
 *
 * The emulated API level comes from `robolectricSdk` in gradle/libs.versions.toml via
 * robolectric-host-test.gradle.kts, which code-generates ROBOLECTRIC_SDK.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [ROBOLECTRIC_SDK])
class ShareManagerCompositionAndroidTest : ShareManagerCompositionScenarios()
