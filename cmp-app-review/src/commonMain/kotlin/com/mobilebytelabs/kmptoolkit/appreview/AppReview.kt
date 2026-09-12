/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
package com.mobilebytelabs.kmptoolkit.appreview

import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi

/**
 * The single commonMain entry point. Configure once, call from shared code, and the right thing
 * happens on every target.
 *
 * ```kotlin
 * // once, at startup — in commonMain
 * AppReview.configure(
 *     StoreListing(
 *         appStoreId = "1234567890",
 *         microsoftStoreProductId = "9NBLGGH4NNS1",
 *         webUrl = "https://example.com/app",
 *     ),
 * )
 *
 * // anywhere in shared code
 * AppReview.requestReview()
 * ```
 *
 * That one call resolves per platform with no `expect`/`actual` in consumer code and no platform
 * branches:
 *
 * | Target | What happens |
 * |---|---|
 * | Android | Play In-App Review; falls back to `market://` if Play declines |
 * | iOS / macOS | `SKStoreReviewController`; falls back to the store's review composer |
 * | tvOS / watchOS | store listing (watchOS routes it to the paired iPhone) |
 * | Windows | `ms-windows-store://review/`, else the web listing |
 * | JVM / Linux / JS / wasmJs | web listing in the browser |
 * | wasmWasi | [AppReviewResult.NoStoreConfigured] — no user, no surface |
 *
 * `playStorePackage` is optional: Android reads the running app's package when it is omitted.
 *
 * ## When to use [AppReviewManager] instead
 * This object is process-global state, which is right for a genuinely global concern but awkward in
 * tests. Inject [AppReviewManager] — and substitute
 * [com.mobilebytelabs.kmptoolkit.appreview.testing.FakeAppReviewManager] — wherever you want to
 * assert *that* your gating logic asked. Both share one implementation; this is the convenience skin.
 */
@OptIn(ExperimentalAtomicApi::class)
public object AppReview {

    // The LISTING is the global, not a manager instance. Every AppReviewManagerImpl reads it at CALL
    // time, which is what lets DI construct the manager before startup configuration has run — the
    // ordering you get for free with `single { AppReviewManagerImpl() }` in a platform module.
    // Storing a configured manager instead would capture an empty listing whenever the graph was
    // built first, and that failure is silent.
    private val listingRef: AtomicReference<StoreListing> = AtomicReference(StoreListing.None)

    // An explicitly installed manager wins over the default, for tests and custom launchers.
    private val overrideRef: AtomicReference<AppReviewManager?> = AtomicReference(null)

    /** The configured listing. Read by every [AppReviewManagerImpl] that was not given its own. */
    public val storeListing: StoreListing get() = listingRef.load()

    /**
     * Point the whole app at your store listing. Call once at startup, before any review request.
     *
     * This is the only configuration step. Wire the ids from wherever your build keeps them — a
     * generated BuildKonfig constant, an app-profile YAML, a remote config — and every injected
     * [AppReviewManager] picks them up, because they read this at call time.
     */
    public fun configure(listing: StoreListing) {
        listingRef.store(listing)
    }

    /**
     * Install a fully built manager, overriding the default for the shared entry point — for a
     * consumer supplying their own [com.mobilebytelabs.kmptoolkit.openurl.UrlLauncher], or to point
     * the global API at a fake in an integration test.
     */
    public fun configure(manager: AppReviewManager) {
        overrideRef.store(manager)
    }

    private val active: AppReviewManager get() = overrideRef.load() ?: AppReviewManagerImpl()

    /** What this target supports, without needing to know which target it is. */
    public val capabilities: AppReviewCapabilities get() = active.capabilities

    /** Ask for a review by the best route this platform has. Never throws. */
    public suspend fun requestReview(): AppReviewResult = active.requestReview()

    /** Open the store listing directly, skipping any native prompt. Never throws. */
    public fun openStoreListing(): AppReviewResult = active.openStoreListing()

    /** Drop the listing and any installed manager. Intended for tests. */
    public fun reset() {
        listingRef.store(StoreListing.None)
        overrideRef.store(null)
    }
}
