/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
package com.mobilebytelabs.kmptoolkit.clipboard.testing

import com.mobilebytelabs.kmptoolkit.clipboard.Clipboard
import com.mobilebytelabs.kmptoolkit.clipboard.ClipboardCapabilities

/**
 * In-memory [Clipboard] for tests — shipped in the main artifact.
 *
 * The real clipboard is shared mutable state owned by the machine: a JVM CI box may have none at
 * all, an iOS simulator accepts a write and reads back `null`, and a browser gates reads behind a
 * permission prompt. A test that asserts "we copied the referral code" must not depend on any of
 * that.
 *
 * ```kotlin
 * val clipboard = FakeClipboard()
 * ReferralViewModel(clipboard).copyCode("ABC123")
 * assertEquals("ABC123", clipboard.paste())
 * assertEquals(listOf("ABC123"), clipboard.copied)
 * ```
 *
 * Simulate a platform that refuses writes, and assert the caller notices:
 *
 * ```kotlin
 * val readOnly = FakeClipboard(writable = false)
 * ReferralViewModel(readOnly).copyCode("ABC123")
 * assertNull(readOnly.paste())            // and no "Copied" toast was shown
 * ```
 */
public class FakeClipboard(
    initial: String? = null,
    /** When `false`, every write is refused — models a platform or permission denial. */
    private val writable: Boolean = true,
    override val capabilities: ClipboardCapabilities = ClipboardCapabilities.System,
) : Clipboard {

    private var content: String? = initial

    /** Every successful copy, in order. */
    public val copied: MutableList<String> = mutableListOf()

    /** Every write that was refused because [writable] is `false`. */
    public val refused: MutableList<String> = mutableListOf()

    /** How many times the clipboard was read. */
    public var pasteCalls: Int = 0
        private set

    override fun copy(text: String): Boolean {
        if (!writable) {
            refused += text
            return false
        }
        content = text
        copied += text
        return true
    }

    override fun paste(): String? {
        pasteCalls++
        return content
    }

    override fun hasText(): Boolean = !content.isNullOrEmpty()

    override fun clear() {
        content = null
    }

    // The async forms share the sync state deliberately: a test should not have to care which the
    // code under test happened to call.
    override suspend fun copyAsync(text: String): Boolean = copy(text)

    override suspend fun pasteAsync(): String? = paste()

    /** Forget the contents and every recorded call. */
    public fun reset() {
        content = null
        copied.clear()
        refused.clear()
        pasteCalls = 0
    }
}
