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

import androidx.compose.material3.Text
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import com.mobilebytelabs.kmptoolkit.appintents.AppIntentsCapabilities
import com.mobilebytelabs.kmptoolkit.appintents.AppIntentsManager
import com.mobilebytelabs.kmptoolkit.appintents.AppIntentsManagerImpl
import com.mobilebytelabs.kmptoolkit.appintents.testing.FakeAppIntentsManager
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * What the Compose surface resolves to inside a REAL composition. Abstract so each target supplies
 * its own runner; the Android host variant needs Robolectric for the same `Build.FINGERPRINT`
 * reason documented in the cmp-share-compose scenarios.
 */
@OptIn(ExperimentalTestApi::class)
abstract class AppIntentsCompositionScenarios {

    @Test
    fun no_provider_yields_a_working_manager_rather_than_throwing() = runComposeUiTest {
        var resolved: AppIntentsManager? = null
        setContent {
            resolved = LocalAppIntentsManager.current
            Text("rendered")
        }
        onNodeWithText("rendered").assertIsDisplayed()
        assertIs<AppIntentsManagerImpl>(resolved)
    }

    @Test
    fun provider_overrides_the_default_for_the_subtree() = runComposeUiTest {
        val fake = FakeAppIntentsManager()
        var resolved: AppIntentsManager? = null
        setContent {
            ProvideAppIntentsManager(fake) {
                resolved = rememberAppIntentsManager()
                Text("rendered")
            }
        }
        onNodeWithText("rendered").assertIsDisplayed()
        assertEquals(fake, resolved)
    }

    @Test
    fun a_nested_provider_wins_over_an_outer_one() = runComposeUiTest {
        val outer = FakeAppIntentsManager()
        val inner = FakeAppIntentsManager()
        var seenByOuter: AppIntentsManager? = null
        var seenByInner: AppIntentsManager? = null
        setContent {
            ProvideAppIntentsManager(outer) {
                seenByOuter = rememberAppIntentsManager()
                ProvideAppIntentsManager(inner) { seenByInner = rememberAppIntentsManager() }
                Text("rendered")
            }
        }
        onNodeWithText("rendered").assertIsDisplayed()
        assertEquals(outer, seenByOuter)
        assertEquals(inner, seenByInner)
    }

    @Test
    fun reach_comes_from_the_provided_manager_not_the_host_platform() = runComposeUiTest {
        // Without this, a test on JVM would report JVM reach and hide a tvOS-shaped bug.
        val inProcess = FakeAppIntentsManager(AppIntentsCapabilities.InProcessOnly)
        var reachesOs = true
        var caps: AppIntentsCapabilities? = null
        setContent {
            ProvideAppIntentsManager(inProcess) {
                reachesOs = rememberAppIntentsReachOs()
                caps = rememberAppIntentsCapabilities()
                Text("rendered")
            }
        }
        onNodeWithText("rendered").assertIsDisplayed()
        // The whole point: no voice onboarding where the phrase could never work.
        assertFalse(reachesOs)
        assertEquals(AppIntentsCapabilities.InProcessOnly, caps)
    }

    @Test
    fun os_integrated_reach_is_reported_when_the_manager_has_it() = runComposeUiTest {
        var reachesOs = false
        setContent {
            ProvideAppIntentsManager(FakeAppIntentsManager(AppIntentsCapabilities.OsIntegrated)) {
                reachesOs = rememberAppIntentsReachOs()
                Text("rendered")
            }
        }
        onNodeWithText("rendered").assertIsDisplayed()
        assertTrue(reachesOs)
    }
}
