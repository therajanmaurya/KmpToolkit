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

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * One payload item crossing a **host boundary**.
 *
 * ## Where this applies
 * Two targets have no share sheet, no clipboard and no other application to hand content to —
 * tvOS and wasmWasi. That does not make sharing impossible; it means the destination is not
 * inside the sandbox. On both, the only reachable "somewhere else" is whatever embedded this
 * code: a companion Swift layer on tvOS, the WASI host on wasmWasi. cmp-share treats that
 * boundary as the share target instead of refusing.
 *
 * A [SharePayload.Multi] flattens into several items, so a host never has to unpack a nested
 * structure. See [TvosShare] and [WasiShare] for the per-target channels.
 */
public class HostShareItem(
    /** `"text"`, `"url"`, `"image"` or `"file"` — never `"multi"`; bundles are flattened. */
    public val kind: String,
    /** Text content, the link, or the file URI. `null` for [SharePayload.Image]. */
    public val value: String?,
    /** Raw image bytes. `null` for every other kind. */
    public val bytes: ByteArray?,
    /** MIME type for image and file items. */
    public val mimeType: String?,
    /** Suggested file name, when the payload carried one. */
    public val filename: String?,
    /** [ShareOptions.chooserTitle], if the caller set one. */
    public val title: String?,
) {
    /**
     * A single-line, parseable rendering, for hosts that receive text rather than objects.
     *
     * Image bytes are Base64. Field order is stable, and `|`, newlines and backslashes inside
     * values are escaped, so one item is always exactly one line.
     */
    @OptIn(ExperimentalEncodingApi::class)
    public fun encode(): String = buildString {
        append("cmp-share|").append(kind)
        title?.let { append("|title=").append(escape(it)) }
        mimeType?.let { append("|mime=").append(escape(it)) }
        filename?.let { append("|name=").append(escape(it)) }
        value?.let { append("|value=").append(escape(it)) }
        bytes?.let { append("|base64=").append(Base64.encode(it)) }
    }

    private fun escape(s: String): String = s
        .replace("\\", "\\\\")
        .replace("|", "\\p")
        .replace("\n", "\\n")
        .replace("\r", "\\r")

    override fun toString(): String = encode()
}

/** Flatten [payload] into the items a host receives. Nested bundles flatten all the way down. */
internal fun flattenToHostItems(payload: SharePayload, options: ShareOptions): List<HostShareItem> = when (payload) {
    is SharePayload.Text -> listOf(
        HostShareItem("text", payload.content, null, payload.mimeType, null, options.chooserTitle),
    )

    is SharePayload.Url -> listOf(
        HostShareItem("url", payload.href, null, null, null, options.chooserTitle),
    )

    is SharePayload.Image -> listOf(
        HostShareItem("image", null, payload.bytes, payload.mimeType, payload.filename, options.chooserTitle),
    )

    is SharePayload.File -> listOf(
        HostShareItem("file", payload.uri, null, payload.mimeType, payload.filename, options.chooserTitle),
    )

    is SharePayload.Multi -> payload.items.flatMap { flattenToHostItems(it, options) }
}
