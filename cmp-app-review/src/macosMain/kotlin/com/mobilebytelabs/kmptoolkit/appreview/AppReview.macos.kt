/*
 * Copyright 2026 MobileByteLabs · Apache 2.0
 */
package com.mobilebytelabs.kmptoolkit.appreview

import platform.StoreKit.SKStoreReviewController

public actual val platformAppReviewCapabilities: AppReviewCapabilities = AppReviewCapabilities.Native

/** Available on macOS 10.14+, with the same "the OS decides" contract as iOS. */
internal actual suspend fun requestNativeReview(): AppReviewResult {
    SKStoreReviewController.requestReview()
    return AppReviewResult.NativeFlowRequested
}

/** `macappstore://` opens the Mac App Store app; the web URL works for a notarised direct download. */
internal actual fun resolveStoreUrl(listing: StoreListing): String? {
    val id = listing.appStoreId
    return when {
        id != null -> "macappstore://apps.apple.com/app/id$id?action=write-review"
        else -> listing.webUrl
    }
}
