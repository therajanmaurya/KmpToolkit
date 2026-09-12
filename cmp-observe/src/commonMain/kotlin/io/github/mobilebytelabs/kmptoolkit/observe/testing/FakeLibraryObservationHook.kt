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

import io.github.mobilebytelabs.kmptoolkit.observe.CmpMetadata
import io.github.mobilebytelabs.kmptoolkit.observe.LibraryObservationHook

/**
 * Recording [LibraryObservationHook] for tests — shipped in the main artifact, like
 * `FakeNetworkMonitor` and `FakeAnalyticsHelper`.
 *
 * ## Why the interface alone is not enough
 * `LibraryObservationHook` is a five-method interface, so "just implement it" means five stub
 * overrides in every test that wants to assert one notification. This library's own test carried
 * exactly that boilerplate, and it flattened every callback to a string — which silently dropped the
 * lifecycle `payload`, so the test named *"notifyLifecycle propagates event name + payload"* only
 * ever asserted the name. Recording structured [Event]s instead makes meta, payload and throwable
 * assertable, and is why this class exists rather than a hand-rolled recorder per consumer.
 *
 * ```kotlin
 * val hook = FakeLibraryObservationHook()
 * LibraryObservation.register(hook)
 *
 * myLibrary.initialize()
 *
 * assertEquals("cmp-share", hook.initCompletions.single().meta.name)
 * assertEquals("whatsapp", hook.payloadOf("share_invoked")?.get("target"))
 * ```
 *
 * Pass [failWith] to model the other half of the contract — a hook that throws. [LibraryObservation]
 * documents that hook exceptions are caught and isolated, and this is how a consumer proves their own
 * code still runs when someone else's hook misbehaves:
 *
 * ```kotlin
 * LibraryObservation.register(FakeLibraryObservationHook(failWith = RuntimeException("boom")))
 * LibraryObservation.register(mine)
 * LibraryObservation.notifyInit(meta)   // does not throw; `mine` still receives the notification
 * ```
 *
 * Not thread-safe: [events] is a plain list. [LibraryObservation] fans out on the calling thread, so
 * a test driving notifications from one thread — the normal case — is fine. A test deliberately
 * notifying concurrently should synchronise its own assertions.
 *
 * @param failWith when non-null, every callback throws this instead of recording. The callback is
 *   still counted in [failedCallCount], so a test can assert the hook was actually reached.
 */
public class FakeLibraryObservationHook(private val failWith: Throwable? = null) : LibraryObservationHook {

    /** One entry per callback, in the order received. */
    public sealed interface Event {
        /** The library that emitted this notification. */
        public val meta: CmpMetadata

        public data class InitStart(override val meta: CmpMetadata) : Event

        public data class InitComplete(override val meta: CmpMetadata) : Event

        public data class InitFailure(override val meta: CmpMetadata, public val throwable: Throwable) : Event

        public data class Lifecycle(
            override val meta: CmpMetadata,
            public val event: String,
            public val payload: Map<String, Any?>,
        ) : Event

        public data class Close(override val meta: CmpMetadata) : Event
    }

    /** Every recorded callback, oldest first. Empty when constructed with `failWith`. */
    public val events: MutableList<Event> = mutableListOf()

    /** How many callbacks were reached while [failWith] was set. */
    public var failedCallCount: Int = 0
        private set

    override fun onInitStart(meta: CmpMetadata): Unit = record(Event.InitStart(meta))

    override fun onInitComplete(meta: CmpMetadata): Unit = record(Event.InitComplete(meta))

    override fun onInitFailure(meta: CmpMetadata, throwable: Throwable): Unit =
        record(Event.InitFailure(meta, throwable))

    override fun onLifecycleEvent(meta: CmpMetadata, event: String, payload: Map<String, Any?>): Unit =
        record(Event.Lifecycle(meta, event, payload))

    override fun onClose(meta: CmpMetadata): Unit = record(Event.Close(meta))

    // ── assertion helpers ────────────────────────────────────────────────────────────────────────

    /** Init-start notifications, in order. */
    public val initStarts: List<Event.InitStart> get() = events.filterIsInstance<Event.InitStart>()

    /** Init-complete notifications, in order. */
    public val initCompletions: List<Event.InitComplete> get() = events.filterIsInstance<Event.InitComplete>()

    /** Init-failure notifications, in order. */
    public val initFailures: List<Event.InitFailure> get() = events.filterIsInstance<Event.InitFailure>()

    /** Lifecycle notifications, in order. */
    public val lifecycleEvents: List<Event.Lifecycle> get() = events.filterIsInstance<Event.Lifecycle>()

    /** Close notifications, in order. */
    public val closes: List<Event.Close> get() = events.filterIsInstance<Event.Close>()

    /** Payload of the FIRST lifecycle notification named [event], or null if it never fired. */
    public fun payloadOf(event: String): Map<String, Any?>? = lifecycleEvents.firstOrNull { it.event == event }?.payload

    /** Whether a lifecycle notification named [event] was received. */
    public fun receivedLifecycle(event: String): Boolean = lifecycleEvents.any { it.event == event }

    /** Names of every lifecycle notification received, in order — handy for one-line ordering asserts. */
    public fun lifecycleNames(): List<String> = lifecycleEvents.map { it.event }

    /** Drop all recorded events and reset [failedCallCount]. */
    public fun clear() {
        events.clear()
        failedCallCount = 0
    }

    private fun record(event: Event) {
        val failure = failWith
        if (failure != null) {
            failedCallCount++
            throw failure
        }
        events += event
    }
}
