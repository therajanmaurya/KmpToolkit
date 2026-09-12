/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
package io.github.mobilebytelabs.kmptoolkit.observe.testing

import io.github.mobilebytelabs.kmptoolkit.observe.LibraryObservation

/**
 * Unregister every hook. Call from `@BeforeTest` / `@AfterTest`.
 *
 * ## Why this is needed
 * [LibraryObservation] is a process-wide `object`, so hooks registered by one test are still
 * registered in the next — a later test then sees notifications it never caused, and assertions on
 * "how many events did my hook receive" depend on test execution order. `LibraryObservation` has
 * always had a `reset()` for exactly this, but it is `internal`: it worked for this library's own
 * suite and was unreachable from any consumer, leaving them to either accept the leak or avoid
 * asserting counts at all. This exposes it, and nothing else.
 *
 * ```kotlin
 * class MyHookTest {
 *     private val hook = FakeLibraryObservationHook()
 *
 *     @BeforeTest fun setUp() {
 *         resetLibraryObservation()
 *         LibraryObservation.register(hook)
 *     }
 *
 *     @AfterTest fun tearDown() = resetLibraryObservation()
 * }
 * ```
 *
 * Deliberately a top-level function in `testing/` rather than making `LibraryObservation.reset()`
 * public: production callers have no business clearing another library's hooks, and the `testing`
 * package keeps that intent legible at the call site.
 */
public fun resetLibraryObservation() {
    LibraryObservation.reset()
}
