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

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * The [UpdateResult] → [UpdateOutcome] flattening, which every consumer used to write by hand.
 *
 * The subtle case — and the reason `started` exists — is that `Success` with an available update
 * means two different things depending on which engine call produced it.
 */
class UpdateOutcomeTest {

    private fun available(): UpdateResult.Success = UpdateResult.Success(
        UpdateInfo(isAvailable = true, currentVersion = AppVersion.UNKNOWN),
    )

    private fun upToDate(): UpdateResult.Success = UpdateResult.Success(
        UpdateInfo(isAvailable = false, currentVersion = AppVersion.UNKNOWN),
    )

    @Test
    fun success_without_an_available_update_is_up_to_date_either_way() {
        assertIs<UpdateOutcome.UpToDate>(upToDate().toOutcome())
        assertIs<UpdateOutcome.UpToDate>(upToDate().toOutcome(started = true))
    }

    @Test
    fun an_available_update_after_a_check_is_Available_not_Started() {
        // The distinction that matters: reporting "started" here would tell the UI the OS has
        // taken over when in fact nothing has been offered to the user yet.
        val outcome = assertIs<UpdateOutcome.Available>(available().toOutcome())
        assertEquals(true, outcome.info.isAvailable)
    }

    @Test
    fun an_available_update_after_a_start_is_UpdateStarted() {
        assertIs<UpdateOutcome.UpdateStarted>(available().toOutcome(started = true))
    }

    @Test
    fun cancelled_maps_straight_through() {
        assertIs<UpdateOutcome.Cancelled>(UpdateResult.Cancelled.toOutcome())
    }

    @Test
    fun not_supported_keeps_its_reason_for_the_developer() {
        val outcome = assertIs<UpdateOutcome.NotSupported>(
            UpdateResult.NotSupported("tvOS has no in-app update flow").toOutcome(),
        )
        assertEquals("tvOS has no in-app update flow", outcome.reason)
    }

    @Test
    fun an_error_keeps_its_message() {
        val outcome = assertIs<UpdateOutcome.Failed>(UpdateResult.Error("network down").toOutcome())
        assertEquals("network down", outcome.message)
    }
}
