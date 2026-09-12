/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
package com.mobilebytelabs.kmptoolkit.toast

import com.mobilebytelabs.kmptoolkit.toast.testing.FakeToastDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The dispatcher is the half of this module a ViewModel depends on, so these run entirely without
 * composition — which is the point of separating it from [ToastHost] in the first place.
 */
class ToastDispatcherTest {

    @Test
    fun a_dispatched_toast_is_recorded_with_its_defaults() = runTest {
        val toasts = FakeToastDispatcher()
        toasts.showToast("Saved")

        val shown = toasts.shown.single()
        assertEquals("Saved", shown.message)
        assertNull(shown.actionLabel)
        // Defaults are declared on ToastDispatcher; a caller omitting them must still get these.
        assertEquals(ToastDuration.SHORT, shown.duration)
        assertEquals(ToastPosition.BOTTOM, shown.position)
        assertEquals(ToastStyle.DEFAULT, shown.style)
    }

    @Test
    fun every_argument_is_carried_through() = runTest {
        val toasts = FakeToastDispatcher()
        toasts.showToast(
            message = "Deleted",
            actionLabel = "Undo",
            duration = ToastDuration.LONG,
            position = ToastPosition.TOP,
            style = ToastStyle.ERROR,
        )

        val shown = toasts.shown.single()
        assertEquals("Undo", shown.actionLabel)
        assertEquals(ToastDuration.LONG, shown.duration)
        assertEquals(ToastPosition.TOP, shown.position)
        assertEquals(ToastStyle.ERROR, shown.style)
    }

    @Test
    fun the_scripted_result_drives_an_undo_flow() = runTest {
        // The reason showToast suspends and returns: an Undo action is only honoured when the
        // user actually tapped it.
        val tapped = FakeToastDispatcher(result = ToastResult.ACTION_PERFORMED)
        assertEquals(ToastResult.ACTION_PERFORMED, tapped.showToast("Deleted", actionLabel = "Undo"))

        val ignored = FakeToastDispatcher(result = ToastResult.DISMISSED)
        assertEquals(ToastResult.DISMISSED, ignored.showToast("Deleted", actionLabel = "Undo"))
    }

    @Test
    fun the_fake_returns_immediately_rather_than_awaiting_a_host() = runTest {
        // ToastHostState suspends until the toast resolves, so a ViewModel test using the real
        // one would hang for the full duration waiting for a host no test renders.
        val toasts = FakeToastDispatcher()
        repeat(5) { toasts.showToast("burst $it") }
        assertEquals(5, toasts.shown.size)
    }

    @Test
    fun reset_clears_the_ledger() = runTest {
        val toasts = FakeToastDispatcher()
        toasts.showToast("one")
        toasts.reset()
        assertTrue(toasts.shown.isEmpty())
    }

    @Test
    fun the_real_state_satisfies_the_dispatcher_contract() {
        // Guards the binding the DI module relies on: ToastHostState IS a ToastDispatcher, so one
        // instance can serve both composition and injection.
        val state: ToastDispatcher = ToastHostState()
        assertTrue(state is ToastHostState)
    }
}
