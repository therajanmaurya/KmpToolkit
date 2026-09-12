/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
package io.github.mobilebytelabs.kmptoolkit.observe

import io.github.mobilebytelabs.kmptoolkit.observe.testing.FakeLibraryObservationHook
import io.github.mobilebytelabs.kmptoolkit.observe.testing.resetLibraryObservation
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * TDD coverage for [LibraryObservation] registry + [LibraryObservationHook] contract.
 *
 * Authored 2026-05-30 by library-runtime-observability epic Phase 01 T1 (RED) + T2/T3 (GREEN).
 */
class LibraryObservationTest {

    private val testMeta = CmpMetadata(
        name = "cmp-test",
        version = "1.0.0",
        artifact = "io.github.mobilebytelabs:cmp-test",
    )

    // Via the public `testing` helper rather than the internal reset(), so this suite exercises the
    // same isolation path a consumer has.
    @BeforeTest fun setUp() {
        resetLibraryObservation()
    }

    @AfterTest fun tearDown() {
        resetLibraryObservation()
    }

    @Test fun `notifyInit fans out to all registered hooks`() {
        val h1 = FakeLibraryObservationHook()
        val h2 = FakeLibraryObservationHook()
        LibraryObservation.register(h1)
        LibraryObservation.register(h2)
        LibraryObservation.notifyInit(testMeta)
        assertEquals(listOf(testMeta), h1.initStarts.map { it.meta })
        assertEquals(listOf(testMeta), h2.initStarts.map { it.meta })
    }

    @Test fun `hook exception is isolated — subsequent hooks still run`() {
        // `failWith` replaces five stub overrides of an anonymous object, and unlike that object it
        // also records that the failing hook was actually reached.
        val failingHook = FakeLibraryObservationHook(failWith = RuntimeException("hook crashed"))
        val recordingHook = FakeLibraryObservationHook()
        LibraryObservation.register(failingHook)
        LibraryObservation.register(recordingHook)
        LibraryObservation.notifyInit(testMeta)
        assertEquals(1, failingHook.failedCallCount, "the throwing hook was reached")
        assertTrue(recordingHook.events.isNotEmpty(), "second hook ran despite first throwing")
    }

    @Test fun `notifyInit with zero registered hooks is silent + non-throwing`() {
        LibraryObservation.notifyInit(testMeta) // Should NOT throw
    }

    @Test fun `replaceHooks atomically swaps registered list`() {
        val before = FakeLibraryObservationHook()
        val after = FakeLibraryObservationHook()
        LibraryObservation.register(before)
        LibraryObservation.replaceHooks { _ -> listOf(after) }
        LibraryObservation.notifyInit(testMeta)
        assertEquals(emptyList(), before.events, "old hook unregistered")
        assertEquals(listOf(testMeta), after.initStarts.map { it.meta }, "new hook registered")
    }

    @Test fun `notifyLifecycle propagates event name + payload`() {
        val hook = FakeLibraryObservationHook()
        LibraryObservation.register(hook)
        LibraryObservation.notifyLifecycle(testMeta, "share_invoked", mapOf("target" to "whatsapp"))
        // The payload half of this test's own name was never asserted before: the previous recorder
        // flattened each callback to "lifecycle:<module>:<event>" and dropped the map entirely.
        assertEquals(listOf("share_invoked"), hook.lifecycleNames())
        assertEquals(mapOf("target" to "whatsapp"), hook.payloadOf("share_invoked"))
    }

    @Test fun `notifyInitFailure carries throwable to hook`() {
        val hook = FakeLibraryObservationHook()
        LibraryObservation.register(hook)
        val cause = IllegalStateException("bad config")
        LibraryObservation.notifyInitFailure(testMeta, cause)
        // The throwable IDENTITY is assertable now, not just its message.
        assertEquals(cause, hook.initFailures.single().throwable)
    }
}
