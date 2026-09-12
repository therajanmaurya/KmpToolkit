/*
 * Copyright 2026 MobileByteLabs · Apache 2.0
 */
package io.github.mobilebytelabs.kmptoolkit.observe.testing

import io.github.mobilebytelabs.kmptoolkit.observe.CmpMetadata
import io.github.mobilebytelabs.kmptoolkit.observe.LibraryObservation
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The fake is shipped public API, so it carries its own coverage rather than being trusted because
 * [io.github.mobilebytelabs.kmptoolkit.observe.LibraryObservationTest] happens to use it.
 */
class FakeLibraryObservationHookTest {

    private val meta = CmpMetadata("cmp-test", "1.0.0", "io.github.mobilebytelabs:cmp-test")
    private val other = CmpMetadata("cmp-other", "2.0.0", "io.github.mobilebytelabs:cmp-other")

    @BeforeTest fun setUp() = resetLibraryObservation()

    @AfterTest fun tearDown() = resetLibraryObservation()

    @Test
    fun every_callback_is_recorded_as_its_own_structured_event() {
        val hook = FakeLibraryObservationHook()
        val cause = IllegalStateException("bad config")

        hook.onInitStart(meta)
        hook.onInitComplete(meta)
        hook.onInitFailure(meta, cause)
        hook.onLifecycleEvent(meta, "share_invoked", mapOf("target" to "whatsapp"))
        hook.onClose(meta)

        assertEquals(5, hook.events.size, "one event per callback, nothing collapsed")
        assertEquals(listOf(meta), hook.initStarts.map { it.meta })
        assertEquals(listOf(meta), hook.initCompletions.map { it.meta })
        assertEquals(cause, hook.initFailures.single().throwable)
        assertEquals(listOf(meta), hook.closes.map { it.meta })
        assertEquals(
            FakeLibraryObservationHook.Event.Lifecycle(meta, "share_invoked", mapOf("target" to "whatsapp")),
            hook.lifecycleEvents.single(),
        )
    }

    @Test
    fun the_payload_survives_recording() {
        // The whole reason this fake exists: the recorder it replaced flattened callbacks to strings
        // and dropped the map.
        val hook = FakeLibraryObservationHook()
        hook.onLifecycleEvent(meta, "purchase", mapOf("amount" to "12.00", "currency" to null))

        assertEquals(mapOf("amount" to "12.00", "currency" to null), hook.payloadOf("purchase"))
        assertTrue(hook.receivedLifecycle("purchase"))
        assertFalse(hook.receivedLifecycle("refund"))
        assertNull(hook.payloadOf("refund"), "an event that never fired has no payload")
    }

    @Test
    fun events_keep_their_arrival_order_and_their_originating_module() {
        val hook = FakeLibraryObservationHook()
        hook.onLifecycleEvent(meta, "first", emptyMap())
        hook.onLifecycleEvent(other, "second", emptyMap())
        hook.onLifecycleEvent(meta, "third", emptyMap())

        assertEquals(listOf("first", "second", "third"), hook.lifecycleNames())
        assertEquals(listOf("cmp-test", "cmp-other", "cmp-test"), hook.lifecycleEvents.map { it.meta.name })
    }

    @Test
    fun failWith_throws_from_every_callback_and_counts_the_reach() {
        val boom = RuntimeException("boom")
        val hook = FakeLibraryObservationHook(failWith = boom)

        // Driven through the registry, which is the contract under test: LibraryObservation catches.
        LibraryObservation.register(hook)
        LibraryObservation.notifyInit(meta)
        LibraryObservation.notifyClose(meta)

        assertEquals(2, hook.failedCallCount, "both callbacks were reached and both threw")
        assertTrue(hook.events.isEmpty(), "a failing hook records nothing")
    }

    @Test
    fun clear_drops_events_and_the_failure_count() {
        val hook = FakeLibraryObservationHook()
        hook.onInitStart(meta)
        hook.onLifecycleEvent(meta, "e", emptyMap())
        hook.clear()

        assertTrue(hook.events.isEmpty())
        assertEquals(0, hook.failedCallCount)
        assertTrue(hook.lifecycleNames().isEmpty())
    }

    @Test
    fun resetLibraryObservation_unregisters_hooks_so_tests_cannot_leak_into_each_other() {
        // The gap this helper closes: LibraryObservation is a process-wide object and its reset() was
        // internal, so a consumer could not isolate suites at all.
        val first = FakeLibraryObservationHook()
        LibraryObservation.register(first)
        LibraryObservation.notifyInit(meta)
        assertEquals(1, first.events.size)

        resetLibraryObservation()

        val second = FakeLibraryObservationHook()
        LibraryObservation.register(second)
        LibraryObservation.notifyInit(meta)

        assertEquals(1, first.events.size, "the unregistered hook received nothing further")
        assertEquals(1, second.events.size)
    }
}
