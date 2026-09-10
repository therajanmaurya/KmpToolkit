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

import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * wasmWasi routes intents across the host boundary, and capabilities track whether a host is
 * listening — so a caller can branch instead of discovering it by failing.
 */
class WasiIntentsTest {

    @BeforeTest
    fun setUp() = WasiIntents.reset()

    @AfterTest
    fun tearDown() = WasiIntents.reset()

    @Test
    fun without_a_handler_nothing_is_claimed_to_be_supported() {
        assertEquals(IntentCapabilities.None, platformIntentCapabilities)
        assertFalse(IntentManagerImpl().supports(IntentOperation.PickImage))
    }

    @Test
    fun without_a_handler_the_error_says_how_to_fix_it() = runTest {
        val result = assertIs<IntentResult.Failed>(IntentManagerImpl().pickImage())
        val cause = assertIs<IntentError.Unknown>(result.cause)
        assertTrue(cause.message.contains("WasiIntents.handler"), "got: ${cause.message}")
    }

    @Test
    fun registering_a_handler_opens_up_capabilities() {
        WasiIntents.handler = { IntentResult.Cancelled }
        assertEquals(IntentCapabilities.Full, platformIntentCapabilities)
        assertTrue(IntentManagerImpl().supports(IntentOperation.PickDocument))
    }

    @Test
    fun the_handler_receives_the_request_and_its_result_is_returned() = runTest {
        val seen = mutableListOf<HostIntentRequest>()
        WasiIntents.handler = {
            seen += it
            IntentResult.Ok(IntentData(uri = "file:///picked.png"))
        }

        val result = assertIs<IntentResult.Ok>(IntentManagerImpl().pickImage())
        assertEquals("file:///picked.png", result.data?.uri)
        assertTrue(seen.single().expectsResult)
    }

    @Test
    fun system_intents_arrive_under_synthetic_actions() = runTest {
        val seen = mutableListOf<HostIntentRequest>()
        WasiIntents.handler = {
            seen += it
            IntentResult.Ok(null)
        }

        val manager = IntentManagerImpl()
        manager.openAppSettings()
        manager.createDocument("report.pdf", "application/pdf")

        assertEquals(HostIntentRequest.ACTION_APP_SETTINGS, seen[0].action)
        assertEquals(HostIntentRequest.ACTION_CREATE_DOCUMENT, seen[1].action)
        // A host implements one function, so the filename has to travel with the request.
        assertEquals("report.pdf", seen[1].extras[HostIntentRequest.EXTRA_SUGGESTED_NAME])
        assertEquals("application/pdf", seen[1].type)
    }
}
