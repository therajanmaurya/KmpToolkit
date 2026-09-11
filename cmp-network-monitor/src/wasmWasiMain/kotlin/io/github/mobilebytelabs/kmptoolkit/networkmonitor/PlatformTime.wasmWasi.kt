/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
package io.github.mobilebytelabs.kmptoolkit.networkmonitor

import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * wasmWasi wall-clock time.
 *
 * ## What changed and why
 * This returned `0L` above a comment reading "WASI has no clock API accessible from Kotlin yet".
 * That is no longer true — `kotlin.time.Clock` resolves on wasmWasi (WASI preview1 exposes
 * `clock_time_get`, and the stdlib binds it), which `cmp-deep-link` has been relying on all along.
 *
 * The constant zero was not harmless: `CachedNetworkState.from()` stamps `timestampMs` with it, so
 * every cached state looked like it was recorded at the epoch and any staleness check comparing it
 * against a real clock treated the cache as infinitely old. The shared `NetworkStateCacheTest`
 * caught it — but no CI job runs the wasmWasi test task, so nobody saw it fail.
 */
@OptIn(ExperimentalTime::class)
internal actual fun currentTimeMillis(): Long = Clock.System.now().toEpochMilliseconds()
