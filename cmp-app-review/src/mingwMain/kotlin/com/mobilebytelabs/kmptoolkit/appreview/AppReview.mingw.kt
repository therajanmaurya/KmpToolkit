/*
 * Copyright 2026 MobileByteLabs · Apache 2.0
 */
package com.mobilebytelabs.kmptoolkit.appreview

public actual val platformAppReviewCapabilities: AppReviewCapabilities = AppReviewCapabilities.StoreOnly

internal actual suspend fun requestNativeReview(): AppReviewResult =
    AppReviewResult.Failed("Windows has no in-app review API outside the UWP/WinRT sandbox")

/**
 * `ms-windows-store://review/?ProductId=…` opens the Store app on the review pane. Falls back to the
 * web listing for an app distributed outside the Microsoft Store, which most mingw builds are.
 */
internal actual fun resolveStoreUrl(listing: StoreListing): String? {
    val id = listing.microsoftStoreProductId
    return when {
        id != null -> "ms-windows-store://review/?ProductId=$id"
        else -> listing.webUrl
    }
}
