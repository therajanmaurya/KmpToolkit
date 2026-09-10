/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
package com.mobilebytelabs.kmptoolkit.intentlauncher

import com.mobilebytelabs.kmptoolkit.intentlauncher.testing.FakeIntentManager
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Contract for the injectable [IntentManager] facade.
 *
 * The convenience methods are interface defaults that all funnel into the single abstract
 * [IntentManager.launch], so these assert the REQUEST each one builds — that mapping is the whole
 * value of the facade, and a regression there silently launches the wrong intent.
 */
class IntentManagerTest {

    // ---- convenience methods build the right request -----------------------------------------

    @Test
    fun viewUri_builds_a_view_action_carrying_the_uri() = runTest {
        val fake = FakeIntentManager()
        fake.viewUri("https://example.com")

        val request = fake.recorded.single()
        assertEquals("VIEW", request.action)
        assertEquals("https://example.com", request.data)
        assertFalse(request.expectsResult, "a plain view is fire-and-forget")
    }

    @Test
    fun pickImage_requests_a_result_and_filters_to_images() = runTest {
        val fake = FakeIntentManager()
        fake.pickImage()

        val request = fake.recorded.single()
        assertEquals("image/" + "*", request.type)
        assertTrue(request.expectsResult, "a picker must expect a result back")
    }

    @Test
    fun pickDocument_defaults_to_any_type_but_honours_a_filter() = runTest {
        val fake = FakeIntentManager()
        fake.pickDocument()
        fake.pickDocument("application/pdf")

        assertEquals("*/" + "*", fake.recorded[0].type)
        assertEquals("application/pdf", fake.recorded[1].type)
    }

    @Test
    fun every_picker_expects_a_result() = runTest {
        val fake = FakeIntentManager()
        fake.pickImage()
        fake.pickMultipleImages()
        fake.pickDocument()
        fake.pickContact()

        assertTrue(fake.recorded.all { it.expectsResult }, "expectsResult drives real dispatch")
    }

    // ---- capabilities gate the UI ------------------------------------------------------------

    @Test
    fun supports_follows_capabilities_for_every_operation() {
        val full = FakeIntentManager(IntentCapabilities.Full)
        IntentOperation.entries.forEach { assertTrue(full.supports(it), "Full should support $it") }

        val none = FakeIntentManager(IntentCapabilities.None)
        IntentOperation.entries.forEach { assertFalse(none.supports(it), "None should refuse $it") }
    }

    @Test
    fun a_desktop_has_file_dialogs_but_no_contact_picker() {
        val desktop = FakeIntentManager(IntentCapabilities.desktop(pickMultipleImages = true))
        assertTrue(desktop.supports(IntentOperation.PickDocument))
        assertTrue(desktop.supports(IntentOperation.CreateDocument))
        // The one every desktop target genuinely lacks.
        assertFalse(desktop.supports(IntentOperation.PickContact))
    }

    @Test
    fun the_jvm_shape_differs_from_native_desktop_only_in_multi_select() {
        val jvm = IntentCapabilities.desktop(pickMultipleImages = false)
        val native = IntentCapabilities.desktop(pickMultipleImages = true)
        assertEquals(jvm, native.copy(pickMultipleImages = false))
    }

    // ---- results ------------------------------------------------------------------------------

    @Test
    fun an_unscripted_call_reports_cancelled_rather_than_success() = runTest {
        // Cancelled is what a real picker gives when the user backs out, and the branch call
        // sites most often forget — so it is the safer default for a fake.
        assertIs<IntentResult.Cancelled>(FakeIntentManager().pickImage())
    }

    @Test
    fun scripted_results_come_back_in_order() = runTest {
        val fake = FakeIntentManager()
        fake.scriptResult(IntentResult.Ok(IntentData(uri = "file:///a.png")))
        fake.scriptResult(IntentResult.Failed(IntentError.NoHandler))

        assertEquals("file:///a.png", assertIs<IntentResult.Ok>(fake.pickImage()).data?.uri)
        assertEquals(IntentError.NoHandler, assertIs<IntentResult.Failed>(fake.pickImage()).cause)
        assertIs<IntentResult.Cancelled>(fake.pickImage())
    }

    @Test
    fun reset_clears_history_and_script() = runTest {
        val fake = FakeIntentManager()
        fake.scriptResult(IntentResult.Ok(null))
        fake.pickImage()
        fake.reset()

        assertTrue(fake.recorded.isEmpty())
        assertIs<IntentResult.Cancelled>(fake.pickImage())
    }

    // ---- the real implementation ---------------------------------------------------------------

    @Test
    fun impl_reports_this_platforms_capabilities_when_a_launcher_exists() {
        val impl = IntentManagerImpl()
        // Android's default launcher is null, which deliberately narrows the surface; everywhere
        // else the manager must report exactly what the platform descriptor says.
        if (defaultIntentLauncher() != null) {
            assertEquals(platformIntentCapabilities, impl.capabilities)
        } else {
            assertFalse(impl.capabilities.pickImage, "no launcher must not advertise pickers")
        }
    }

    @Test
    fun without_a_launcher_pickers_are_hidden_but_system_intents_survive() {
        // The Android-without-an-Activity state, asserted on every target by passing null.
        val impl = IntentManagerImpl(launcher = null)
        assertFalse(impl.supports(IntentOperation.PickImage))
        assertFalse(impl.supports(IntentOperation.ViewUri))
        // These two run through SystemIntents, which needs no Activity.
        assertEquals(
            platformIntentCapabilities.openAppSettings,
            impl.supports(IntentOperation.OpenAppSettings),
        )
        assertEquals(
            platformIntentCapabilities.createDocument,
            impl.supports(IntentOperation.CreateDocument),
        )
    }

    @Test
    fun without_a_launcher_launch_explains_itself_rather_than_failing_blankly() = runTest {
        val result = assertIs<IntentResult.Failed>(IntentManagerImpl(launcher = null).pickImage())
        val cause = assertIs<IntentError.Unknown>(result.cause)
        assertTrue(
            cause.message.contains("intentLauncher()"),
            "the error must name the fix, got: ${cause.message}",
        )
    }
}
