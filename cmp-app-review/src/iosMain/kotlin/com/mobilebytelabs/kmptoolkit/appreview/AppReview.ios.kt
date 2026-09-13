/*
 * Copyright 2026 MobileByteLabs · Apache 2.0
 */
package com.mobilebytelabs.kmptoolkit.appreview

import platform.StoreKit.SKStoreReviewController

public actual val platformAppReviewCapabilities: AppReviewCapabilities = AppReviewCapabilities.Native

/**
 * StoreKit's review prompt. iOS decides whether it actually renders — the app is told nothing, by
 * design, so this reports only that the request was delivered.
 */
internal actual suspend fun requestNativeReview(): AppReviewResult {
    SKStoreReviewController.requestReview()
    return AppReviewResult.NativeFlowRequested
}

/**
 * `itms-apps` opens the App Store app directly; `action=write-review` lands on the review composer
 * rather than the product page, which is the whole point of the fallback.
 */
internal actual fun resolveStoreUrl(listing: StoreListing): String? {
    val id = listing.appStoreId
    return when {
        id != null -> "itms-apps://itunes.apple.com/app/id$id?action=write-review"
        else -> listing.webUrl
    }
}
