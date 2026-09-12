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
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * WASI sharing is a real delivery channel, not a stub — these assert that content actually
 * crosses the host boundary.
 *
 * The distinction matters: an earlier version of this actual returned
 * `Failed(UnsupportedPlatform)` for everything, which is indistinguishable from a working
 * implementation whose host simply ignored the payload.
 */
class ShareWasmWasiTest {

    @BeforeTest
    fun setUp() {
        WasiShare.reset()
        // Keep test output clean; the buffer is what the assertions read.
        WasiShare.echoToStdout = false
    }

    @AfterTest
    fun tearDown() = WasiShare.reset()

    @Test
    fun capabilities_are_full_because_every_kind_crosses_the_boundary() {
        assertEquals(ShareCapabilities.Full, platformShareCapabilities)
        val share = ShareManagerImpl()
        assertTrue(share.supports(SharePayload.Text("t")))
        assertTrue(share.supports(SharePayload.Image(byteArrayOf(1), "image/png")))
        assertTrue(share.supports(SharePayload.File("file:///a.pdf", "application/pdf")))
    }

    @Test
    fun text_reaches_the_outbox() = runTest {
        assertIs<ShareResult.Completed>(ShareManagerImpl().shareText("hello"))
        val item = WasiShare.drain().single()
        assertEquals("text", item.kind)
        assertEquals("hello", item.value)
    }

    @Test
    fun image_bytes_survive_the_crossing() = runTest {
        val bytes = byteArrayOf(1, 2, 3, 4)
        ShareManagerImpl().shareImage(bytes, "image/png", filename = "chart.png")
        val item = WasiShare.drain().single()
        assertEquals("image", item.kind)
        assertEquals("image/png", item.mimeType)
        assertEquals("chart.png", item.filename)
        assertTrue(bytes.contentEquals(item.bytes))
    }

    @Test
    fun a_bundle_flattens_into_one_item_per_payload() = runTest {
        ShareManagerImpl().shareFile("file:///r.pdf", "application/pdf", message = "Latest report")
        val items = WasiShare.drain()
        // shareFile(message=) bundles Text + File; the host receives them flat, in order.
        assertEquals(listOf("text", "file"), items.map { it.kind })
        assertEquals("Latest report", items[0].value)
        assertEquals("file:///r.pdf", items[1].value)
    }

    @Test
    fun nested_bundles_flatten_all_the_way_down() = runTest {
        val nested = SharePayload.Multi(
            listOf(SharePayload.Text("a"), SharePayload.Multi(listOf(SharePayload.Url("https://b")))),
        )
        ShareManagerImpl().share(nested)
        assertEquals(listOf("a", "https://b"), WasiShare.drain().map { it.value })
    }

    @Test
    fun a_registered_handler_intercepts_and_bypasses_the_outbox() = runTest {
        val seen = mutableListOf<HostShareItem>()
        WasiShare.handler = {
            seen += it
            true
        }

        assertIs<ShareResult.Completed>(ShareManagerImpl().shareUrl("https://example.com"))
        assertEquals("https://example.com", seen.single().value)
        assertTrue(WasiShare.outbox.isEmpty(), "handler must own delivery, not duplicate into the outbox")
    }

    @Test
    fun a_rejecting_handler_surfaces_as_no_handler() = runTest {
        WasiShare.handler = { false }
        val result = assertIs<ShareResult.Failed>(ShareManagerImpl().shareText("nope"))
        assertEquals(ShareError.NoHandler, result.cause)
    }

    @Test
    fun an_empty_bundle_is_not_a_hollow_success() = runTest {
        val result = assertIs<ShareResult.Failed>(ShareManagerImpl().share(SharePayload.Multi(emptyList())))
        assertIs<ShareError.Unknown>(result.cause)
    }

    @Test
    fun encoding_is_one_parseable_line_even_with_pipes_and_newlines() {
        val item = HostShareItem("text", "a|b\nc", null, "text/plain", null, "T")
        val line = item.encode()
        assertEquals(1, line.lines().size, "an item must never span lines: $line")
        assertContains(line, "cmp-share|text")
        assertContains(line, "value=a\\pb\\nc")
    }
}
