/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
package io.github.mobilebytelabs.kmptoolkit.firebase.analytics

import io.github.mobilebytelabs.kmptoolkit.firebase.FirebaseConfig
import io.github.mobilebytelabs.kmptoolkit.firebase.FirebaseRuntime
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertSame

/**
 * Tier-agnostic contract for [provideAnalyticsHelper], asserted on EVERY target —
 * including the browser (`jsBrowserTest` / `wasmJsBrowserTest`), where the firebase
 * tier is live.
 *
 * The contract is stated in `FirebaseKit.initialize`'s KDoc:
 *
 * > "When this platform has no options (and is not on the MP tier), analytics
 * > degrades to NoOp with a WARN — **it never throws** (analytics must not break
 * > the app)."
 *
 * `nonFirebaseMain` has always honoured this (`?: NoOpAnalyticsHelper`).
 * `firebaseMain` did not: it called `FirebaseAnalyticsHelper(Firebase.analytics)`
 * unconditionally, so a web/wasmJs app that never supplied `FirebaseConfig.web`
 * threw `No Firebase App '[DEFAULT]' has been created` from the analytics factory —
 * crashing the app on the very path the library logs as "analytics will NoOp".
 *
 * These tests pin the contract so the crash cannot come back on any tier.
 */
class AnalyticsProviderContractTest {

    @BeforeTest
    fun clearCache() {
        FirebaseRuntime.analyticsHelper = null
    }

    @AfterTest
    fun reset() {
        FirebaseRuntime.config = null
        FirebaseRuntime.analyticsHelper = null
    }

    /**
     * No config at all — the factory must return a usable helper rather than throw.
     * On the firebase tier this is the "Firebase was never initialized" path.
     */
    @Test
    fun provider_does_not_throw_when_no_config() {
        FirebaseRuntime.config = null
        assertNotNull(provideAnalyticsHelper())
    }

    /**
     * Config present but carrying no options for THIS platform and no MpConfig —
     * the documented degrade-to-NoOp case.
     */
    @Test
    fun provider_does_not_throw_when_config_has_no_options_for_platform() {
        FirebaseRuntime.config = FirebaseConfig()
        assertNotNull(provideAnalyticsHelper())
    }

    /**
     * Degradation must be silent-but-safe, never a half-built helper: with nothing
     * configured on any tier the result is the shared [NoOpAnalyticsHelper].
     */
    @Test
    fun unconfigured_platform_degrades_to_noop() {
        FirebaseRuntime.config = FirebaseConfig()
        assertSame(NoOpAnalyticsHelper, provideAnalyticsHelper())
    }

    /**
     * Logging an event through the degraded helper must also not throw — the app
     * keeps running even though nothing is captured.
     */
    @Test
    fun degraded_helper_swallows_events_without_throwing() {
        FirebaseRuntime.config = FirebaseConfig()
        val helper = provideAnalyticsHelper()
        helper.logEvent("contract_probe", mapOf("k" to "v"))
        assertNotNull(helper)
    }
}
