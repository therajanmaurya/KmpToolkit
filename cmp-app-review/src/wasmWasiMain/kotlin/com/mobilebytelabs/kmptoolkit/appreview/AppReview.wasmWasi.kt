/*
 * Copyright 2026 MobileByteLabs · Apache 2.0
 */
package com.mobilebytelabs.kmptoolkit.appreview

/**
 * wasmWasi is a sandboxed server-side runtime: no store, no browser, and nothing to open a URL in.
 *
 * This is the one target where [AppReviewCapabilities.None] is the honest answer rather than a
 * cop-out — there is no degraded path to design, because there is no user sitting in front of it.
 * A WASI host that *does* want to surface a prompt should read
 * [com.mobilebytelabs.kmptoolkit.appreview.AppReviewManager.capabilities], see `canRequestReview ==
 * false`, and route the request through its own host bridge.
 */
public actual val platformAppReviewCapabilities: AppReviewCapabilities = AppReviewCapabilities.None

internal actual suspend fun requestNativeReview(): AppReviewResult =
    AppReviewResult.Failed("wasmWasi has no review surface")

internal actual fun resolveStoreUrl(listing: StoreListing): String? = null
