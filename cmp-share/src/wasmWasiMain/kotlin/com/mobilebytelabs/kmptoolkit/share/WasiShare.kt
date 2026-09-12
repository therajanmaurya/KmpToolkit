/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
package com.mobilebytelabs.kmptoolkit.share

/**
 * The WASI share channel — see [HostShareItem] for why a sandbox still has somewhere to share to.
 *
 * ## Two ways to receive
 *
 * **1. Register a handler** — the embedder decides what sharing means (POST it, write a file,
 * push to a queue):
 *
 * ```kotlin
 * WasiShare.handler = { item ->
 *     hostQueue.publish(item.kind, item.value, item.bytes)
 *     true            // false -> the caller sees ShareResult.Failed(NoHandler)
 * }
 * ```
 *
 * **2. Do nothing** — with no handler every item lands in [outbox] and is echoed to stdout as one
 * [HostShareItem.encode] line, so a host that only reads process output still receives it. WASI
 * always has stdout, which is why wasmWasi reports full capabilities with no setup at all.
 *
 * ```kotlin
 * Share.text("hello")
 * WasiShare.drain().single().value   // "hello"
 * ```
 */
public object WasiShare {

    /**
     * Host interception point. Return `true` when the item was accepted; `false` surfaces to the
     * caller as [ShareResult.Failed] with [ShareError.NoHandler].
     *
     * When set, [outbox] and [echoToStdout] are bypassed — the host owns delivery.
     */
    public var handler: ((HostShareItem) -> Boolean)? = null

    /** Whether un-handled items are printed to stdout. Default `true`. */
    public var echoToStdout: Boolean = true

    private val buffer: MutableList<HostShareItem> = mutableListOf()

    /** Everything shared so far and not yet drained, oldest first. */
    public val outbox: List<HostShareItem> get() = buffer.toList()

    /** Take everything in [outbox] and empty it. */
    public fun drain(): List<HostShareItem> {
        val snapshot = buffer.toList()
        buffer.clear()
        return snapshot
    }

    /** Drop buffered items and restore defaults — intended for tests. */
    public fun reset() {
        buffer.clear()
        handler = null
        echoToStdout = true
    }

    internal fun deliver(item: HostShareItem): Boolean {
        val hostHandler = handler
        if (hostHandler != null) return hostHandler(item)
        buffer += item
        if (echoToStdout) println(item.encode())
        return true
    }
}

/**
 * wasmWasi `Share` — delivers every payload across the host boundary via [WasiShare].
 *
 * An empty [SharePayload.Multi] carries nothing, so it reports [ShareError.Unknown] rather than a
 * hollow success.
 */
public actual object Share {
    public actual suspend fun share(payload: SharePayload, options: ShareOptions): ShareResult {
        val items = flattenToHostItems(payload, options)
        if (items.isEmpty()) return ShareResult.Failed(ShareError.Unknown("Empty payload"))
        // All-or-nothing: a partially delivered bundle would report success while dropping content.
        val accepted = items.all { WasiShare.deliver(it) }
        return if (accepted) ShareResult.Completed else ShareResult.Failed(ShareError.NoHandler)
    }
}
