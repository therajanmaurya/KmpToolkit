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
 * tvOS `Share` — routes every payload to the app-supplied [TvosShare.handler].
 *
 * ## What changed and why
 * This previously probed for a `CmpShareTvosBridge` Objective-C class and, if the class existed,
 * returned [ShareResult.Completed] **without dispatching anything** — a comment conceded the real
 * dispatch was "deferred". Every caller was told the share succeeded while nothing happened, and
 * the capability matrix inherited that claim. A silent no-op reporting success is the one outcome
 * this library must never produce, so the probe is gone; delivery now goes through a Kotlin
 * handler whose return value is the truth.
 *
 * Unlike wasmWasi there is no stdout-style default here: a tvOS app with no handler has nowhere
 * for the content to go, so the honest answer is [ShareError.NoHandler] — and
 * [platformShareCapabilities] says so up front.
 */
public actual object Share {
    public actual suspend fun share(payload: SharePayload, options: ShareOptions): ShareResult {
        if (!TvosShare.isConfigured) {
            return ShareResult.Failed(
                ShareError.Unknown(
                    "No TvosShare.handler registered. tvOS has no share sheet and no pasteboard, " +
                        "so the app must decide what sharing means (QR code, companion-app push, " +
                        "backend call). Set TvosShare.handler at startup, or check " +
                        "ShareManager.supports(payload) before offering the action.",
                ),
            )
        }
        val items = flattenToHostItems(payload, options)
        if (items.isEmpty()) return ShareResult.Failed(ShareError.Unknown("Empty payload"))
        val accepted = items.all { TvosShare.deliver(it) }
        return if (accepted) ShareResult.Completed else ShareResult.Failed(ShareError.NoHandler)
    }
}
