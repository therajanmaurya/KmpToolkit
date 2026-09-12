/*
 * Copyright 2026 MobileByteLabs · Apache 2.0
 */
package com.mobilebytelabs.kmptoolkit.appreview

/**
 * Desktop JVM, Linux, browser and Node: no app store to prompt from, but a browser to open.
 *
 * These targets get [StoreOnly] rather than [AppReviewCapabilities.None] because cmp-open-url can
 * always reach a web page here. Supply [StoreListing.webUrl] — a download page, a Play/App Store web
 * listing, even a feedback form; anything is better than a button that does nothing.
 */
public actual val platformAppReviewCapabilities: AppReviewCapabilities = AppReviewCapabilities.StoreOnly

internal actual suspend fun requestNativeReview(): AppReviewResult =
    AppReviewResult.Failed("no in-app review API on this target")

/**
 * Prefers an explicit [StoreListing.webUrl], then synthesises a Play/App Store *web* listing from
 * whichever id was supplied — a desktop build of a mobile app is a normal case, and the web listing
 * is reachable from any browser.
 */
internal actual fun resolveStoreUrl(listing: StoreListing): String? = listing.webUrl
    ?: listing.playStorePackage?.let { "https://play.google.com/store/apps/details?id=$it" }
    ?: listing.appStoreId?.let { "https://apps.apple.com/app/id$it?action=write-review" }
