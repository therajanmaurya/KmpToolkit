/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
package com.mobilebytelabs.kmptoolkit.deeplink

import com.mobilebytelabs.kmptoolkit.deeplink.testing.FakeDeepLinkManager
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Contract for the injectable facade and its isolated test double.
 *
 * The fake's independence from `DeepLinkHandler` is the point: the real handler is a process-wide
 * `object` with a 500 ms dedup window, so tests sharing it leak state into each other and lose
 * repeated links.
 */
class DeepLinkManagerTest {

    @Test
    fun handle_records_the_uri_and_parses_it() {
        val links = FakeDeepLinkManager()
        links.handle("myapp://open/product/42?ref=email")

        assertEquals(listOf("myapp://open/product/42?ref=email"), links.handled)
        val last = assertNotNull(links.lastReceived.value)
        assertEquals("myapp", last.scheme)
        assertEquals(listOf("product", "42"), last.pathSegments)
        assertEquals("email", last.queryParams["ref"])
    }

    @Test
    fun the_fake_has_no_dedup_window() {
        // The real handler suppresses a repeat within 500ms. A test that emits the same link
        // twice would see one delivery and blame the code under test.
        val links = FakeDeepLinkManager()
        links.handle("myapp://open/a")
        links.handle("myapp://open/a")
        assertEquals(2, links.handled.size)
    }

    @Test
    fun an_initial_link_models_a_cold_start() {
        // A link that launched the app arrives before any collector exists, so `incoming` alone
        // would miss it — `lastReceived` is what a startup path must read.
        val links = FakeDeepLinkManager(initial = DeepLink.parse("myapp://open/product/42"))
        val link = assertNotNull(links.lastReceived.value)
        // `open` is the HOST here, so it is not part of the path — worth pinning, because
        // "myapp://open/x" and "myapp://x" parse to different hosts with the same-looking path.
        assertEquals("open", link.host)
        assertEquals("/product/42", link.path)
        assertTrue(links.handled.isEmpty(), "a launch link was not routed through handle()")
    }

    @Test
    fun incoming_emits_to_a_live_collector() = runTest {
        val links = FakeDeepLinkManager()
        val seen = mutableListOf<DeepLink>()
        // Unconfined so the collector is attached before handle() emits; the fake's buffer is
        // UNLIMITED, so nothing is dropped either way.
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            links.incoming.collect { seen += it }
        }

        links.handle("myapp://open/first")
        links.handle("myapp://open/second")
        job.cancel()

        assertEquals(listOf("/first", "/second"), seen.map { it.path })
    }

    @Test
    fun clear_forgets_the_last_link_so_it_is_not_reprocessed() {
        val links = FakeDeepLinkManager()
        links.handle("myapp://open/a")
        links.clear()

        assertNull(links.lastReceived.value)
        assertEquals(1, links.clearCalls)
    }

    @Test
    fun reset_returns_the_fake_to_a_clean_state() {
        val links = FakeDeepLinkManager(initial = DeepLink.parse("myapp://open/a"))
        links.handle("myapp://open/b")
        links.reset()

        assertTrue(links.handled.isEmpty())
        assertNull(links.lastReceived.value)
        assertEquals(0, links.clearCalls)
    }

    @Test
    fun the_real_impl_exposes_the_singletons_streams() {
        // Both must be the same object: an OS callback reaches DeepLinkHandler directly, so an
        // impl exposing anything else would leave injected consumers listening to nothing.
        val impl = DeepLinkManagerImpl()
        assertEquals(DeepLinkHandler.incoming, impl.incoming)
        assertEquals(DeepLinkHandler.lastReceived, impl.lastReceived)
    }
}
