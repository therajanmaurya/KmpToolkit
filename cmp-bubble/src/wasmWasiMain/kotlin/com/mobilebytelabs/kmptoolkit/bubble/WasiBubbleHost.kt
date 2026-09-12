/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
package com.mobilebytelabs.kmptoolkit.bubble

/**
 * The wasmWasi notification channel.
 *
 * WASI is headless, so there is no notification surface inside the sandbox — but a notification is
 * a *message to someone outside*, and the host that embedded the module is exactly that. A CLI
 * host can print it, a server host can forward it to a real alerting system.
 *
 * ```kotlin
 * WasiBubbleHost.onNotify = { title, message -> hostAlerting.post(title, message); true }
 * ```
 *
 * With no handler each notification is buffered in [delivered] and echoed to stdout, so a host
 * that only reads process output still receives it. `capability` reports
 * [BubbleCapability.Notification] once a handler is registered and [BubbleCapability.None]
 * before — so a caller can tell the difference rather than guessing.
 */
public object WasiBubbleHost {

    /** Receives every notification. Return `false` to report the delivery as failed. */
    public var onNotify: ((title: String, message: String) -> Boolean)? = null

    /** Whether un-handled notifications are printed to stdout. Default `true`. */
    public var echoToStdout: Boolean = true

    private val buffer: MutableList<Pair<String, String>> = mutableListOf()

    /** Every notification delivered so far and not yet drained, oldest first. */
    public val delivered: List<Pair<String, String>> get() = buffer.toList()

    /** Whether a host handler is registered. */
    public val isConfigured: Boolean get() = onNotify != null

    /** Take everything in [delivered] and empty it. */
    public fun drain(): List<Pair<String, String>> {
        val snapshot = buffer.toList()
        buffer.clear()
        return snapshot
    }

    /** Clear the buffer and handler — intended for tests. */
    public fun reset() {
        buffer.clear()
        onNotify = null
        echoToStdout = true
    }

    internal fun deliver(title: String, message: String): Boolean {
        val handler = onNotify
        if (handler != null) return handler(title, message)
        buffer += title to message
        if (echoToStdout) println("cmp-bubble|$title|$message")
        return true
    }
}
