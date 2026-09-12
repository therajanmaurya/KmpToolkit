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
 * What clipboard access on this target actually means.
 *
 * ## Why [systemWide] matters
 * Every target can copy and read back — three of them do it through an app-scoped buffer
 * ([InAppClipboard]) because the platform ships no system clipboard at all. The operations
 * succeed either way, so a boolean "supported" flag would be useless. What a caller needs to know
 * is whether the text left the app: it decides whether your toast should say "Copied to
 * clipboard" or just "Copied", and whether "paste into another app" is a thing you can suggest.
 */
public data class ClipboardCapabilities(
    /** Text can be written. True on every target. */
    public val write: Boolean,
    /** Written text can be read back. True on every target, though the web may prompt first. */
    public val read: Boolean,
    /** The clipboard is the OS one, visible to other applications. */
    public val systemWide: Boolean,
    /** Reads may require a user permission prompt — the web's Clipboard Read API. */
    public val readNeedsPermission: Boolean,
) {
    public companion object {
        /** A real OS clipboard with unrestricted reads — Android, iOS, macOS, JVM, Linux, Windows. */
        public val System: ClipboardCapabilities = ClipboardCapabilities(
            write = true,
            read = true,
            systemWide = true,
            readNeedsPermission = false,
        )

        /** A real OS clipboard whose reads are permission-gated — the browser. */
        public val SystemPermissioned: ClipboardCapabilities = ClipboardCapabilities(
            write = true,
            read = true,
            systemWide = true,
            readNeedsPermission = true,
        )

        /** An app-scoped buffer — tvOS, watchOS, wasmWasi. See [InAppClipboard]. */
        public val InApp: ClipboardCapabilities = ClipboardCapabilities(
            write = true,
            read = true,
            systemWide = false,
            readNeedsPermission = false,
        )
    }
}

/**
 * What clipboard access means on THIS target.
 *
 * | Target | kind | note |
 * |---|---|---|
 * | Android, iOS, macOS, JVM, Linux, Windows | system | the real OS clipboard |
 * | JS, wasmJs | system, permissioned | `navigator.clipboard.readText()` may prompt |
 * | tvOS, watchOS, wasmWasi | in-app | no system clipboard exists; see [InAppClipboard] |
 */
public expect val platformClipboardCapabilities: ClipboardCapabilities
