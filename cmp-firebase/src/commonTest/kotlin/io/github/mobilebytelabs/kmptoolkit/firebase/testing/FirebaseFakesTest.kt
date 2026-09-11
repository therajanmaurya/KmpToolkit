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
import io.github.mobilebytelabs.kmptoolkit.firebase.analytics.Param
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The recording fakes exist so a test can assert WHICH analytics event fired and HOW a failure was
 * reported — neither of which the shipped no-op implementations can tell you.
 */
class FirebaseFakesTest {

    // ---- analytics ---------------------------------------------------------------------------

    @Test
    fun an_event_is_recorded_with_its_parameters() {
        val analytics = FakeAnalyticsHelper()
        analytics.logEvent(AnalyticsEvent("purchase", listOf(Param("amount", "12.00"))))

        assertEquals("purchase", analytics.events.single().type)
        assertEquals("12.00", analytics.paramsOf("purchase")["amount"])
    }

    @Test
    fun the_vararg_and_map_overloads_land_as_the_same_event() {
        val analytics = FakeAnalyticsHelper()
        analytics.logEvent("signup", "method" to "email")
        analytics.logEvent("signup", mapOf("method" to "google"))

        // Both are interface defaults funnelling into logEvent — what reaches Firebase in
        // production is the built event, so that is what the fake must record.
        assertEquals(2, analytics.eventsOf("signup").size)
        assertEquals("email", analytics.paramsOf("signup")["method"], "paramsOf reads the FIRST")
    }

    @Test
    fun screen_views_and_button_clicks_are_recorded_as_the_events_they_build() {
        val analytics = FakeAnalyticsHelper()
        analytics.logScreenView("checkout")
        analytics.logButtonClick("pay", screenName = "checkout")

        assertTrue(analytics.loggedType("screen_view"))
        assertEquals(2, analytics.events.size)
    }

    @Test
    fun paramsOf_is_empty_for_an_event_that_never_fired() {
        // Empty rather than null, so an assertion fails on the missing KEY rather than on a
        // null dereference that hides which expectation actually broke.
        assertTrue(FakeAnalyticsHelper().paramsOf("never_logged").isEmpty())
    }

    @Test
    fun analytics_reset_clears_the_ledger() {
        val analytics = FakeAnalyticsHelper()
        analytics.logEvent("a")
        analytics.reset()
        assertTrue(analytics.events.isEmpty())
    }

    // ---- crash reporting ----------------------------------------------------------------------

    @Test
    fun an_exception_is_recorded_with_its_fatality_and_extras() {
        val crashes = FakeCrashReporter()
        crashes.recordException(
            IllegalStateException("sync failed"),
            fatal = false,
            extraKeys = mapOf("stage" to "sync"),
        )

        val recorded = crashes.recorded.single()
        // Fatality is the distinction most worth asserting: a retryable failure reported as fatal
        // pollutes crash-free-users metrics.
        assertFalse(recorded.fatal)
        assertEquals("sync", recorded.extraKeys["stage"])
    }

    @Test
    fun recordException_defaults_to_non_fatal() {
        val crashes = FakeCrashReporter()
        crashes.recordException(RuntimeException("boom"))
        assertFalse(crashes.recorded.single().fatal)
    }

    @Test
    fun breadcrumbs_keep_their_order() {
        val crashes = FakeCrashReporter()
        crashes.log("opened checkout")
        crashes.log("tapped pay")
        assertEquals(listOf("opened checkout", "tapped pay"), crashes.breadcrumbs)
    }

    @Test
    fun a_custom_key_is_sticky_and_last_write_wins() {
        val crashes = FakeCrashReporter()
        crashes.setCustomKey("tier", "free")
        crashes.setCustomKey("tier", "paid")
        assertEquals("paid", crashes.customKeys["tier"])
    }

    @Test
    fun user_id_and_install_are_observable() {
        val crashes = FakeCrashReporter()
        assertNull(crashes.userId)
        assertFalse(crashes.installed)

        crashes.setUserId("obfuscated-123")
        crashes.install()

        assertEquals("obfuscated-123", crashes.userId)
        assertTrue(crashes.installed)
    }

    @Test
    fun a_test_can_assert_no_pii_was_attached() {
        // The documented use: CrashReporter.setUserId's KDoc says NEVER pass PII, and this is how
        // a consumer proves it.
        val crashes = FakeCrashReporter()
        crashes.setCustomKey("tier", "paid")
        crashes.setUserId("obfuscated-123")

        assertFalse(crashes.customKeys.values.any { it.contains("@") })
        assertFalse(crashes.userId!!.contains("@"))
    }

    @Test
    fun crash_reset_clears_everything() {
        val crashes = FakeCrashReporter()
        crashes.recordException(RuntimeException("x"))
        crashes.log("b")
        crashes.setCustomKey("k", "v")
        crashes.setUserId("u")
        crashes.install()
        crashes.reset()

        assertTrue(crashes.recorded.isEmpty())
        assertTrue(crashes.breadcrumbs.isEmpty())
        assertTrue(crashes.customKeys.isEmpty())
        assertNull(crashes.userId)
        assertFalse(crashes.installed)
    }
}
