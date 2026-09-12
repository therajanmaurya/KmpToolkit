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
 * The tvOS share channel — see [HostShareItem] for why a target with no share surface still has
 * somewhere to share to.
 *
 * ## Why a bridge and not a system API
 * tvOS ships **no** `UIActivityViewController` and **no** `UIPasteboard` — the latter is not a
 * Kotlin/Native binding gap, Apple simply does not include it in the tvOS SDK (verified by
 * compiling against it). There is no OS-level "hand this to another app" on tvOS at all. What a
 * tvOS app *can* do is act on the content itself: show a QR code for a link, display a pairing
 * code, push it to a companion phone app, or send it to a backend. That decision belongs to the
 * app, so cmp-share routes the payload to it instead of pretending.
 *
 * ## Wiring it
 * ```kotlin
 * // once, at startup
 * TvosShare.handler = { item ->
 *     when (item.kind) {
 *         "url", "text" -> { showQrCodeOverlay(item.value!!); true }
 *         else -> false          // -> ShareResult.Failed(NoHandler)
 *     }
 * }
 * ```
 *
 * With no handler registered, [platformShareCapabilities] reports [ShareCapabilities.None], so
 * `supports()` is `false` and a UI can hide its share affordance rather than offering a button
 * that cannot work. Register a handler and capabilities become [ShareCapabilities.Full].
 *
 * A handler may still decline an individual item — returning `false` for image bytes it has no
 * use for, say — which surfaces as [ShareError.NoHandler] for that call.
 */
public object TvosShare {

    /**
     * Where shared content goes on tvOS. Return `true` when the item was consumed.
     *
     * Setting this to a non-`null` value flips [platformShareCapabilities] from
     * [ShareCapabilities.None] to [ShareCapabilities.Full].
     */
    public var handler: ((HostShareItem) -> Boolean)? = null

    /** Whether a handler is currently registered. */
    public val isConfigured: Boolean get() = handler != null

    /** Unregister the handler — intended for tests. */
    public fun reset() {
        handler = null
    }

    internal fun deliver(item: HostShareItem): Boolean = handler?.invoke(item) ?: false
}
