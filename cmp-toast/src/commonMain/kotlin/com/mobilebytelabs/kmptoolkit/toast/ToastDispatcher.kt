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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Raise a toast from anywhere — the injectable, Compose-free half of this module.
 *
 * ## Why this is a dispatcher and not a "manager"
 * A toast has two halves that belong in different places. **Rendering** is Compose's job and stays
 * with [ToastHost]; there is nothing to abstract there. **Dispatch** — "something happened, tell
 * the user" — is a ViewModel or repository concern, and those have no business importing Compose.
 *
 * [ToastHostState] already had a Compose-free API; what it lacked was a type a caller could depend
 * on and substitute. This interface is that type, and `ToastHostState` implements it, so nothing
 * about the existing wiring changes.
 *
 * ```kotlin
 * class CheckoutViewModel(private val toasts: ToastDispatcher) : ViewModel() {
 *     fun onPaid() = viewModelScope.launch {
 *         toasts.showToast("Payment received", style = ToastStyle.SUCCESS)
 *     }
 * }
 * ```
 *
 * In tests, swap in `FakeToastDispatcher` and assert the message — no composition required.
 */
public interface ToastDispatcher {

    /**
     * Show [message] and suspend until it is dismissed, actioned or times out.
     *
     * Suspending is the point: the returned [ToastResult] tells you whether the user tapped the
     * action, which is what an "Undo" flow needs to decide whether to actually undo.
     */
    public suspend fun showToast(
        message: String,
        actionLabel: String? = null,
        duration: ToastDuration = ToastDuration.SHORT,
        position: ToastPosition = ToastPosition.BOTTOM,
        style: ToastStyle = ToastStyle.DEFAULT,
    ): ToastResult
}

/**
 * The [ToastDispatcher] for this composition subtree.
 *
 * Reading it without a provider throws, unlike the other toolkit CompositionLocals — deliberately.
 * A share sheet or a URL launcher works with no setup, but a toast needs a [ToastHost] somewhere in
 * the tree to render it. Falling back to a detached dispatcher would accept messages and display
 * nothing, which is indistinguishable from a broken toast.
 *
 * ```kotlin
 * val toastState = rememberToastHostState()
 * ProvideToastDispatcher(toastState) {
 *     ToastHost(state = toastState)
 *     App()
 * }
 * ```
 */
public val LocalToastDispatcher: ProvidableCompositionLocal<ToastDispatcher> =
    staticCompositionLocalOf {
        error(
            "No ToastDispatcher provided. Wrap your content in ProvideToastDispatcher(state) and " +
                "render a ToastHost(state = state) — without a host there is nothing to display " +
                "the toast, so a silent default would hide the mistake.",
        )
    }

/** Provide [dispatcher] to [content] via [LocalToastDispatcher]. */
@Composable
public fun ProvideToastDispatcher(dispatcher: ToastDispatcher, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalToastDispatcher provides dispatcher, content = content)
}

/**
 * The [ToastDispatcher] in scope.
 *
 * Named `remember…` to match the toolkit's other Compose accessors; no `remember` call is needed
 * because a CompositionLocal read is already stable across recompositions.
 */
@Composable
public fun rememberToastDispatcher(): ToastDispatcher = LocalToastDispatcher.current
