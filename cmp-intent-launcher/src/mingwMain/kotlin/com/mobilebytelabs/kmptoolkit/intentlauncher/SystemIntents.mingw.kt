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

package com.mobilebytelabs.kmptoolkit.intentlauncher

import kotlinx.cinterop.allocArray
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.toKString
import platform.posix._pclose
import platform.posix._popen
import platform.posix.fgets
import platform.posix.system

/**
 * mingw (Windows) `SystemIntents` actual.
 *
 * - `openAppSettings()` — `start ms-settings:appsfeatures` via `system()` (canonical
 *   Windows-shell URI dispatch; works under MSYS / cmd alike).
 * - `createDocument()` — a real Windows save dialog, driven through PowerShell's
 *   `System.Windows.Forms.SaveFileDialog` over `popen`. This mirrors how the Linux actual
 *   shells out to `zenity`: a subprocess dialog is the established pattern in this module, and
 *   it needs no cinterop at all. The `GetSaveFileNameW` binding stayed blocked because K/N
 *   cinterop cannot resolve Win32 SDK types when the klib is built on a non-Windows host, so
 *   waiting for it meant shipping `UnsupportedPlatform` indefinitely.
 */
@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
public actual object SystemIntents {

    public actual suspend fun openAppSettings(): IntentResult {
        val rc = system("start ms-settings:appsfeatures")
        return if (rc == 0) {
            IntentResult.Ok(IntentData(uri = "ms-settings:appsfeatures"))
        } else {
            IntentResult.Failed(IntentError.Unknown("system() exit=$rc"))
        }
    }

    public actual suspend fun createDocument(suggestedName: String, mimeType: String): IntentResult {
        // PowerShell ships on every supported Windows version, so this needs no extra install —
        // the Linux equivalent's dependency on zenity is the weaker assumption of the two.
        val filter = filterFor(mimeType)
        // `$` is escaped throughout: these are PowerShell variables, not Kotlin templates.
        val script = buildString {
            append("Add-Type -AssemblyName System.Windows.Forms; ")
            append("\$d = New-Object System.Windows.Forms.SaveFileDialog; ")
            append("\$d.FileName = '").append(suggestedName.psEscape()).append("'; ")
            append("\$d.Filter = '").append(filter).append("'; ")
            append("\$d.OverwritePrompt = \$true; ")
            // ShowDialog() would otherwise print OK/Cancel to stdout alongside the path.
            append("if (\$d.ShowDialog() -eq 'OK') { Write-Output \$d.FileName }")
        }
        val cmd = "powershell -NoProfile -NonInteractive -STA -Command \"$script\" 2>NUL"

        val pipe = _popen(cmd, "r") ?: return IntentResult.Failed(IntentError.Unknown("popen failed"))
        val picked = memScoped {
            val buf = allocArray<kotlinx.cinterop.ByteVar>(4096)
            fgets(buf, 4096, pipe)?.toKString()?.trim()
        }
        _pclose(pipe)

        // Same limitation the Linux actual documents: the packed wait status cannot cleanly
        // separate "user cancelled" from "powershell missing", and both yield empty stdout.
        // Cancelled is the graceful reading — no dialog, no error.
        return if (picked.isNullOrEmpty()) {
            IntentResult.Cancelled
        } else {
            IntentResult.Ok(IntentData(uri = "file:///" + picked.replace('\\', '/'), mimeType = mimeType))
        }
    }

    /** SaveFileDialog filter string for [mimeType], falling back to "All files". */
    private fun filterFor(mimeType: String): String = when (mimeType.lowercase()) {
        "application/pdf" -> "PDF document (*.pdf)|*.pdf|All files (*.*)|*.*"
        "image/png" -> "PNG image (*.png)|*.png|All files (*.*)|*.*"
        "image/jpeg", "image/jpg" -> "JPEG image (*.jpg)|*.jpg;*.jpeg|All files (*.*)|*.*"
        "text/plain" -> "Text file (*.txt)|*.txt|All files (*.*)|*.*"
        "text/csv" -> "CSV file (*.csv)|*.csv|All files (*.*)|*.*"
        "application/json" -> "JSON file (*.json)|*.json|All files (*.*)|*.*"
        "application/zip" -> "ZIP archive (*.zip)|*.zip|All files (*.*)|*.*"
        else -> "All files (*.*)|*.*"
    }

    /**
     * Escape for a single-quoted PowerShell literal, which only treats `'` specially — and strip
     * the quote and control characters that would let a filename break out of the `cmd` string.
     */
    private fun String.psEscape(): String = this
        .replace("'", "''")
        .filterNot { it == '"' || it == '\n' || it == '\r' || it == '&' || it == '|' || it == '<' || it == '>' }
}
