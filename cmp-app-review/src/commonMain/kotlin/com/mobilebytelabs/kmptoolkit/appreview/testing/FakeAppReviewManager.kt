/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
package com.mobilebytelabs.kmptoolkit.appreview.testing

import com.mobilebytelabs.kmptoolkit.appreview.AppReviewCapabilities
import com.mobilebytelabs.kmptoolkit.appreview.AppReviewManager
import com.mobilebytelabs.kmptoolkit.appreview.AppReviewResult

/**
 * Recording [AppReviewManager] for tests — shipped in the main artifact, like `FakeUrlLauncher`.
 *
 * The real manager cannot be asserted against: Play and StoreKit deliberately report nothing about
 * whether a prompt appeared, and both are rate-limited, so a test that calls the real thing learns
 * only that it did not crash. What a test actually wants is *did my code ask, and how often* —
 * gating logic ("prompt after the third successful export, at most once per release") is the part
 * worth testing, and it is all on the caller's side.
 *
 * ```kotlin
 * val review = FakeAppReviewManager()
 * repeat(3) { viewModel.onExportSucceeded() }
 *
 * assertEquals(1, review.requestCount)   // asked once, not three times
 * ```
 *
 * @param capabilities what the fake claims to support — set [AppReviewCapabilities.StoreOnly] to
 *   exercise the branch a desktop or tvOS build takes.
 * @param result what both entry points return.
 */
public class FakeAppReviewManager(
    override val capabilities: AppReviewCapabilities = AppReviewCapabilities.Native,
    public var result: AppReviewResult = AppReviewResult.NativeFlowRequested,
) : AppReviewManager {

    /** How many times [requestReview] was called. */
    public var requestCount: Int = 0
        private set

    /** How many times [openStoreListing] was called, including via [requestReview]'s fallback. */
    public var storeOpenCount: Int = 0
        private set

    /** Every call in order, as `"request"` / `"store"` — for asserting sequences. */
    public val calls: MutableList<String> = mutableListOf()

    override suspend fun requestReview(): AppReviewResult {
        requestCount++
        calls += "request"
        return result
    }

    override fun openStoreListing(): AppReviewResult {
        storeOpenCount++
        calls += "store"
        return result
    }

    /** Reset counters and recorded calls. [capabilities] and [result] are left as configured. */
    public fun clear() {
        requestCount = 0
        storeOpenCount = 0
        calls.clear()
    }
}
