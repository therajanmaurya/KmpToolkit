/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
package com.mobilebytelabs.kmptoolkit.intentlauncher.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import com.mobilebytelabs.kmptoolkit.intentlauncher.IntentCapabilities
import com.mobilebytelabs.kmptoolkit.intentlauncher.IntentManager
import com.mobilebytelabs.kmptoolkit.intentlauncher.IntentManagerImpl
import com.mobilebytelabs.kmptoolkit.intentlauncher.IntentOperation

/**
 * The [IntentManager] for this composition subtree.
 *
 * ## Works with no provider — but read the Android note
 * Reading this without a provider does not throw; it falls back to a plain [IntentManagerImpl].
 * On every target except Android that is fully functional. **On Android the fallback has no
 * `IntentLauncher`**, because one is Activity-scoped — so `openAppSettings` and `createDocument`
 * work while the pickers report unsupported, and [rememberIntentCapabilities] says so.
 *
 * To get pickers on Android, provide a manager built from the Activity-scoped launcher:
 *
 * ```kotlin
 * // inside setContent { } of a ComponentActivity
 * val manager = rememberIntentManagerFromLauncher()
 * ProvideIntentManager(manager) { App() }
 * ```
 *
 * ## Using Koin?
 * Prefer injecting [IntentManager] (see `intentLauncherModule`) and handing it to
 * [ProvideIntentManager], so one instance serves composables, ViewModels and repositories alike.
 */
public val LocalIntentManager: ProvidableCompositionLocal<IntentManager> =
    staticCompositionLocalOf { IntentManagerImpl() }

/**
 * Provide [manager] to [content] via [LocalIntentManager].
 *
 * ```kotlin
 * val intents: IntentManager = koinInject()
 * ProvideIntentManager(intents) { App() }
 * ```
 */
@Composable
public fun ProvideIntentManager(manager: IntentManager, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalIntentManager provides manager, content = content)
}

/**
 * The [IntentManager] in scope — whatever [LocalIntentManager] resolves to.
 *
 * ```kotlin
 * val intents = rememberIntentManager()
 * val scope = rememberCoroutineScope()
 * Button(onClick = { scope.launch { intents.pickImage() } }) { Text("Choose photo") }
 * ```
 *
 * Named `remember…` to match the toolkit's other Compose accessors; no `remember` call is needed
 * because a CompositionLocal read is already stable across recompositions.
 */
@Composable
public fun rememberIntentManager(): IntentManager = LocalIntentManager.current

/**
 * An [IntentManager] wired to this composition's platform launcher — the one to provide on
 * Android, where the launcher must come from the hosting Activity.
 *
 * On other targets this is equivalent to `IntentManagerImpl()`; using it everywhere keeps the
 * call site uniform.
 */
@Composable
public fun rememberIntentManagerFromLauncher(): IntentManager {
    val launcher = rememberIntentLauncher()
    return remember(launcher) { IntentManagerImpl(launcher) }
}

/**
 * What this target can do — read it to decide whether to render an affordance at all, rather than
 * offering an action that fails once tapped.
 *
 * ```kotlin
 * if (rememberIntentCapabilities().pickContact) {
 *     ListItem(headlineContent = { Text("Choose from contacts") }, modifier = Modifier.clickable(::pick))
 * }
 * ```
 */
@Composable
public fun rememberIntentCapabilities(): IntentCapabilities {
    val manager = rememberIntentManager()
    return remember(manager) { manager.capabilities }
}

/**
 * Whether [operation] can run here — the single-operation form of [rememberIntentCapabilities].
 */
@Composable
public fun rememberSupportsIntent(operation: IntentOperation): Boolean {
    val capabilities = rememberIntentCapabilities()
    return remember(capabilities, operation) { operation in capabilities }
}
