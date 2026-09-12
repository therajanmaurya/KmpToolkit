package com.mobilebytelabs.kmptoolkit.clipboard

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.set
import kotlinx.cinterop.toKString
import kotlinx.cinterop.usePinned
import platform.windows.CF_TEXT
import platform.windows.CloseClipboard
import platform.windows.EmptyClipboard
import platform.windows.GMEM_MOVEABLE
import platform.windows.GetClipboardData
import platform.windows.GlobalAlloc
import platform.windows.GlobalLock
import platform.windows.GlobalUnlock
import platform.windows.IsClipboardFormatAvailable
import platform.windows.OpenClipboard
import platform.windows.SetClipboardData
import platform.windows.Sleep

/*
 * Windows implementation of clipboard operations using the Win32 API.
 *
 * ## Thread safety — read this before "simplifying" the retry
 *
 * An earlier revision of this file claimed "Windows clipboard operations are thread-safe — the system
 * handles concurrent access through OpenClipboard/CloseClipboard." That is **wrong**, and it is why
 * `ClipboardManagerTest.pauseResume_doesNotThrow[mingwX64]` died intermittently on CI (2026-09-12,
 * run 34698039536 — the same test had passed on the two runs before it).
 *
 * The clipboard is a single system-wide resource owned by ONE thread at a time. `OpenClipboard`
 * FAILS — it does not block or queue — when any other thread or process currently holds it, and on a
 * busy machine that happens routinely. `MingwClipboardMonitor` makes the window wide: `start()` reads
 * the clipboard on the caller's thread and its polling job then reads it again from
 * `Dispatchers.Default`, so two of this library's own threads contend.
 *
 * Two further defects made the failure a process kill rather than a returned `null`:
 *  - `GlobalUnlock(hData)` ran even when `GlobalLock` had returned null.
 *  - `catch (e: Exception)` cannot catch a Win32 access violation. An AV is not a Kotlin `Throwable`,
 *    so the handler gave false confidence while the process died with no stack — which is exactly how
 *    it surfaced: a test result of "Unknown" with no assertion message.
 *
 * Every entry point below therefore goes through [withClipboard], which retries the open a bounded
 * number of times and guarantees the close. Contention now degrades to a `false`/`null` return, which
 * is what every caller already handles.
 */

/** Attempts for [OpenClipboard]. Contention is brief, so a handful of short retries clears it. */
private const val OPEN_ATTEMPTS = 10

/** Delay between attempts, ms. 10 × 10ms bounds a failed acquisition at ~100ms. */
private const val OPEN_RETRY_DELAY_MS = 10u

/**
 * Run [block] with the clipboard open, or return null if it could not be acquired.
 *
 * `CloseClipboard` is in a `finally` so an early return inside [block] cannot leak the lock — leaking
 * it would wedge the clipboard for every other process on the machine, not just this one.
 */
@OptIn(ExperimentalForeignApi::class)
private inline fun <T : Any> withClipboard(block: () -> T?): T? {
    var attempt = 0
    while (OpenClipboard(null) == 0) {
        attempt++
        if (attempt >= OPEN_ATTEMPTS) return null
        Sleep(OPEN_RETRY_DELAY_MS)
    }
    return try {
        block()
    } finally {
        CloseClipboard()
    }
}

@OptIn(ExperimentalForeignApi::class)
actual fun copyToClipboard(text: String): Boolean = withClipboard {
    EmptyClipboard()

    val bytes = text.encodeToByteArray()
    val hMem = GlobalAlloc(GMEM_MOVEABLE.toUInt(), (bytes.size + 1).toULong()) ?: return@withClipboard null

    val locked = GlobalLock(hMem) ?: return@withClipboard null
    bytes.usePinned { pinned ->
        val dest = locked.reinterpret<ByteVar>()
        for (i in bytes.indices) {
            dest[i] = pinned.get()[i]
        }
        dest[bytes.size] = 0 // CF_TEXT is NUL-terminated ANSI.
    }
    // Balanced with the GlobalLock above — only unlock what was actually locked.
    GlobalUnlock(hMem)

    // Ownership of hMem passes to the system on success; do NOT free it here.
    if (SetClipboardData(CF_TEXT.toUInt(), hMem) == null) null else true
} ?: false

@OptIn(ExperimentalForeignApi::class)
actual fun getFromClipboard(): String? = withClipboard {
    // Checked while holding the clipboard: between an unlocked check and the read, another process
    // can replace the contents, and GetClipboardData would then hand back a handle for a format that
    // is no longer CF_TEXT.
    if (IsClipboardFormatAvailable(CF_TEXT.toUInt()) == 0) return@withClipboard null

    val hData = GetClipboardData(CF_TEXT.toUInt()) ?: return@withClipboard null
    val locked = GlobalLock(hData) ?: return@withClipboard null
    try {
        locked.reinterpret<ByteVar>().toKString()
    } finally {
        GlobalUnlock(hData)
    }
}

@OptIn(ExperimentalForeignApi::class)
actual fun hasClipboardText(): Boolean = IsClipboardFormatAvailable(CF_TEXT.toUInt()) != 0

@OptIn(ExperimentalForeignApi::class)
actual fun clearClipboard() {
    withClipboard {
        EmptyClipboard()
        true
    }
}
