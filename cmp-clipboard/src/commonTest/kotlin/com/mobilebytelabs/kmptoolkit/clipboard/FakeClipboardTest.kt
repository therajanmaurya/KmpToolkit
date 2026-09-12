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

import com.mobilebytelabs.kmptoolkit.clipboard.testing.FakeClipboard
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The narrow [Clipboard] surface and its fake.
 *
 * These exist because the real clipboard is machine-owned shared state: a CI JVM may have none, an
 * iOS simulator accepts a write and reads back null, and a browser gates reads behind a prompt.
 */
class FakeClipboardTest {

    @Test
    fun a_copy_round_trips_and_is_recorded() {
        val clipboard = FakeClipboard()
        assertTrue(clipboard.copy("ABC123"))

        assertEquals("ABC123", clipboard.paste())
        assertEquals(listOf("ABC123"), clipboard.copied)
        assertTrue(clipboard.hasText())
    }

    @Test
    fun an_initial_value_models_a_clipboard_that_already_had_content() {
        val clipboard = FakeClipboard(initial = "pasted from elsewhere")
        assertEquals("pasted from elsewhere", clipboard.paste())
        assertTrue(clipboard.copied.isEmpty(), "an initial value was not copied by this test")
    }

    @Test
    fun a_read_only_clipboard_refuses_writes_and_says_so() {
        // Models a permission denial or a platform that cannot write — the caller must notice
        // rather than assume the copy landed.
        val clipboard = FakeClipboard(writable = false)

        assertFalse(clipboard.copy("ABC123"))
        assertNull(clipboard.paste())
        assertEquals(listOf("ABC123"), clipboard.refused)
        assertTrue(clipboard.copied.isEmpty())
    }

    @Test
    fun clear_empties_it() {
        val clipboard = FakeClipboard(initial = "x")
        clipboard.clear()
        assertNull(clipboard.paste())
        assertFalse(clipboard.hasText())
    }

    @Test
    fun an_empty_string_does_not_count_as_content() {
        val clipboard = FakeClipboard()
        clipboard.copy("")
        assertFalse(clipboard.hasText())
    }

    @Test
    fun the_async_forms_share_state_with_the_sync_ones() = runTest {
        // A test should not have to care which form the code under test happened to call.
        val clipboard = FakeClipboard()
        clipboard.copyAsync("async value")
        assertEquals("async value", clipboard.paste())
        assertEquals("async value", clipboard.pasteAsync())
    }

    @Test
    fun reads_are_counted() {
        val clipboard = FakeClipboard(initial = "x")
        clipboard.paste()
        clipboard.paste()
        assertEquals(2, clipboard.pasteCalls)
    }

    @Test
    fun capabilities_can_be_pinned_to_model_an_app_scoped_platform() {
        // tvOS / watchOS / wasmWasi: copy works, but nothing outside the app sees it — so a UI
        // should say "Copied", not "Copied to clipboard".
        val appScoped = FakeClipboard(capabilities = ClipboardCapabilities.InApp)
        assertFalse(appScoped.capabilities.systemWide)
        assertTrue(appScoped.copy("still works"))
    }

    @Test
    fun reset_clears_contents_and_every_ledger() {
        val clipboard = FakeClipboard()
        clipboard.copy("a")
        clipboard.paste()
        clipboard.reset()

        assertNull(clipboard.paste())
        assertTrue(clipboard.copied.isEmpty())
        assertEquals(1, clipboard.pasteCalls, "the post-reset read is the only one counted")
    }

    @Test
    fun the_real_manager_satisfies_the_narrow_surface() {
        // Guards the DI binding: ClipboardManager IS a Clipboard, so both resolve to one object.
        val manager: Clipboard = ClipboardManager()
        assertEquals(platformClipboardCapabilities, manager.capabilities)
    }
}
