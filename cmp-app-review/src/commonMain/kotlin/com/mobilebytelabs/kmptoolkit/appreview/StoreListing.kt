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
 * Where this app is published, so a platform with no in-app review API can still send the user
 * somewhere real.
 *
 * Only fill in the stores you actually ship to. Every field is optional and resolution is
 * per-platform: an iOS build reads [appStoreId], a Windows build reads [microsoftStoreProductId],
 * and anything with no platform-specific entry falls back to [webUrl].
 *
 * ```kotlin
 * val listing = StoreListing(
 *     playStorePackage = "com.example.app",
 *     appStoreId = "1234567890",
 *     webUrl = "https://example.com/download",
 * )
 * ```
 *
 * [playStorePackage] is optional even on Android: the package name is read from the running
 * application when omitted, which is almost always what you want.
 */
public data class StoreListing(
    /** Android application id, e.g. `com.example.app`. Defaults to the running app's package. */
    public val playStorePackage: String? = null,
    /** Apple numeric app id (no `id` prefix), e.g. `1234567890`. Used by iOS, macOS and tvOS. */
    public val appStoreId: String? = null,
    /** Microsoft Store product id, e.g. `9NBLGGH4NNS1`. */
    public val microsoftStoreProductId: String? = null,
    /**
     * Catch-all page for targets with no app store — desktop JVM, Linux, browser, Node.
     * A download or landing page is fine; it is opened in the browser.
     */
    public val webUrl: String? = null,
) {
    /** True when nothing at all was supplied, so no fallback URL can be built on any platform. */
    public val isEmpty: Boolean
        get() = playStorePackage == null &&
            appStoreId == null &&
            microsoftStoreProductId == null &&
            webUrl == null

    public companion object {
        /**
         * Nothing configured. `requestReview()` still uses the native flow where one exists;
         * platforms relying on the fallback return [AppReviewResult.NoStoreConfigured].
         */
        public val None: StoreListing = StoreListing()
    }
}

/**
 * The store URL this platform should open, or null when [listing] carries nothing usable here.
 *
 * Each platform builds its own scheme — `market://` on Android, `itms-apps://` with
 * `action=write-review` on iOS, `ms-windows-store://review/` on Windows — because those deep-link
 * straight to the review composer rather than the store's front page. Web URLs are the fallback of
 * the fallback.
 */
internal expect fun resolveStoreUrl(listing: StoreListing): String?
