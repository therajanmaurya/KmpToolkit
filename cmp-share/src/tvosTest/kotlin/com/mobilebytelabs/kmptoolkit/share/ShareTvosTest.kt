/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
package com.mobilebytelabs.kmptoolkit.share

import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * tvOS routes sharing to an app-supplied handler, and capabilities track whether one exists.
 *
 * The regression these lock down is specific and was live in the shipped code: `Share.share`
 * used to probe for an Objective-C bridge class and return [ShareResult.Completed] **without
 * dispatching anything**, so every caller was told the share worked while nothing happened.
 */
class ShareTvosTest {

    @BeforeTest
    fun setUp() = TvosShare.reset()

    @AfterTest
    fun tearDown() = TvosShare.reset()

    @Test
    fun without_a_handler_nothing_is_claimed_to_be_shareable() {
        assertEquals(ShareCapabilities.None, platformShareCapabilities)
        assertFalse(ShareManagerImpl().supports(SharePayload.Text("t")))
    }

    @Test
    fun without_a_handler_sharing_fails_instead_of_reporting_success() = runTest {
        // The exact regression: this used to return Completed.
        val result = assertIs<ShareResult.Failed>(ShareManagerImpl().shareText("hello"))
        val cause = assertIs<ShareError.Unknown>(result.cause)
        assertTrue(
            cause.message.contains("TvosShare.handler"),
            "the error must say how to fix it, got: ${cause.message}",
        )
    }

    @Test
    fun registering_a_handler_opens_up_capabilities() {
        TvosShare.handler = { true }
        assertEquals(ShareCapabilities.Full, platformShareCapabilities)
        assertTrue(ShareManagerImpl().supports(SharePayload.Image(byteArrayOf(1), "image/png")))
    }

    @Test
    fun the_handler_receives_the_payload() = runTest {
        val seen = mutableListOf<HostShareItem>()
        TvosShare.handler = {
            seen += it
            true
        }

        assertIs<ShareResult.Completed>(ShareManagerImpl().shareUrl("https://example.com"))
        assertEquals("url", seen.single().kind)
        assertEquals("https://example.com", seen.single().value)
    }

    @Test
    fun a_bundle_arrives_flattened_and_in_order() = runTest {
        val seen = mutableListOf<HostShareItem>()
        TvosShare.handler = {
            seen += it
            true
        }

        ShareManagerImpl().shareFile("file:///r.pdf", "application/pdf", message = "Report")
        assertEquals(listOf("text", "file"), seen.map { it.kind })
    }

    @Test
    fun a_declining_handler_surfaces_as_no_handler() = runTest {
        TvosShare.handler = { false }
        val result = assertIs<ShareResult.Failed>(ShareManagerImpl().shareText("t"))
        assertEquals(ShareError.NoHandler, result.cause)
    }
}
