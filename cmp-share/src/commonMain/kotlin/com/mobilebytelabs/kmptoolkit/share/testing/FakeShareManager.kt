/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
package com.mobilebytelabs.kmptoolkit.share.testing

import com.mobilebytelabs.kmptoolkit.share.ShareCapabilities
import com.mobilebytelabs.kmptoolkit.share.ShareError
import com.mobilebytelabs.kmptoolkit.share.ShareManager
import com.mobilebytelabs.kmptoolkit.share.ShareOptions
import com.mobilebytelabs.kmptoolkit.share.SharePayload
import com.mobilebytelabs.kmptoolkit.share.ShareResult

/** One recorded [ShareManager.share] call. */
public data class RecordedShare(public val payload: SharePayload, public val options: ShareOptions)

/**
 * In-memory [ShareManager] for tests — shipped in the main artifact, like
 * `FakeNetworkMonitor`, so consumers can assert on sharing without a real share sheet.
 *
 * ```kotlin
 * val share = FakeShareManager()
 * ReportViewModel(share).exportPdf("file:///report.pdf")
 *
 * val payload = share.recorded.single().payload
 * assertIs<SharePayload.Multi>(payload)   // message + file, bundled by shareFile()
 * ```
 *
 * Constrain the platform to assert capability-dependent UI:
 *
 * ```kotlin
 * val tv = FakeShareManager(capabilities = ShareCapabilities.TextAndUrlOnly)
 * assertFalse(tv.supports(SharePayload.File("file:///a.pdf", "application/pdf")))
 * ```
 *
 * Results are returned from a FIFO script; when it is empty every call returns
 * [ShareResult.Completed]. Queue failures with [scriptResult] to drive error paths.
 */
public class FakeShareManager(override val capabilities: ShareCapabilities = ShareCapabilities.Full) : ShareManager {

    /** Every [share] call, in order. */
    public val recorded: MutableList<RecordedShare> = mutableListOf()

    private val scripted: ArrayDeque<ShareResult> = ArrayDeque()

    /** Queue the result for the next (or a subsequent) [share] call. */
    public fun scriptResult(result: ShareResult) {
        scripted.addLast(result)
    }

    /** Queue a [ShareResult.Failed] with [cause]. */
    public fun scriptError(cause: ShareError) {
        scripted.addLast(ShareResult.Failed(cause))
    }

    /** Forget every recorded call and queued result. */
    public fun reset() {
        recorded.clear()
        scripted.clear()
    }

    override suspend fun share(payload: SharePayload, options: ShareOptions): ShareResult {
        recorded += RecordedShare(payload, options)
        return scripted.removeFirstOrNull() ?: ShareResult.Completed
    }
}
