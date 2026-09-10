/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
package com.mobilebytelabs.kmptoolkit.appintents.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import com.mobilebytelabs.kmptoolkit.appintents.AppIntentsCapabilities
import com.mobilebytelabs.kmptoolkit.appintents.AppIntentsManager
import com.mobilebytelabs.kmptoolkit.appintents.AppIntentsManagerImpl

/**
 * The [AppIntentsManager] for this composition subtree.
 *
 * ## Works with no provider
 * Reading this without a provider does not throw — it falls back to a real
 * [AppIntentsManagerImpl]. Registration is process-wide and needs no configuration, so failing the
 * common case would buy nothing. Provide your own to substitute one: a fake in tests, or a
 * decorator that filters intents by entitlement.
 *
 * ```kotlin
 * val intents: AppIntentsManager = koinInject()
 * ProvideAppIntentsManager(intents) { App() }
 * ```
 */
public val LocalAppIntentsManager: ProvidableCompositionLocal<AppIntentsManager> =
    staticCompositionLocalOf { AppIntentsManagerImpl() }

/** Provide [manager] to [content] via [LocalAppIntentsManager]. */
@Composable
public fun ProvideAppIntentsManager(manager: AppIntentsManager, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalAppIntentsManager provides manager, content = content)
}

/**
 * The [AppIntentsManager] in scope.
 *
 * Named `remember…` to match the toolkit's other Compose accessors; no `remember` call is needed
 * because a CompositionLocal read is already stable across recompositions.
 */
@Composable
public fun rememberAppIntentsManager(): AppIntentsManager = LocalAppIntentsManager.current

/**
 * How far registration reaches here — read it before promising a voice affordance.
 *
 * ```kotlin
 * if (rememberAppIntentsCapabilities().osIntegration) {
 *     VoiceOnboardingCard()          // "Try saying: add a task"
 * }
 * ```
 *
 * Showing that card where reach is only `in-process` teaches the user a phrase that will never
 * work — which is exactly the failure the capability model exists to prevent.
 */
@Composable
public fun rememberAppIntentsCapabilities(): AppIntentsCapabilities {
    val manager = rememberAppIntentsManager()
    return remember(manager) { manager.capabilities }
}

/** Whether registered intents reach the OS here — the single-flag form of the above. */
@Composable
public fun rememberAppIntentsReachOs(): Boolean = rememberAppIntentsCapabilities().osIntegration
