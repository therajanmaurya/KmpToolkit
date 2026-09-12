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

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The capability descriptor has to agree with what this target's functions actually do. */
class ClipboardCapabilitiesTest {

    @Test
    fun every_target_can_write_and_read() {
        // The floor: three targets reach this through an app-scoped buffer rather than the OS,
        // but none of them refuses outright any more.
        val caps = platformClipboardCapabilities
        assertTrue(caps.write)
        assertTrue(caps.read)
    }

    @Test
    fun an_app_scoped_clipboard_round_trips() {
        // Deliberately scoped to the app-scoped targets (tvOS, watchOS, wasmWasi). A SYSTEM
        // clipboard's round-trip depends on the environment, not on this library: an iOS
        // simulator's UIPasteboard accepts a write and reads back null, and a headless CI JVM
        // may have no clipboard at all. Asserting it there tests the runner, not the code —
        // which is exactly how this test first failed.
        if (platformClipboardCapabilities.systemWide) return

        val marker = "cmp-clipboard round-trip check"
        assertTrue(copyToClipboard(marker))
        assertEquals(marker, getFromClipboard())
        assertTrue(hasClipboardText())
    }

    @Test
    fun a_system_clipboard_at_least_accepts_a_write_without_throwing() {
        if (!platformClipboardCapabilities.systemWide) return
        // The only claim that holds in every runner: writing is attempted and reports a verdict
        // rather than raising. Whether a sandboxed simulator then serves it back is not ours.
        copyToClipboard("cmp-clipboard write check")
    }

    @Test
    fun in_app_capabilities_are_not_system_wide() {
        assertTrue(ClipboardCapabilities.InApp.write)
        assertTrue(ClipboardCapabilities.InApp.read)
        // The distinction the whole descriptor exists for.
        assertEquals(false, ClipboardCapabilities.InApp.systemWide)
    }

    @Test
    fun browser_reads_are_flagged_as_permission_gated() {
        assertTrue(ClipboardCapabilities.SystemPermissioned.systemWide)
        assertTrue(ClipboardCapabilities.SystemPermissioned.readNeedsPermission)
        assertEquals(false, ClipboardCapabilities.System.readNeedsPermission)
    }
}
