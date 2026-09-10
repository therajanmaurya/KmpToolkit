/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
package com.mobilebytelabs.kmptoolkit.share.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import com.mobilebytelabs.kmptoolkit.share.ShareCapabilities
import com.mobilebytelabs.kmptoolkit.share.ShareManager
import com.mobilebytelabs.kmptoolkit.share.ShareManagerImpl

/**
 * The [ShareManager] for this composition subtree.
 *
 * ## Works with no provider
 * Unlike `LocalNetworkMonitor`, reading this without a provider does NOT throw — it falls back to
 * a real [ShareManagerImpl]. Sharing is stateless and zero-config on every target, so there is
 * nothing for an app to set up and no reason to make the common case fail. Provide your own only
 * when you want to substitute one: a fake in tests, or a decorator that adds analytics or a
 * consent check.
 *
 * ```kotlin
 * // Reading — no setup required
 * val share = LocalShareManager.current
 *
 * // Overriding — tests, or an app-specific decorator
 * ProvideShareManager(FakeShareManager()) { MyScreen() }
 * ```
 *
 * ## Using Koin?
 * Prefer injecting [ShareManager] (see `shareModule`) and handing it to [ProvideShareManager], so
 * one instance serves composables, ViewModels and repositories alike.
 */
public val LocalShareManager: ProvidableCompositionLocal<ShareManager> =
    staticCompositionLocalOf { ShareManagerImpl() }

/**
 * Provide [manager] to [content] via [LocalShareManager].
 *
 * ```kotlin
 * val share: ShareManager = koinInject()
 * ProvideShareManager(share) { App() }
 * ```
 */
@Composable
public fun ProvideShareManager(manager: ShareManager, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalShareManager provides manager, content = content)
}

/**
 * The [ShareManager] in scope — whatever [LocalShareManager] resolves to, which is the provided
 * instance when there is one and a lazily-created [ShareManagerImpl] otherwise.
 *
 * Named `remember…` to match the toolkit's other Compose accessors; no `remember` call is needed
 * because a CompositionLocal read is already stable across recompositions.
 *
 * ```kotlin
 * val share = rememberShareManager()
 * val scope = rememberCoroutineScope()
 * Button(onClick = { scope.launch { share.shareUrl(article.url) } }) { Text("Share") }
 * ```
 */
@Composable
public fun rememberShareManager(): ShareManager = LocalShareManager.current

/**
 * What this target can share — read it to decide whether to render a share affordance at all,
 * rather than showing a button that fails once tapped.
 *
 * ```kotlin
 * if (rememberShareCapabilities().file) {
 *     IconButton(onClick = ::exportPdf) { Icon(Icons.Default.Share, null) }
 * }
 * ```
 */
@Composable
public fun rememberShareCapabilities(): ShareCapabilities {
    val manager = rememberShareManager()
    return remember(manager) { manager.capabilities }
}
