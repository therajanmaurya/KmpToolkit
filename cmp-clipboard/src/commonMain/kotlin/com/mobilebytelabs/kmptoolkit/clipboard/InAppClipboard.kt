/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
package com.mobilebytelabs.kmptoolkit.clipboard

/**
 * An app-scoped clipboard, used by the three targets that have no system one — tvOS, watchOS and
 * wasmWasi.
 *
 * ## Why this exists rather than returning `false`
 * A clipboard is, at bottom, *somewhere to put text and get it back*. Apple ships no
 * `UIPasteboard` to tvOS or watchOS (verified by compiling against it), and WASI has no desktop —
 * but none of that stops a copy from round-tripping **inside the app**, which is what most
 * in-app copy flows actually need: copy this code, paste it into that field, two screens later.
 *
 * Previously all three returned `false` / `null` unconditionally, so `copyToClipboard` looked
 * broken and `getFromClipboard` never returned what was just copied. That is strictly less useful
 * than a buffer, and indistinguishable from a bug.
 *
 * ## What it does NOT claim
 * This is **not** the system clipboard — nothing outside your app can see it, and
 * `platformClipboardCapabilities.systemWide` is `false` on these targets so a UI can say
 * "Copied" rather than "Copied to clipboard" where the distinction matters.
 *
 * ## Reaching further
 * Register a bridge and the copy leaves the process — to a companion phone over `WCSession`, to a
 * WASI host, to anywhere you can reach:
 *
 * ```kotlin
 * InAppClipboard.onCopy = { text -> session.transferUserInfo(mapOf("clip" to text)); true }
 * InAppClipboard.onRead = { lastReceivedFromPhone }
 * ```
 *
 * With a bridge installed the buffer is still updated, so a local read works even when the remote
 * side is unreachable.
 */
public object InAppClipboard {

    /**
     * Called on every copy. Return `false` to report the copy as failed.
     *
     * The buffer is updated either way, so an in-app paste keeps working when the bridge cannot
     * reach its destination.
     */
    public var onCopy: ((String) -> Boolean)? = null

    /** Consulted before the buffer on every read. Return `null` to fall back to the buffer. */
    public var onRead: (() -> String?)? = null

    private var buffer: String? = null
    private val listeners: MutableList<(String) -> Unit> = mutableListOf()

    /** The current app-scoped content, ignoring any [onRead] bridge. */
    public val bufferedContent: String? get() = buffer

    /** Clear the buffer, both bridges and every listener — intended for tests. */
    public fun reset() {
        buffer = null
        onCopy = null
        onRead = null
        listeners.clear()
    }

    /** Subscribe to copies. Used by [InAppClipboardMonitor]; removed via [removeListener]. */
    internal fun addListener(listener: (String) -> Unit) {
        listeners += listener
    }

    internal fun removeListener(listener: (String) -> Unit) {
        listeners -= listener
    }

    internal fun copy(text: String): Boolean {
        buffer = text
        val accepted = onCopy?.invoke(text) ?: true
        // Listeners fire regardless of the bridge's verdict: the app-scoped content DID change,
        // and an in-app monitor should see it even when the remote hop failed.
        listeners.toList().forEach { it(text) }
        return accepted
    }

    internal fun read(): String? = onRead?.invoke() ?: buffer

    internal fun has(): Boolean = !read().isNullOrEmpty()

    internal fun clear() {
        buffer = null
    }
}
