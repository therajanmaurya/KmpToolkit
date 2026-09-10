/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
package com.mobilebytelabs.kmptoolkit.appintents.testing

import com.mobilebytelabs.kmptoolkit.appintents.AppIntentResult
import com.mobilebytelabs.kmptoolkit.appintents.AppIntentsCapabilities
import com.mobilebytelabs.kmptoolkit.appintents.AppIntentsConfig
import com.mobilebytelabs.kmptoolkit.appintents.AppIntentsManager
import com.mobilebytelabs.kmptoolkit.appintents.performForTesting

/**
 * In-memory [AppIntentsManager] for tests — shipped in the main artifact, like `FakeShareManager`,
 * so consumers can assert on registration without touching the OS.
 *
 * ```kotlin
 * val intents = FakeAppIntentsManager()
 * AppStartup(intents).onCreate()
 * assertEquals(listOf("add_task"), intents.registered.single().intents.map { it.id })
 * ```
 *
 * Constrain the reach to assert capability-dependent behaviour without running on that platform:
 *
 * ```kotlin
 * val tv = FakeAppIntentsManager(capabilities = AppIntentsCapabilities.InProcessOnly)
 * assertFalse(tv.reachesOs())     // so no voice onboarding is shown
 * ```
 *
 * Unlike the real implementation this does NOT touch the process-wide `AppIntentsRuntime`, so
 * tests cannot leak registrations into one another.
 */
public class FakeAppIntentsManager(
    override val capabilities: AppIntentsCapabilities = AppIntentsCapabilities.OsIntegrated,
) : AppIntentsManager {

    /** Every [register] call, in order. */
    public val registered: MutableList<AppIntentsConfig> = mutableListOf()

    override fun register(config: AppIntentsConfig) {
        registered += config
    }

    /** The most recent registration, or `null` if [register] has not been called. */
    override fun current(): AppIntentsConfig? = registered.lastOrNull()

    /** Perform against the LAST registration only — no process-wide state is consulted. */
    override suspend fun invoke(id: String, params: Map<String, Any>): AppIntentResult? =
        current()?.intents?.firstOrNull { it.id == id }?.let { def -> def.performForTesting(params) }

    /** Forget every recorded registration. */
    public fun reset() {
        registered.clear()
    }
}
