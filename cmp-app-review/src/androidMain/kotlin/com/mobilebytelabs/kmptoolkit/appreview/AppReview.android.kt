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

import com.google.android.play.core.review.ReviewManagerFactory
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

public actual val platformAppReviewCapabilities: AppReviewCapabilities = AppReviewCapabilities.Native

/**
 * Google Play In-App Review.
 *
 * Two-step by design: `requestReviewFlow()` fetches a `ReviewInfo` token, then `launchReviewFlow()`
 * consumes it. The token is short-lived and single-use, so they are always issued together here
 * rather than cached.
 *
 * Returns [AppReviewResult.Failed] — not an exception — whenever the flow cannot be attempted, so
 * [AppReviewManagerImpl] falls through to the store listing. The three real cases:
 *  - **no Activity**: the app is backgrounded, or the init provider was stripped by a manifest merge;
 *  - **not installed from Play**: sideloaded and debug builds get an error from the API;
 *  - **quota exhausted**: Play silently succeeds without showing anything, which is indistinguishable
 *    from success and is exactly why [AppReviewResult.NativeFlowRequested] promises nothing.
 */
internal actual suspend fun requestNativeReview(): AppReviewResult {
    val context = AppReviewContext.context
        ?: return AppReviewResult.Failed("no application context — AppReviewInitProvider did not run")
    val activity = AppReviewContext.activity
        ?: return AppReviewResult.Failed("no resumed Activity to launch the review flow into")

    val manager = ReviewManagerFactory.create(context)

    return suspendCancellableCoroutine { cont ->
        manager.requestReviewFlow()
            .addOnCompleteListener { request ->
                if (!request.isSuccessful) {
                    cont.resume(
                        AppReviewResult.Failed(
                            "requestReviewFlow failed: ${request.exception?.message ?: "unknown"}",
                        ),
                    )
                    return@addOnCompleteListener
                }
                manager.launchReviewFlow(activity, request.result)
                    .addOnCompleteListener { launch ->
                        // Play reports completion, never the user's action or whether a dialog showed.
                        if (launch.isSuccessful) {
                            cont.resume(AppReviewResult.NativeFlowRequested)
                        } else {
                            cont.resume(
                                AppReviewResult.Failed(
                                    "launchReviewFlow failed: ${launch.exception?.message ?: "unknown"}",
                                ),
                            )
                        }
                    }
            }
    }
}

/**
 * `market://` opens the Play Store app straight to the listing; the package defaults to the running
 * app, which is what a consumer almost always means.
 */
internal actual fun resolveStoreUrl(listing: StoreListing): String? {
    val pkg = listing.playStorePackage ?: AppReviewContext.context?.packageName
    return when {
        pkg != null -> "market://details?id=$pkg"
        else -> listing.webUrl
    }
}
