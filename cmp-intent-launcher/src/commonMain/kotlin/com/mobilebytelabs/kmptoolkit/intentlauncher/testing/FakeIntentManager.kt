/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
package com.mobilebytelabs.kmptoolkit.intentlauncher.testing

import com.mobilebytelabs.kmptoolkit.intentlauncher.HostIntentRequest
import com.mobilebytelabs.kmptoolkit.intentlauncher.IntentBuilder
import com.mobilebytelabs.kmptoolkit.intentlauncher.IntentCapabilities
import com.mobilebytelabs.kmptoolkit.intentlauncher.IntentManager
import com.mobilebytelabs.kmptoolkit.intentlauncher.IntentResult
import com.mobilebytelabs.kmptoolkit.intentlauncher.snapshotOf

/**
 * In-memory [IntentManager] for tests — shipped in the main artifact, like `FakeShareManager`, so
 * consumers can assert on intent dispatch without a real picker.
 *
 * ```kotlin
 * val intents = FakeIntentManager()
 * AvatarViewModel(intents).choose()
 * assertEquals("image/ *", intents.recorded.single().type)   // (note: no space in real code)
 * ```
 *
 * Constrain the platform to assert capability-dependent UI:
 *
 * ```kotlin
 * val tv = FakeIntentManager(capabilities = IntentCapabilities.None)
 * assertFalse(tv.supports(IntentOperation.PickImage))
 * ```
 *
 * Results come from a FIFO script; when it is empty every call returns [IntentResult.Cancelled] —
 * the outcome a real picker gives when the user backs out, and the one most call sites forget to
 * handle.
 */
public class FakeIntentManager(override val capabilities: IntentCapabilities = IntentCapabilities.Full) :
    IntentManager {

    /** Every [launch] call, in order, as a read-only snapshot of the builder. */
    public val recorded: MutableList<HostIntentRequest> = mutableListOf()

    private val scripted: ArrayDeque<IntentResult> = ArrayDeque()

    /** Queue the result for the next (or a subsequent) call. */
    public fun scriptResult(result: IntentResult) {
        scripted.addLast(result)
    }

    /** Forget every recorded call and queued result. */
    public fun reset() {
        recorded.clear()
        scripted.clear()
    }

    override suspend fun launch(block: IntentBuilder.() -> Unit): IntentResult {
        recorded += snapshotOf(block)
        return scripted.removeFirstOrNull() ?: IntentResult.Cancelled
    }
}
