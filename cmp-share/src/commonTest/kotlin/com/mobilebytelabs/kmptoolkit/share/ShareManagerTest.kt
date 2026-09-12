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

import com.mobilebytelabs.kmptoolkit.share.testing.FakeShareManager
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Contract for the injectable [ShareManager] facade.
 *
 * The convenience methods are interface defaults that all funnel into the single abstract
 * [ShareManager.share], so these assert the PAYLOAD each one builds — that mapping is the whole
 * value of the facade and is where a regression would silently change what gets shared.
 */
class ShareManagerTest {

    // ---- convenience methods build the right payload ---------------------------------------

    @Test
    fun shareText_builds_a_text_payload() = runTest {
        val fake = FakeShareManager()
        fake.shareText("hello")
        assertEquals(SharePayload.Text("hello"), fake.recorded.single().payload)
    }

    @Test
    fun shareUrl_builds_a_url_payload_not_text() = runTest {
        val fake = FakeShareManager()
        fake.shareUrl("https://example.com")
        // Url, not Text — receivers render a link preview only for the Url variant.
        assertEquals(SharePayload.Url("https://example.com"), fake.recorded.single().payload)
    }

    @Test
    fun shareFile_without_message_shares_the_file_alone() = runTest {
        val fake = FakeShareManager()
        fake.shareFile("file:///r.pdf", "application/pdf", filename = "r.pdf")
        assertEquals(
            SharePayload.File("file:///r.pdf", "application/pdf", "r.pdf"),
            fake.recorded.single().payload,
        )
    }

    @Test
    fun shareFile_with_message_bundles_message_then_file() = runTest {
        val fake = FakeShareManager()
        fake.shareFile("file:///r.pdf", "application/pdf", message = "Latest report")

        // This is the case that previously forced callers to assemble SharePayload.Multi by hand.
        val multi = assertIs<SharePayload.Multi>(fake.recorded.single().payload)
        assertEquals(
            listOf(
                SharePayload.Text("Latest report"),
                SharePayload.File("file:///r.pdf", "application/pdf", null),
            ),
            multi.items,
        )
    }

    @Test
    fun shareFile_with_blank_message_does_not_bundle() = runTest {
        val fake = FakeShareManager()
        fake.shareFile("file:///r.pdf", "application/pdf", message = "")
        // An empty message must not produce a Multi carrying an empty Text item.
        assertIs<SharePayload.File>(fake.recorded.single().payload)
    }

    @Test
    fun shareImage_defaults_to_png_and_bundles_a_message() = runTest {
        val fake = FakeShareManager()
        val bytes = byteArrayOf(1, 2, 3)
        fake.shareImage(bytes, message = "chart")

        val multi = assertIs<SharePayload.Multi>(fake.recorded.single().payload)
        assertEquals(SharePayload.Text("chart"), multi.items[0])
        val image = assertIs<SharePayload.Image>(multi.items[1])
        assertEquals("image/png", image.mimeType)
        assertTrue(bytes.contentEquals(image.bytes))
    }

    @Test
    fun options_are_passed_through_untouched() = runTest {
        val fake = FakeShareManager()
        val options = ShareOptions(chooserTitle = "Send", targetPackage = "com.whatsapp")
        fake.shareText("hi", options)
        assertSame(options, fake.recorded.single().options)
    }

    // ---- supports() derives from capabilities ----------------------------------------------

    @Test
    fun supports_follows_capabilities_for_every_payload_kind() {
        val full = FakeShareManager(ShareCapabilities.Full)
        assertTrue(full.supports(SharePayload.Text("t")))
        assertTrue(full.supports(SharePayload.Url("u")))
        assertTrue(full.supports(SharePayload.Image(byteArrayOf(), "image/png")))
        assertTrue(full.supports(SharePayload.File("f", "text/plain")))

        val tvos = FakeShareManager(ShareCapabilities.TextAndUrlOnly)
        assertTrue(tvos.supports(SharePayload.Text("t")))
        assertTrue(tvos.supports(SharePayload.Url("u")))
        assertFalse(tvos.supports(SharePayload.Image(byteArrayOf(), "image/png")))
        assertFalse(tvos.supports(SharePayload.File("f", "text/plain")))
    }

    @Test
    fun a_bundle_is_supported_only_when_every_item_is() {
        // Windows: bundles work, but binary items inside one are dropped — so a bundle
        // containing an image is NOT honestly shareable, even though `multi` is true.
        val windows = FakeShareManager(
            ShareCapabilities(text = true, url = true, image = false, file = false, multi = true),
        )
        assertTrue(windows.supports(SharePayload.Multi(listOf(SharePayload.Text("a"), SharePayload.Url("b")))))
        assertFalse(
            windows.supports(
                SharePayload.Multi(listOf(SharePayload.Text("a"), SharePayload.Image(byteArrayOf(), "image/png"))),
            ),
        )
    }

    @Test
    fun a_bundle_is_unsupported_when_bundling_itself_is_unsupported() {
        val tvos = FakeShareManager(ShareCapabilities.TextAndUrlOnly)
        // Every item is individually fine; tvOS still cannot bundle.
        assertFalse(tvos.supports(SharePayload.Multi(listOf(SharePayload.Text("a")))))
    }

    @Test
    fun nested_bundles_are_checked_recursively() {
        val windows = FakeShareManager(
            ShareCapabilities(text = true, url = true, image = false, file = false, multi = true),
        )
        val nested = SharePayload.Multi(
            listOf(SharePayload.Multi(listOf(SharePayload.File("f", "text/plain")))),
        )
        assertFalse(windows.supports(nested))
    }

    // ---- results ----------------------------------------------------------------------------

    @Test
    fun scripted_results_are_returned_in_order_then_default_to_completed() = runTest {
        val fake = FakeShareManager()
        fake.scriptError(ShareError.UserGestureMissing)
        fake.scriptResult(ShareResult.Cancelled)

        val failed = assertIs<ShareResult.Failed>(fake.shareText("a"))
        assertEquals(ShareError.UserGestureMissing, failed.cause)
        assertIs<ShareResult.Cancelled>(fake.shareText("b"))
        assertIs<ShareResult.Completed>(fake.shareText("c"))
        assertEquals(3, fake.recorded.size)
    }

    @Test
    fun reset_clears_history_and_script() = runTest {
        val fake = FakeShareManager()
        fake.scriptResult(ShareResult.Cancelled)
        fake.shareText("a")
        fake.reset()

        assertTrue(fake.recorded.isEmpty())
        // The queued Cancelled must be gone too, or the next test-phase call would consume it.
        assertIs<ShareResult.Completed>(fake.shareText("b"))
    }

    // ---- the real implementation -------------------------------------------------------------

    @Test
    fun impl_reports_this_platforms_capabilities() {
        val impl = ShareManagerImpl()
        assertEquals(platformShareCapabilities, impl.capabilities)
    }

    @Test
    fun supports_agrees_with_the_descriptor_on_this_target() {
        // The invariant that holds on ALL 21 targets. An earlier version of this test asserted
        // that text and url are shareable everywhere; adding wasmWasi — which has no share
        // surface of any kind — made that false, and this test is what caught it.
        val impl = ShareManagerImpl()
        assertEquals(impl.capabilities.text, impl.supports(SharePayload.Text("t")))
        assertEquals(impl.capabilities.url, impl.supports(SharePayload.Url("u")))
        assertEquals(impl.capabilities.image, impl.supports(SharePayload.Image(byteArrayOf(), "image/png")))
        assertEquals(impl.capabilities.file, impl.supports(SharePayload.File("f", "text/plain")))
    }
}
