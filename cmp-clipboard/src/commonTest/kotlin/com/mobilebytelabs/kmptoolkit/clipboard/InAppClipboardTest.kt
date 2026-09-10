/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
package com.mobilebytelabs.kmptoolkit.clipboard

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * [InAppClipboard] is what tvOS, watchOS and wasmWasi copy through, so its round-trip is the whole
 * contract on those targets — and it runs everywhere, so the behaviour is pinned regardless of
 * which platform the suite executes on.
 */
class InAppClipboardTest {

    @BeforeTest
    fun setUp() = InAppClipboard.reset()

    @AfterTest
    fun tearDown() = InAppClipboard.reset()

    @Test
    fun a_copy_can_be_read_back() {
        // The regression: on tvOS/watchOS/wasmWasi this used to return null forever.
        assertTrue(InAppClipboard.copy("hello"))
        assertEquals("hello", InAppClipboard.read())
        assertTrue(InAppClipboard.has())
    }

    @Test
    fun an_empty_buffer_has_nothing() {
        assertNull(InAppClipboard.read())
        assertFalse(InAppClipboard.has())
    }

    @Test
    fun copying_replaces_the_previous_value() {
        InAppClipboard.copy("first")
        InAppClipboard.copy("second")
        assertEquals("second", InAppClipboard.read())
    }

    @Test
    fun clear_empties_it() {
        InAppClipboard.copy("hello")
        InAppClipboard.clear()
        assertNull(InAppClipboard.read())
        assertFalse(InAppClipboard.has())
    }

    @Test
    fun an_empty_string_does_not_count_as_content() {
        InAppClipboard.copy("")
        assertFalse(InAppClipboard.has(), "an empty clipboard must not report content")
    }

    // ---- bridges -----------------------------------------------------------------------------

    @Test
    fun an_onCopy_bridge_sees_every_copy() {
        val forwarded = mutableListOf<String>()
        InAppClipboard.onCopy = {
            forwarded += it
            true
        }

        InAppClipboard.copy("to the phone")
        assertEquals(listOf("to the phone"), forwarded)
    }

    @Test
    fun a_failing_bridge_reports_failure_but_still_buffers_locally() {
        // The remote side being unreachable must not break an in-app paste.
        InAppClipboard.onCopy = { false }

        assertFalse(InAppClipboard.copy("hello"))
        assertEquals("hello", InAppClipboard.read(), "local paste must still work")
    }

    @Test
    fun an_onRead_bridge_takes_priority_over_the_buffer() {
        InAppClipboard.copy("local")
        InAppClipboard.onRead = { "from the host" }
        assertEquals("from the host", InAppClipboard.read())
    }

    @Test
    fun an_onRead_bridge_returning_null_falls_back_to_the_buffer() {
        InAppClipboard.copy("local")
        InAppClipboard.onRead = { null }
        assertEquals("local", InAppClipboard.read())
    }

    @Test
    fun bufferedContent_ignores_the_read_bridge() {
        InAppClipboard.copy("local")
        InAppClipboard.onRead = { "remote" }
        // Useful for diagnostics: what is actually held here, not what the bridge would serve.
        assertEquals("local", InAppClipboard.bufferedContent)
    }
}
