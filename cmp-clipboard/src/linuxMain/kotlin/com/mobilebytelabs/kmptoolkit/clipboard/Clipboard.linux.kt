package com.mobilebytelabs.kmptoolkit.clipboard

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.toKString
import platform.posix.FILE
import platform.posix.SIGPIPE
import platform.posix.SIG_IGN
import platform.posix.fgets
import platform.posix.fputs
import platform.posix.pclose
import platform.posix.popen
import platform.posix.signal

/**
 * Linux implementation of clipboard operations using xclip/xsel.
 *
 * This implementation uses external clipboard tools that are commonly
 * available on Linux systems:
 * - Primary: xclip (preferred)
 * - Fallback: xsel
 *
 * ## Requirements
 *
 * At least one of these tools must be installed:
 * ```bash
 * # Ubuntu/Debian
 * sudo apt install xclip
 * # or
 * sudo apt install xsel
 *
 * # Fedora/RHEL
 * sudo dnf install xclip
 * # or
 * sudo dnf install xsel
 *
 * # Arch Linux
 * sudo pacman -S xclip
 * # or
 * sudo pacman -S xsel
 * ```
 *
 * ## Wayland Support
 *
 * For Wayland, consider installing wl-clipboard:
 * ```bash
 * sudo apt install wl-clipboard
 * ```
 *
 * Note: Current implementation only supports X11 clipboard via xclip/xsel.
 */

@OptIn(ExperimentalForeignApi::class)
actual fun copyToClipboard(text: String): Boolean = writeViaProcess("xclip -selection clipboard", text) ||
    writeViaProcess("xsel --clipboard --input", text)

/**
 * Pipe [text] into [command], reporting whether it actually succeeded.
 *
 * ## Why this is not just `popen(...) != null`
 * `popen` forks a SHELL, so it returns a valid handle even when the command does not exist — the
 * shell only then fails with `sh: 1: xclip: not found`. The previous code read a non-null handle
 * as success, wrote to the now-dead pipe, and returned `true`.
 *
 * Writing to a pipe with no reader raises `SIGPIPE`, whose default disposition **terminates the
 * process**. So on a Linux box with neither `xclip` nor `xsel` installed, copying to the clipboard
 * killed the calling application outright — while reporting success. CI surfaced it the first time
 * the Linux native suite ever ran, as "Test running process exited unexpectedly".
 *
 * Ignoring `SIGPIPE` turns that into an ordinary `EPIPE` write failure, and `pclose`'s exit status
 * (127 when the shell cannot find the command) gives the honest answer.
 */
@OptIn(ExperimentalForeignApi::class)
private fun writeViaProcess(command: String, text: String): Boolean {
    signal(SIGPIPE, SIG_IGN)
    val process = popen(command, "w") ?: return false
    fputs(text, process)
    return pclose(process) == 0
}

@OptIn(ExperimentalForeignApi::class)
actual fun getFromClipboard(): String? {
    memScoped {
        // Try xclip first
        var process = popen("xclip -selection clipboard -o 2>/dev/null", "r")
        if (process != null) {
            val result = readFromProcess(process)
            pclose(process)
            if (result.isNotEmpty()) return result
        }

        // Fallback to xsel
        process = popen("xsel --clipboard --output 2>/dev/null", "r")
        if (process != null) {
            val result = readFromProcess(process)
            pclose(process)
            if (result.isNotEmpty()) return result
        }
    }
    return null
}

@OptIn(ExperimentalForeignApi::class)
private fun readFromProcess(process: kotlinx.cinterop.CPointer<FILE>): String {
    memScoped {
        val buffer = allocArray<kotlinx.cinterop.ByteVar>(4096)
        val result = StringBuilder()
        while (fgets(buffer, 4096, process) != null) {
            result.append(buffer.toKString())
        }
        return result.toString().trimEnd('\n')
    }
}

actual fun hasClipboardText(): Boolean {
    val text = getFromClipboard()
    return text != null && text.isNotEmpty()
}

actual fun clearClipboard() {
    copyToClipboard("")
}
