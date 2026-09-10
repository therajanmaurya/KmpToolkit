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

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Every target must describe its own bubble capability truthfully — the reason string is what a
 * developer reads when a notification does not appear, so a vague or false one costs real time.
 *
 * Four platforms used to claim `None` with "X has no notification capability". Three of those were
 * wrong: the JVM has `SystemTray`, Linux has `notify-send`, watchOS has `UNUserNotificationCenter`.
 * tvOS genuinely has none — its notification content type exposes no title or body — and now says
 * exactly that instead.
 */
class BubbleCapabilityContractTest {

    @Test
    fun a_bubble_always_reports_a_capability_and_a_reason() {
        val bubble = createBubble()
        assertTrue(
            bubble.capabilityReason.isNotBlank(),
            "capabilityReason is the developer's only clue when nothing appears",
        )
    }

    @Test
    fun a_reason_that_merely_repeats_None_is_not_good_enough() {
        val bubble = createBubble()
        if (bubble.capability != BubbleCapability.None) return

        // Guards the exact regression: "X has no notification capability" says nothing a caller
        // can act on. A real reason names the API that is missing, or what to set.
        val reason = bubble.capabilityReason.lowercase()
        assertFalse(
            reason.endsWith("has no notification capability"),
            "an unsupported platform must explain WHY, not restate that it is unsupported: " +
                bubble.capabilityReason,
        )
    }

    @Test
    fun a_fresh_bubble_is_not_showing() {
        val bubble = createBubble()
        assertFalse(bubble.isShowing)
        assertTrue(bubble.state.value is BubbleState.Hidden)
    }

    @Test
    fun dismiss_moves_a_bubble_out_of_showing() {
        val bubble = createBubble()
        bubble.dismiss()
        assertFalse(bubble.isShowing)
    }

    @Test
    fun capability_and_reason_agree_about_support() {
        val bubble = createBubble()
        val supported = bubble.capability != BubbleCapability.None
        // A supported platform naming nothing, or an unsupported one describing a working
        // mechanism, means the two drifted apart.
        assertTrue(bubble.capabilityReason.length > 3)
        if (supported) {
            assertFalse(
                bubble.capabilityReason.contains("no notification", ignoreCase = true),
                "a supported capability must not describe itself as absent",
            )
        }
    }
}
