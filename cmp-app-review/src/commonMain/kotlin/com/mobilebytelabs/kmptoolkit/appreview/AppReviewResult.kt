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
 * Outcome of a review request.
 *
 * Note what [NativeFlowRequested] does NOT promise: neither Google Play nor StoreKit tells the app
 * whether a prompt actually appeared or what the user did. Both silently do nothing when their own
 * quota says so. Any UI that claims "thanks for rating!" off the back of this is lying — treat it as
 * "the ask was delivered to the OS", and never gate a reward on it.
 */
public sealed class AppReviewResult {

    /**
     * The platform's in-app review flow was handed to the OS. Whether a dialog rendered is
     * deliberately unknowable — see the class KDoc.
     */
    public object NativeFlowRequested : AppReviewResult()

    /** No in-app API here, so the store listing was opened instead. */
    public data class StoreOpened(public val url: String) : AppReviewResult()

    /**
     * This platform has no in-app review API and [StoreListing] carried nothing usable for it.
     * Supply a store id or a `webUrl` — this is a configuration gap, not a platform limitation.
     */
    public object NoStoreConfigured : AppReviewResult()

    /** The attempt failed. [message] is for logs; it is not end-user copy. */
    public data class Failed(public val message: String) : AppReviewResult()
}
