/*
 * Copyright 2026 MobileByteLabs · Apache 2.0
 */
package com.mobilebytelabs.kmptoolkit.appreview

/**
 * tvOS and watchOS ship no `SKStoreReviewController` — it is genuinely absent from those SDKs, not
 * merely unsupported at runtime. Both can still reach a store page, so this is [StoreOnly] rather
 * than [AppReviewCapabilities.None]: the user gets somewhere real instead of a dead button.
 *
 * On watchOS the URL is handed to cmp-open-url, whose `WKExtension.openSystemURL` routes an https
 * link to the paired iPhone — which is where the review would be written anyway.
 */
public actual val platformAppReviewCapabilities: AppReviewCapabilities = AppReviewCapabilities.StoreOnly

internal actual suspend fun requestNativeReview(): AppReviewResult =
    AppReviewResult.Failed("no in-app review API on this Apple platform")

/**
 * `https`, not `itms-apps`: the App Store app does not exist on watchOS, and on tvOS the scheme
 * opens the tvOS store which has no review composer. The web listing is the honest target.
 */
internal actual fun resolveStoreUrl(listing: StoreListing): String? {
    val id = listing.appStoreId
    return when {
        id != null -> "https://apps.apple.com/app/id$id?action=write-review"
        else -> listing.webUrl
    }
}
