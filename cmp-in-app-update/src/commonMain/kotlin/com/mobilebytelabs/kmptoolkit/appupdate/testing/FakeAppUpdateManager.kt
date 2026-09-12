/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
package com.mobilebytelabs.kmptoolkit.appupdate.testing

import com.mobilebytelabs.kmptoolkit.appupdate.AppUpdateManager
import com.mobilebytelabs.kmptoolkit.appupdate.AppVersion
import com.mobilebytelabs.kmptoolkit.appupdate.UpdateOutcome
import com.mobilebytelabs.kmptoolkit.appupdate.UpdateType

/**
 * In-memory [AppUpdateManager] for tests — shipped in the main artifact, so an update prompt can
 * be exercised without a real store.
 *
 * ```kotlin
 * val updates = FakeAppUpdateManager(scripted = UpdateOutcome.UpdateStarted)
 * UpdateGate(updates).onLaunch()
 * assertEquals(1, updates.checkAndStartCalls)
 * ```
 *
 * Simulate a platform with no in-app flow, and assert the app falls back to the store link:
 *
 * ```kotlin
 * val tv = FakeAppUpdateManager(
 *     supported = false,
 *     scripted = UpdateOutcome.NotSupported("tvOS has no in-app update flow"),
 * )
 * UpdateGate(tv).onLaunch()
 * assertEquals(1, tv.openStoreCalls)
 * ```
 */
public class FakeAppUpdateManager(
    private val supported: Boolean = true,
    private val version: AppVersion = AppVersion.UNKNOWN,
    /** Returned by [check], [checkAndStart] and [start] unless a per-call script is queued. */
    public var scripted: UpdateOutcome = UpdateOutcome.UpToDate,
    /** What [openStore] reports. */
    public var storeOpens: Boolean = true,
) : AppUpdateManager {

    public var checkCalls: Int = 0
        private set
    public var checkAndStartCalls: Int = 0
        private set
    public var startCalls: Int = 0
        private set
    public var openStoreCalls: Int = 0
        private set

    /** The update type each [checkAndStart] / [start] was asked for, in order. */
    public val requestedTypes: MutableList<UpdateType> = mutableListOf()

    override fun isSupported(): Boolean = supported

    override fun currentVersion(): AppVersion = version

    override suspend fun check(): UpdateOutcome {
        checkCalls++
        return scripted
    }

    override suspend fun checkAndStart(updateType: UpdateType): UpdateOutcome {
        checkAndStartCalls++
        requestedTypes += updateType
        return scripted
    }

    override suspend fun start(updateType: UpdateType): UpdateOutcome {
        startCalls++
        requestedTypes += updateType
        return scripted
    }

    override fun openStore(): Boolean {
        openStoreCalls++
        return storeOpens
    }

    /** Zero every counter and recorded type. */
    public fun reset() {
        checkCalls = 0
        checkAndStartCalls = 0
        startCalls = 0
        openStoreCalls = 0
        requestedTypes.clear()
    }
}
