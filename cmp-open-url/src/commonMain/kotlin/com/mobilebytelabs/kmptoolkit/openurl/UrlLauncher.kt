/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
package com.mobilebytelabs.kmptoolkit.openurl

/**
 * Injectable URL opening — the type to depend on from a ViewModel, repository or composable.
 *
 * ## Why an interface when top-level functions already exist
 * `openUrl(...)` and friends are top-level `expect fun`s. A function cannot be substituted, so
 * code calling them is untestable without actually launching a browser, and cannot be wrapped in a
 * decorator that adds analytics, an "external link" confirmation, or an allow-list. [UrlLauncher]
 * is the same capability behind an injectable type. The top-level functions stay public.
 *
 * ## Implementing
 * All four members are abstract, and each maps to exactly one function — a fake is short and
 * obvious:
 *
 * ```kotlin
 * class RecordingLauncher : UrlLauncher {
 *     val opened = mutableListOf<String>()
 *     override fun open(url: String) = opened.add(url)
 *     override fun openInBrowser(url: String) = open(url)
 *     override fun openWith(url: String, appHint: AppHint) =
 *         if (open(url)) OpenUrlResult.Success else OpenUrlResult.NoHandler
 *     override fun canOpen(url: String) = true
 * }
 * ```
 *
 * Or just use `FakeUrlLauncher`, which ships in this artifact.
 *
 * ## Using
 * ```kotlin
 * class ArticleViewModel(private val urls: UrlLauncher) : ViewModel() {
 *     fun openSource(url: String) {
 *         // Ask before offering — do not render a dead link.
 *         if (urls.canOpen(url)) urls.open(url)
 *     }
 * }
 * ```
 */
public interface UrlLauncher {

    /** Open [url] with the platform's default handler. `false` if nothing could handle it. */
    public fun open(url: String): Boolean

    /** Open [url] in a browser specifically, bypassing any app that claims the link. */
    public fun openInBrowser(url: String): Boolean

    /** Open [url] with a specific kind of app — mail client, maps, dialer. */
    public fun openWith(url: String, appHint: AppHint = AppHint.DEFAULT): OpenUrlResult

    /** Whether [url] has a handler — check BEFORE offering the action, not after it fails. */
    public fun canOpen(url: String): Boolean
}

/**
 * The one [UrlLauncher] — for every target.
 *
 * Three one-line delegations to the platform functions. Stateless, so a single instance is safe to
 * share (which is why the DI module binds it as a singleton).
 */
public class UrlLauncherImpl : UrlLauncher {

    override fun open(url: String): Boolean = openUrl(url)

    override fun openInBrowser(url: String): Boolean = com.mobilebytelabs.kmptoolkit.openurl.openInBrowser(url)

    override fun openWith(url: String, appHint: AppHint): OpenUrlResult = openWithApp(url, appHint)

    override fun canOpen(url: String): Boolean = com.mobilebytelabs.kmptoolkit.openurl.canOpen(url)
}
