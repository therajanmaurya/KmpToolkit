/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
package com.mobilebytelabs.kmptoolkit.bubble.testing

import com.mobilebytelabs.kmptoolkit.bubble.Bubble
import com.mobilebytelabs.kmptoolkit.bubble.BubbleAction
import com.mobilebytelabs.kmptoolkit.bubble.BubbleCapability
import com.mobilebytelabs.kmptoolkit.bubble.BubbleIcon
import com.mobilebytelabs.kmptoolkit.bubble.BubbleScreenConfig
import com.mobilebytelabs.kmptoolkit.bubble.BubbleState
import com.mobilebytelabs.kmptoolkit.bubble.BubbleStyle
import com.mobilebytelabs.kmptoolkit.bubble.BubbleTapAction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** One recorded notification. */
public data class ShownBubble(
    public val title: String,
    public val message: String,
    public val actions: List<BubbleAction>,
    public val style: BubbleStyle,
    public val persistent: Boolean,
    public val route: String? = null,
)

/**
 * In-memory [Bubble] for tests — shipped in the main artifact.
 *
 * The real implementations raise actual OS notifications: a tray balloon on Windows, a
 * `notify-send` on Linux, a wrist alert on watchOS. A test asserting "we told the user" must not
 * do that, and on a CI box most of those would fail for want of a session bus or a tray anyway.
 *
 * ```kotlin
 * val bubble = FakeBubble()
 * DownloadViewModel(bubble).onComplete("report.pdf")
 * assertEquals("Download finished", bubble.shown.single().title)
 * ```
 *
 * Simulate a platform that cannot notify, and assert the caller degrades rather than assuming:
 *
 * ```kotlin
 * val tv = FakeBubble(capability = BubbleCapability.None, capabilityReason = "tvOS is badge-only")
 * DownloadViewModel(tv).onComplete("report.pdf")
 * assertTrue(tv.shown.isEmpty())      // and the UI showed an in-app banner instead
 * ```
 */
public class FakeBubble(
    override val capability: BubbleCapability = BubbleCapability.Notification,
    override val capabilityReason: String = "FakeBubble — in-memory, raises no OS notification",
) : Bubble {

    private val _state = MutableStateFlow<BubbleState>(BubbleState.Hidden)
    override val state: StateFlow<BubbleState> = _state.asStateFlow()

    override val isShowing: Boolean get() = _state.value is BubbleState.Showing

    /** Every notification requested, in order. */
    public val shown: MutableList<ShownBubble> = mutableListOf()

    /** How many times [dismiss] was called. */
    public var dismissCalls: Int = 0
        private set

    override fun show(
        title: String,
        message: String,
        icon: BubbleIcon?,
        actions: List<BubbleAction>,
        style: BubbleStyle,
        onTap: BubbleTapAction,
        autoDismissMs: Long,
    ) {
        // A platform that cannot notify must not appear to: mirroring that here is what lets a
        // test prove the caller checked `capability` before relying on it.
        if (capability == BubbleCapability.None) return
        shown += ShownBubble(title, message, actions, style, persistent = false)
        _state.value = BubbleState.Showing
    }

    override fun showScreen(
        title: String,
        route: String,
        screenConfig: BubbleScreenConfig,
        icon: BubbleIcon?,
        style: BubbleStyle,
    ) {
        if (capability == BubbleCapability.None) return
        shown += ShownBubble(title, "", emptyList(), style, persistent = false, route = route)
        _state.value = BubbleState.Showing
    }

    override fun showPersistent(title: String, message: String, actions: List<BubbleAction>, style: BubbleStyle) {
        if (capability == BubbleCapability.None) return
        shown += ShownBubble(title, message, actions, style, persistent = true)
        _state.value = BubbleState.Showing
    }

    override fun update(title: String?, message: String?, actions: List<BubbleAction>?) {
        val last = shown.lastOrNull() ?: return
        shown[shown.lastIndex] = last.copy(
            title = title ?: last.title,
            message = message ?: last.message,
            actions = actions ?: last.actions,
        )
    }

    override fun dismiss() {
        dismissCalls++
        _state.value = BubbleState.Dismissed(byUser = false)
    }

    /** Forget every recorded notification. */
    public fun reset() {
        shown.clear()
        dismissCalls = 0
        _state.value = BubbleState.Hidden
    }
}
