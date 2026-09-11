/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
package io.github.mobilebytelabs.kmptoolkit.firebase.testing

import io.github.mobilebytelabs.kmptoolkit.firebase.analytics.AnalyticsEvent
import io.github.mobilebytelabs.kmptoolkit.firebase.analytics.AnalyticsHelper

/**
 * Recording [AnalyticsHelper] for tests — shipped in the main artifact, like `FakeNetworkMonitor`.
 *
 * ## Why `NoOpAnalyticsHelper` is not enough
 * The library already ships a no-op, and it is the right production fallback when Firebase is
 * absent — but it *discards* events, so a test can only assert that logging did not crash. What a
 * test actually wants to know is **which** event was sent, with which parameters. Getting that
 * today means hand-rolling a recorder in every consumer.
 *
 * ```kotlin
 * val analytics = FakeAnalyticsHelper()
 * CheckoutViewModel(analytics).onPurchase(amount = "12.00")
 *
 * assertEquals("purchase", analytics.events.single().type)
 * assertEquals("12.00", analytics.paramsOf("purchase")["amount"])
 * ```
 *
 * The convenience overloads on [AnalyticsHelper] are interface defaults that funnel into
 * [logEvent], so `logScreenView` and `logButtonClick` are recorded too — as the events they build,
 * which is exactly what reaches Firebase in production.
 *
 * ```kotlin
 * analytics.logScreenView("checkout")
 * assertTrue(analytics.loggedType("screen_view"))
 * ```
 */
public class FakeAnalyticsHelper : AnalyticsHelper {

    /** Every event, in the order it was logged. */
    public val events: MutableList<AnalyticsEvent> = mutableListOf()

    override fun logEvent(event: AnalyticsEvent) {
        events += event
    }

    /** Whether any event of [type] was logged. */
    public fun loggedType(type: String): Boolean = events.any { it.type == type }

    /** Every event of [type], oldest first. */
    public fun eventsOf(type: String): List<AnalyticsEvent> = events.filter { it.type == type }

    /**
     * Parameters of the FIRST event of [type], flattened to a map.
     *
     * Returns an empty map when no such event was logged, so an assertion on a missing key fails
     * on the key rather than on a null dereference.
     */
    public fun paramsOf(type: String): Map<String, String> =
        eventsOf(type).firstOrNull()?.extras?.associate { it.key to it.value }.orEmpty()

    /** Forget every recorded event. */
    public fun reset() {
        events.clear()
    }
}
