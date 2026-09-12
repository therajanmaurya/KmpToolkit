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
 * Which [SharePayload] kinds the current target can actually hand to another app.
 *
 * ## Why this exists
 * Share is not uniform across the 15 targets. tvOS has no share sheet for binary content at all;
 * Windows can put text on the clipboard but cannot hand over a file; the web can share an image
 * as a file but cannot share a `file://`-style URI it has not fetched. Without a way to ask, a UI
 * has two bad options — render a share button that fails after the user taps it, or hide the
 * feature everywhere because one platform cannot do it.
 *
 * Ask [ShareManager.supports] before rendering the affordance; read [ShareManager.capabilities]
 * when you need the whole picture.
 *
 * A `true` here means *the implementation attempts the payload on this target*, not that it is
 * guaranteed to succeed — a missing handler or a cancelled sheet is still reported per-call
 * through [ShareResult]. See [platformShareCapabilities] for the per-target values and their
 * runtime caveats.
 */
public data class ShareCapabilities(
    /** Plain text. Supported on every target. */
    public val text: Boolean,
    /** A link. Supported on every target. */
    public val url: Boolean,
    /** In-memory image bytes. */
    public val image: Boolean,
    /** An existing file, by URI. */
    public val file: Boolean,
    /** A [SharePayload.Multi] bundle. Individual items are still subject to their own capability. */
    public val multi: Boolean,
) {
    public companion object {
        /** Every payload kind — Android, JVM, iOS, macOS, Linux. */
        public val Full: ShareCapabilities = ShareCapabilities(
            text = true,
            url = true,
            image = true,
            file = true,
            multi = true,
        )

        /** Nothing at all — wasmWasi, which has no surface to share to. */
        public val None: ShareCapabilities = ShareCapabilities(
            text = false,
            url = false,
            image = false,
            file = false,
            multi = false,
        )

        /** Text and links only, with no bundling — tvOS. */
        public val TextAndUrlOnly: ShareCapabilities = ShareCapabilities(
            text = true,
            url = true,
            image = false,
            file = false,
            multi = false,
        )
    }
}

/**
 * What THIS target can share. See each `actual` for the reasoning behind its values.
 *
 * | Target | text | url | image | file | multi |
 * |---|:--:|:--:|:--:|:--:|:--:|
 * | Android, JVM, iOS, macOS, Linux | ✅ | ✅ | ✅ | ✅ | ✅ |
 * | JS, wasmJs | ✅ | ✅ | ✅¹ | ❌ | ✅² |
 * | Windows (mingw) | ✅ | ✅ | ❌ | ❌ | ✅² |
 * | watchOS | ✅³ | ✅³ | ❌ | ❌ | ✅² |
 * | tvOS | ✅ | ✅ | ❌ | ❌ | ❌ |
 * | wasmWasi | ❌ | ❌ | ❌ | ❌ | ❌ |
 *
 * ¹ Needs Web Share Level 2 in the browser; older browsers report [ShareError.NoHandler] per call.
 * ² Text and link items only — binary items inside the bundle are skipped.
 * ³ Handed to the paired iPhone over `WCSession`; [ShareError.NoHandler] when it is unreachable.
 */
public expect val platformShareCapabilities: ShareCapabilities
