/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
package com.mobilebytelabs.kmptoolkit.bubble

import com.mobilebytelabs.kmptoolkit.bubble.testing.FakeBubble
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** The fake has to behave like a real bubble, including when the platform cannot notify. */
class FakeBubbleTest {

    @Test
    fun a_shown_bubble_is_recorded_and_marks_the_state() {
        val bubble = FakeBubble()
        bubble.show("Download finished", "report.pdf")

        assertEquals("Download finished", bubble.shown.single().title)
        assertTrue(bubble.isShowing)
    }

    @Test
    fun a_persistent_bubble_is_distinguishable_from_a_transient_one() {
        val bubble = FakeBubble()
        bubble.show("transient", "a")
        bubble.showPersistent("sticky", "b")

        assertEquals(listOf(false, true), bubble.shown.map { it.persistent })
    }

    @Test
    fun showScreen_records_the_route() {
        val bubble = FakeBubble()
        bubble.showScreen("Chat", route = "/chat/42")
        assertEquals("/chat/42", bubble.shown.single().route)
    }

    @Test
    fun an_unsupported_platform_records_nothing() {
        // Mirrors the real contract, so a test can prove the caller checked `capability` first
        // rather than assuming the notification landed.
        val tv = FakeBubble(capability = BubbleCapability.None, capabilityReason = "tvOS is badge-only")
        tv.show("nope", "nothing")
        tv.showPersistent("nope", "nothing")

        assertTrue(tv.shown.isEmpty())
        assertFalse(tv.isShowing)
    }

    @Test
    fun update_edits_the_last_bubble_rather_than_adding_one() {
        val bubble = FakeBubble()
        bubble.show("Downloading", "0%")
        bubble.update(message = "80%")

        assertEquals(1, bubble.shown.size)
        assertEquals("80%", bubble.shown.single().message)
        assertEquals("Downloading", bubble.shown.single().title)
    }

    @Test
    fun dismiss_is_counted_and_clears_showing() {
        val bubble = FakeBubble()
        bubble.show("a", "b")
        bubble.dismiss()

        assertEquals(1, bubble.dismissCalls)
        assertFalse(bubble.isShowing)
    }

    @Test
    fun reset_returns_it_to_a_clean_state() {
        val bubble = FakeBubble()
        bubble.show("a", "b")
        bubble.dismiss()
        bubble.reset()

        assertTrue(bubble.shown.isEmpty())
        assertEquals(0, bubble.dismissCalls)
        assertTrue(bubble.state.value is BubbleState.Hidden)
    }
}
