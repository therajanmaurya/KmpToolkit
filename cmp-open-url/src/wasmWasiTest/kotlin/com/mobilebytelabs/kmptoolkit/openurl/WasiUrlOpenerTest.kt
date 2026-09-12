/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
package com.mobilebytelabs.kmptoolkit.openurl

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * wasmWasi passes URLs across the host boundary instead of dropping them.
 *
 * The regression these lock down: every function here used to return `false` / `NoHandler`
 * unconditionally, which is indistinguishable from a working implementation whose host ignored
 * the request.
 */
class WasiUrlOpenerTest {

    @BeforeTest
    fun setUp() {
        WasiUrlOpener.reset()
        WasiUrlOpener.echoToStdout = false
    }

    @AfterTest
    fun tearDown() = WasiUrlOpener.reset()

    @Test
    fun open_reaches_the_host_buffer() {
        assertTrue(UrlLauncherImpl().open("https://example.com"))
        assertEquals(listOf("https://example.com"), WasiUrlOpener.drain())
    }

    @Test
    fun a_registered_handler_intercepts_and_bypasses_the_buffer() {
        val seen = mutableListOf<Pair<String, AppHint>>()
        WasiUrlOpener.handler = { url, hint ->
            seen += url to hint
            true
        }

        UrlLauncherImpl().openInBrowser("https://example.com")
        assertEquals("https://example.com" to AppHint.BROWSER, seen.single())
        assertTrue(WasiUrlOpener.opened.isEmpty(), "the handler owns delivery")
    }

    @Test
    fun a_declining_handler_surfaces_as_no_handler() {
        WasiUrlOpener.handler = { _, _ -> false }
        assertIs<OpenUrlResult.NoHandler>(UrlLauncherImpl().openWith("tel:123", AppHint.PHONE))
    }

    @Test
    fun an_inapplicable_hint_is_an_error_not_a_silent_pass() {
        // EMAIL against a non-mailto URL cannot be transformed; that is a caller mistake and
        // must not reach the host looking like a successful open.
        val result = assertIs<OpenUrlResult.Error>(
            UrlLauncherImpl().openWith("https://example.com", AppHint.EMAIL),
        )
        assertTrue(result.message.contains("EMAIL"), result.message)
        assertTrue(WasiUrlOpener.opened.isEmpty())
    }

    @Test
    fun a_blank_url_is_refused() {
        assertFalse(UrlLauncherImpl().open("   ".trim()))
        assertFalse(UrlLauncherImpl().canOpen(""))
    }
}
