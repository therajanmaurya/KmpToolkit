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

import androidx.compose.material3.Text
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import com.mobilebytelabs.kmptoolkit.intentlauncher.IntentCapabilities
import com.mobilebytelabs.kmptoolkit.intentlauncher.IntentManager
import com.mobilebytelabs.kmptoolkit.intentlauncher.IntentManagerImpl
import com.mobilebytelabs.kmptoolkit.intentlauncher.IntentOperation
import com.mobilebytelabs.kmptoolkit.intentlauncher.testing.FakeIntentManager
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * What the Compose surface resolves to inside a REAL composition — the default with no provider,
 * the override a provider installs, and what a nested subtree sees. None of that is observable
 * from a direct function call, which is why these render.
 *
 * Abstract so each target can supply its own runner; see the cmp-share-compose scenarios for why
 * the Android host variant needs Robolectric.
 */
@OptIn(ExperimentalTestApi::class)
abstract class IntentManagerCompositionScenarios {

    @Test
    fun no_provider_yields_a_working_manager_rather_than_throwing() = runComposeUiTest {
        var resolved: IntentManager? = null
        setContent {
            resolved = LocalIntentManager.current
            Text("rendered")
        }
        onNodeWithText("rendered").assertIsDisplayed()
        assertIs<IntentManagerImpl>(resolved)
    }

    @Test
    fun provider_overrides_the_default_for_the_subtree() = runComposeUiTest {
        val fake = FakeIntentManager()
        var resolved: IntentManager? = null
        setContent {
            ProvideIntentManager(fake) {
                resolved = rememberIntentManager()
                Text("rendered")
            }
        }
        onNodeWithText("rendered").assertIsDisplayed()
        assertEquals(fake, resolved)
    }

    @Test
    fun a_nested_provider_wins_over_an_outer_one() = runComposeUiTest {
        val outer = FakeIntentManager()
        val inner = FakeIntentManager()
        var seenByOuter: IntentManager? = null
        var seenByInner: IntentManager? = null
        setContent {
            ProvideIntentManager(outer) {
                seenByOuter = rememberIntentManager()
                ProvideIntentManager(inner) { seenByInner = rememberIntentManager() }
                Text("rendered")
            }
        }
        onNodeWithText("rendered").assertIsDisplayed()
        assertEquals(outer, seenByOuter)
        assertEquals(inner, seenByInner)
    }

    @Test
    fun capabilities_come_from_the_provided_manager_not_the_host_platform() = runComposeUiTest {
        // Without this, a test on JVM would report desktop capabilities and hide a tvOS-shaped bug.
        val restricted = FakeIntentManager(capabilities = IntentCapabilities.None)
        var caps: IntentCapabilities? = null
        setContent {
            ProvideIntentManager(restricted) {
                caps = rememberIntentCapabilities()
                Text("rendered")
            }
        }
        onNodeWithText("rendered").assertIsDisplayed()
        assertEquals(IntentCapabilities.None, caps)
    }

    @Test
    fun supports_probe_tracks_the_provided_capabilities() = runComposeUiTest {
        val desktop = FakeIntentManager(IntentCapabilities.desktop(pickMultipleImages = true))
        var canPickDocument = false
        var canPickContact = true
        setContent {
            ProvideIntentManager(desktop) {
                canPickDocument = rememberSupportsIntent(IntentOperation.PickDocument)
                canPickContact = rememberSupportsIntent(IntentOperation.PickContact)
                Text("rendered")
            }
        }
        onNodeWithText("rendered").assertIsDisplayed()
        assertTrue(canPickDocument)
        // The cell every desktop target genuinely lacks — a UI must be able to hide it.
        assertFalse(canPickContact)
    }
}
