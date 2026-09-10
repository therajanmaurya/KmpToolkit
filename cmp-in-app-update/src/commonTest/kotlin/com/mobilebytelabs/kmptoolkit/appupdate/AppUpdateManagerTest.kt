/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
package com.mobilebytelabs.kmptoolkit.appupdate

import com.mobilebytelabs.kmptoolkit.appupdate.testing.FakeAppUpdateManager
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/** Contract for the injectable facade and its test double. */
class AppUpdateManagerTest {

    @Test
    fun the_fake_counts_each_operation_separately() = runTest {
        val updates = FakeAppUpdateManager()
        updates.check()
        updates.checkAndStart()
        updates.start()
        updates.openStore()

        assertEquals(1, updates.checkCalls)
        assertEquals(1, updates.checkAndStartCalls)
        assertEquals(1, updates.startCalls)
        assertEquals(1, updates.openStoreCalls)
    }

    @Test
    fun the_requested_update_type_is_recorded() = runTest {
        val updates = FakeAppUpdateManager()
        updates.checkAndStart(UpdateType.FLEXIBLE)
        updates.start(UpdateType.IMMEDIATE)
        assertEquals(listOf(UpdateType.FLEXIBLE, UpdateType.IMMEDIATE), updates.requestedTypes)
    }

    @Test
    fun a_scripted_outcome_is_returned() = runTest {
        val updates = FakeAppUpdateManager(scripted = UpdateOutcome.UpdateStarted)
        assertIs<UpdateOutcome.UpdateStarted>(updates.checkAndStart())
    }

    @Test
    fun an_unsupported_platform_can_be_simulated_for_the_store_fallback() = runTest {
        // The tvOS / watchOS / desktop shape: no in-app flow, but a store page to open.
        val updates = FakeAppUpdateManager(
            supported = false,
            scripted = UpdateOutcome.NotSupported("no in-app update flow here"),
        )
        assertFalse(updates.isSupported())
        assertIs<UpdateOutcome.NotSupported>(updates.check())
        assertTrue(updates.openStore())
    }

    @Test
    fun reset_zeroes_every_counter() = runTest {
        val updates = FakeAppUpdateManager()
        updates.check()
        updates.openStore()
        updates.reset()

        assertEquals(0, updates.checkCalls)
        assertEquals(0, updates.openStoreCalls)
        assertTrue(updates.requestedTypes.isEmpty())
    }

    @Test
    fun the_real_impl_agrees_with_the_engine_about_support() {
        // Guards the delegation: if these disagree, the manager is lying about the platform.
        assertEquals(AppUpdate.isSupported(), AppUpdateManagerImpl().isSupported())
    }

    @Test
    fun an_unsupported_platform_reports_NotSupported_rather_than_a_bare_failure() = runTest {
        val impl = AppUpdateManagerImpl()
        if (impl.isSupported()) return@runTest
        // tvOS, watchOS, wasmWasi, Linux, Windows land here — the reason must survive the
        // flattening so a developer can see WHY.
        val outcome = assertIs<UpdateOutcome.NotSupported>(impl.check())
        assertTrue(outcome.reason.isNotBlank(), "NotSupported must carry a usable reason")
    }
}
