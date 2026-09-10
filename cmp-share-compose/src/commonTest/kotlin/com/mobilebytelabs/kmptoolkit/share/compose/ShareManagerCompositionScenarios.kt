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

import androidx.compose.material3.Text
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import com.mobilebytelabs.kmptoolkit.share.ShareCapabilities
import com.mobilebytelabs.kmptoolkit.share.ShareManager
import com.mobilebytelabs.kmptoolkit.share.ShareManagerImpl
import com.mobilebytelabs.kmptoolkit.share.SharePayload
import com.mobilebytelabs.kmptoolkit.share.testing.FakeShareManager
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * What the Compose surface of cmp-share resolves to inside a REAL composition.
 *
 * These render through `runComposeUiTest` rather than calling the functions directly, because
 * every claim here is about composition behaviour — the default when no provider exists, the
 * override a provider installs, and what a nested subtree sees. A direct call cannot observe any
 * of that.
 *
 * ABSTRACT ON PURPOSE. `androidx.compose.ui.test`'s Android environment reads
 * `android.os.Build.FINGERPRINT` to pick an idling strategy, and that static field is null under
 * the plain android.jar stub, so the Android host variant must run under Robolectric — which
 * needs a JUnit4 `@RunWith` that a commonTest class cannot carry. The scenarios are written once
 * here; each target's subclass supplies its own runner.
 */
@OptIn(ExperimentalTestApi::class)
abstract class ShareManagerCompositionScenarios {

    @Test
    fun no_provider_yields_a_working_manager_rather_than_throwing() = runComposeUiTest {
        // The deliberate divergence from LocalNetworkMonitor: sharing is zero-config, so the
        // default must be usable, not an error().
        var resolved: ShareManager? = null
        setContent {
            resolved = LocalShareManager.current
            Text("rendered")
        }
        onNodeWithText("rendered").assertIsDisplayed()
        assertIs<ShareManagerImpl>(resolved)
    }

    @Test
    fun provider_overrides_the_default_for_the_subtree() = runComposeUiTest {
        val fake = FakeShareManager()
        var resolved: ShareManager? = null
        setContent {
            ProvideShareManager(fake) {
                resolved = rememberShareManager()
                Text("rendered")
            }
        }
        onNodeWithText("rendered").assertIsDisplayed()
        assertEquals(fake, resolved)
    }

    @Test
    fun a_nested_provider_wins_over_an_outer_one() = runComposeUiTest {
        val outer = FakeShareManager()
        val inner = FakeShareManager()
        var seenByInner: ShareManager? = null
        var seenByOuter: ShareManager? = null
        setContent {
            ProvideShareManager(outer) {
                seenByOuter = rememberShareManager()
                ProvideShareManager(inner) {
                    seenByInner = rememberShareManager()
                }
                Text("rendered")
            }
        }
        onNodeWithText("rendered").assertIsDisplayed()
        assertEquals(outer, seenByOuter)
        assertEquals(inner, seenByInner)
    }

    @Test
    fun capabilities_come_from_the_provided_manager_not_the_host_platform() = runComposeUiTest {
        // A test running on JVM/Android would otherwise report Full and hide a tvOS-shaped bug.
        val restricted = FakeShareManager(capabilities = ShareCapabilities.TextAndUrlOnly)
        var caps: ShareCapabilities? = null
        setContent {
            ProvideShareManager(restricted) {
                caps = rememberShareCapabilities()
                Text("rendered")
            }
        }
        onNodeWithText("rendered").assertIsDisplayed()
        assertEquals(ShareCapabilities.TextAndUrlOnly, caps)
    }

    @Test
    fun sharing_from_composition_records_the_payload() = runComposeUiTest {
        val fake = FakeShareManager()
        var manager: ShareManager? = null
        setContent {
            ProvideShareManager(fake) {
                manager = rememberShareManager()
                Text("rendered")
            }
        }
        onNodeWithText("rendered").assertIsDisplayed()

        // The composition handed back the provided instance, so a share driven from a click
        // handler lands on the fake.
        kotlinx.coroutines.test.runTest {
            manager!!.shareUrl("https://example.com/article")
        }
        assertEquals(SharePayload.Url("https://example.com/article"), fake.recorded.single().payload)
    }

    @Test
    fun the_platform_default_can_always_share_text_and_links() = runComposeUiTest {
        var caps: ShareCapabilities? = null
        setContent {
            caps = rememberShareCapabilities()
            Text("rendered")
        }
        onNodeWithText("rendered").assertIsDisplayed()
        // With no provider these are the REAL capabilities of the target running this test.
        assertTrue(caps!!.text)
        assertTrue(caps!!.url)
    }
}
