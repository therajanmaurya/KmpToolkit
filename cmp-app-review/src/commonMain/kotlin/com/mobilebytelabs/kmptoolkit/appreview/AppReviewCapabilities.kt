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

/**
 * What review mechanisms this target actually has, so a caller can branch before asking rather than
 * discovering it from a result.
 *
 * A typical use is deciding whether to show your own "enjoying the app?" pre-prompt: worth it when
 * [nativeInAppReview] is true (the OS quota is precious), pointless when only [storeListing] is.
 */
public data class AppReviewCapabilities(
    /** A system review prompt exists — Play In-App Review, or StoreKit's review controller. */
    public val nativeInAppReview: Boolean,
    /** A store listing can be opened for this platform, given a configured [StoreListing]. */
    public val storeListing: Boolean,
) {
    /** True when a review can be requested at all, by either route. */
    public val canRequestReview: Boolean get() = nativeInAppReview || storeListing

    public companion object {
        /** Android, iOS, macOS — a native prompt AND a store to fall back to. */
        public val Native: AppReviewCapabilities =
            AppReviewCapabilities(nativeInAppReview = true, storeListing = true)

        /** tvOS, watchOS, desktop, browser — no in-app API, but a store/web page can be opened. */
        public val StoreOnly: AppReviewCapabilities =
            AppReviewCapabilities(nativeInAppReview = false, storeListing = true)

        /** wasmWasi — no store, no browser, no window to open one in. */
        public val None: AppReviewCapabilities =
            AppReviewCapabilities(nativeInAppReview = false, storeListing = false)
    }
}

/** What THIS target supports. */
public expect val platformAppReviewCapabilities: AppReviewCapabilities
