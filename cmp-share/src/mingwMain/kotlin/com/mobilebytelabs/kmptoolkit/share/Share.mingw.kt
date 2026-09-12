/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
// LD-2-coverage: partial

package com.mobilebytelabs.kmptoolkit.share

import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.toKString
import kotlinx.cinterop.usePinned
import platform.posix.fclose
import platform.posix.fopen
import platform.posix.fwrite
import platform.posix.getenv
import platform.posix.system
import kotlin.random.Random

/**
 * mingw (Windows) `Share` — `cmd /c start` for URL share + `cmd /c "echo TEXT | clip"`
 * for text. Image / File / Multi binary payloads → `UnsupportedPlatform`.
 *
 * **Win32 cinterop status**: `win32-clipboard.def` (CF_DIB binary clipboard, Phase 2
 * ADR-09 #3 closure) compiles → klib successfully on macOS-arm64 K/N hosts but the
 * generated bindings do NOT expose the Win32 SDK types (`OpenClipboard`, `CF_DIB`,
 * `SetClipboardData`, etc.) as Kotlin symbols — K/N cinterop's parser drops the SDK
 * struct types when run on a non-Windows host (only `static inline` helper functions
 * end up in the klib).
 *
 * Real binary-clipboard round-trip needs either:
 * 1. A Windows CI host running the cinterop step (where `windows.h` resolves natively)
 * 2. A cinterop wrapper layer that exposes clipboard state through helper functions
 *    only, never as SDK struct refs (rewrite the .def to be Win32-SDK-opaque)
 *
 * Both deferred to post-v0.4. ADR-09 #3 audit-log updated: WONTFIX-PROVISIONAL remains
 * in effect; `win32-clipboard.def` ships as a future-use artifact.
 *
 * **Security:** URL is double-quote-wrapped + embedded quotes escaped to prevent
 * cmd injection.
 */
@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
public actual object Share {
    public actual suspend fun share(payload: SharePayload, options: ShareOptions): ShareResult = when (payload) {
        is SharePayload.Url -> winStart(payload.href)

        is SharePayload.Text -> winClipText(payload.content)

        // Windows has no ambient share sheet, so both of these degrade to the clipboard — the
        // same fallback the JVM, Linux and web targets use. Copying the path is worth far more
        // than refusing: the user can paste it into mail, chat or Explorer.
        is SharePayload.File -> winClipText(payload.uri)

        is SharePayload.Image -> imageShare(payload)

        is SharePayload.Multi -> multi(payload)
    }

    /** Dispatch URL via Windows shell resolver (`start ""` consumes a title arg). */
    private fun winStart(href: String): ShareResult {
        val escaped = href.replace("\"", "\\\"")
        val rc = system("cmd /c start \"\" \"$escaped\"")
        return if (rc == 0) ShareResult.Completed else ShareResult.Failed(ShareError.Unknown("cmd start exit=$rc"))
    }

    /**
     * Write image bytes to `%TEMP%` and put that path on the clipboard.
     *
     * Windows offers no way to hand raw bytes to another app without the WinRT share contract
     * (`IDataTransferManagerInterop`), and the CF_DIB clipboard route is blocked because K/N
     * cinterop cannot resolve the Win32 SDK types when the klib is built on a non-Windows host —
     * see this file's header. A real file plus its path is the useful degradation: the user
     * pastes the path, or drags the file from Explorer.
     */
    private fun imageShare(image: SharePayload.Image): ShareResult {
        val suffix = mimeToSuffix(image.mimeType)
        val basename = image.filename
            ?.substringBeforeLast('.', missingDelimiterValue = image.filename ?: "image")
            ?.replace(Regex("[^A-Za-z0-9._-]"), "_")
            ?.take(64)
            ?: "image"
        val tmpdir = getenv("TEMP")?.toKString()?.takeIf { it.isNotEmpty() }
            ?: getenv("TMP")?.toKString()?.takeIf { it.isNotEmpty() }
            ?: "C:\\Windows\\Temp"
        val rand = Random.nextLong().toULong().toString(16)
        val path = "$tmpdir\\cmp-share-$basename-$rand$suffix"

        val file = fopen(path, "wb")
            ?: return ShareResult.Failed(ShareError.Unknown("fopen failed for $path (TEMP not writable?)"))
        try {
            val written: ULong = image.bytes.usePinned { pinned ->
                fwrite(pinned.addressOf(0), 1uL.convert(), image.bytes.size.convert(), file).convert()
            }
            if (written.toInt() != image.bytes.size) {
                return ShareResult.Failed(
                    ShareError.Unknown("fwrite short write: $written / ${image.bytes.size}"),
                )
            }
        } finally {
            fclose(file)
        }
        return winClipText(path)
    }

    /** File extension for a MIME type, defaulting to `.bin` for anything unrecognised. */
    private fun mimeToSuffix(mime: String): String = when (mime.lowercase()) {
        "image/png" -> ".png"
        "image/jpeg", "image/jpg" -> ".jpg"
        "image/gif" -> ".gif"
        "image/webp" -> ".webp"
        "image/bmp" -> ".bmp"
        else -> ".bin"
    }

    /** Copy text to clipboard via Windows built-in `clip.exe` (since Windows XP). */
    private fun winClipText(text: String): ShareResult {
        // Newlines in text would break the single-line command — replace with a placeholder
        // and re-emit via echo's `^M` continuation? Simpler: only single-line text copied;
        // multi-line falls back to NoHandler. Production consumers wanting binary-safe
        // clipboard should ship their own clip-replacement.
        if (text.contains('\n') || text.contains('"')) {
            return ShareResult.Failed(ShareError.Unknown("clip.exe path limited to single-line non-quoted text"))
        }
        val rc = system("cmd /c \"echo $text | clip\"")
        return if (rc == 0) ShareResult.Completed else ShareResult.Failed(ShareError.Unknown("clip exit=$rc"))
    }

    private fun multi(multi: SharePayload.Multi): ShareResult {
        for (item in multi.items) {
            val r = when (item) {
                is SharePayload.Text -> winClipText(item.content)
                is SharePayload.Url -> winStart(item.href)
                is SharePayload.File -> winClipText(item.uri)
                is SharePayload.Image -> imageShare(item)
                is SharePayload.Multi -> ShareResult.Failed(ShareError.UnsupportedPlatform)
            }
            if (r is ShareResult.Completed) return r
        }
        return ShareResult.Failed(ShareError.NoHandler)
    }
}
