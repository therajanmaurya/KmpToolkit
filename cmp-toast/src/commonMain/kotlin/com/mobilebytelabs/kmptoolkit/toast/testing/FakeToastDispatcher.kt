/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
package com.mobilebytelabs.kmptoolkit.toast.testing

import com.mobilebytelabs.kmptoolkit.toast.ToastDispatcher
import com.mobilebytelabs.kmptoolkit.toast.ToastDuration
import com.mobilebytelabs.kmptoolkit.toast.ToastPosition
import com.mobilebytelabs.kmptoolkit.toast.ToastResult
import com.mobilebytelabs.kmptoolkit.toast.ToastStyle

/** One recorded toast request. */
public data class ShownToast(
    public val message: String,
    public val actionLabel: String?,
    public val duration: ToastDuration,
    public val position: ToastPosition,
    public val style: ToastStyle,
)

/**
 * In-memory [ToastDispatcher] for tests — shipped in the main artifact.
 *
 * The real `ToastHostState` suspends until a toast is dismissed, actioned or times out, so a
 * ViewModel test using it would hang for the toast's full duration or deadlock waiting for a host
 * that no test renders. This returns immediately.
 *
 * ```kotlin
 * val toasts = FakeToastDispatcher()
 * CheckoutViewModel(toasts).onPaid()
 * assertEquals("Payment received", toasts.shown.single().message)
 * ```
 *
 * Drive an Undo flow by scripting the user's tap:
 *
 * ```kotlin
 * val toasts = FakeToastDispatcher(result = ToastResult.ACTION_PERFORMED)
 * viewModel.deleteItem(id)
 * assertTrue(repository.contains(id))   // the undo ran
 * ```
 */
public class FakeToastDispatcher(
    /** Returned by every [showToast] call. Defaults to a plain timeout dismissal. */
    public var result: ToastResult = ToastResult.DISMISSED,
) : ToastDispatcher {

    /** Every toast requested, in order. */
    public val shown: MutableList<ShownToast> = mutableListOf()

    override suspend fun showToast(
        message: String,
        actionLabel: String?,
        duration: ToastDuration,
        position: ToastPosition,
        style: ToastStyle,
    ): ToastResult {
        shown += ShownToast(message, actionLabel, duration, position, style)
        return result
    }

    /** Forget every recorded toast. */
    public fun reset() {
        shown.clear()
    }
}
