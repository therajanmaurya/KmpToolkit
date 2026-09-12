/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
package com.mobilebytelabs.kmptoolkit.deeplink.testing

import com.mobilebytelabs.kmptoolkit.deeplink.DeepLink
import com.mobilebytelabs.kmptoolkit.deeplink.DeepLinkManager
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * An isolated [DeepLinkManager] for tests — shipped in the main artifact.
 *
 * ## Why not just use the real one
 * `DeepLinkHandler` is a process-wide `object`: a link delivered in one test is still the
 * `lastReceived` value in the next, and its 500 ms dedup window silently swallows a repeated URI
 * — so a suite that emits the same link twice sees one delivery and the failure looks like a bug
 * in the code under test. This fake holds its own state, has no dedup window, and records
 * everything.
 *
 * ```kotlin
 * val links = FakeDeepLinkManager()
 * NavCoordinator(links, scope)
 *
 * links.handle("myapp://product/42")
 * assertEquals(listOf("myapp://product/42"), links.handled)
 * ```
 *
 * Simulate a cold start, where the link arrives before anything is collecting:
 *
 * ```kotlin
 * val links = FakeDeepLinkManager(initial = DeepLink.parse("myapp://product/42"))
 * assertNotNull(links.lastReceived.value)
 * ```
 */
public class FakeDeepLinkManager(initial: DeepLink? = null) : DeepLinkManager {

    private val _incoming = MutableSharedFlow<DeepLink>(
        replay = 0,
        extraBufferCapacity = Channel.UNLIMITED,
    )
    override val incoming: SharedFlow<DeepLink> = _incoming.asSharedFlow()

    private val _lastReceived = MutableStateFlow(initial)
    override val lastReceived: StateFlow<DeepLink?> = _lastReceived.asStateFlow()

    /** Every URI passed to [handle], in order — including repeats. */
    public val handled: MutableList<String> = mutableListOf()

    /** How many times [clear] was called. */
    public var clearCalls: Int = 0
        private set

    override fun handle(uri: String) {
        handled += uri
        val link = DeepLink.parse(uri)
        _lastReceived.value = link
        _incoming.tryEmit(link)
    }

    override fun clear() {
        clearCalls++
        _lastReceived.value = null
    }

    /** Forget every recorded URI and reset the last link. */
    public fun reset() {
        handled.clear()
        clearCalls = 0
        _lastReceived.value = null
    }
}
