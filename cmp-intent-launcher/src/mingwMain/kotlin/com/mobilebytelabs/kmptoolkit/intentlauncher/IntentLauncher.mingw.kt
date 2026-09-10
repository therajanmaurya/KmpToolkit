/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
package com.mobilebytelabs.kmptoolkit.intentlauncher

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.toKString
import platform.posix._pclose
import platform.posix._popen
import platform.posix.fgets
import platform.posix.system

/**
 * mingw (Windows) `IntentLauncher` — real file pickers plus arbitrary URL dispatch.
 *
 * Pickers run through PowerShell's `System.Windows.Forms.OpenFileDialog` over `_popen`, the same
 * subprocess-dialog approach the Linux actual uses with `zenity` and this module's
 * [SystemIntents.createDocument] uses for saving. PowerShell ships with Windows, so it needs no
 * install and, unlike the cinterop route, no Windows build host.
 *
 * That cinterop route stayed blocked indefinitely: `win32-pickers.def` compiles to a klib on a
 * macOS K/N host, but the generated bindings never expose `OPENFILENAMEW` / `GetOpenFileNameW` —
 * K/N's cinterop parser drops Win32 SDK struct types when `windows.h` is not resolving natively.
 * Waiting for a Windows CI host meant shipping `UnsupportedPlatform` in the meantime, which is
 * exactly the outcome worth avoiding when a working mechanism is already at hand.
 *
 * `PickContact` remains unsupported — Windows has no ambient contact-picker surface to shell out
 * to, so there is nothing to degrade to.
 */
@OptIn(ExperimentalForeignApi::class)
public actual class IntentLauncher public constructor() {
    public actual suspend fun launch(block: IntentBuilder.() -> Unit): IntentResult {
        val builder = IntentBuilder().apply(block)
        return when (val contract = builder.resultContract) {
            ResultContracts.PickImage -> psOpenDialog(IMAGE_FILTER, multiple = false)

            ResultContracts.PickMultipleImages -> psOpenDialog(IMAGE_FILTER, multiple = true)

            ResultContracts.PickDocument -> psOpenDialog(filterFor(builder.type), multiple = false)

            // No ambient contact picker on Windows — nothing to shell out to.
            ResultContracts.PickContact -> IntentResult.Failed(IntentError.UnsupportedPlatform)

            null -> arbitraryUrl(builder)

            else -> builder.onUnsupportedHandler?.invoke()
                ?: IntentResult.Failed(IntentError.UnsupportedPlatform)
        }
    }

    /**
     * Dispatch arbitrary http/https/mailto/file URI via the Windows shell's URI handler
     * registry. `start ""` requires the empty title argument to avoid `start` interpreting
     * the URI as a window-title.
     */
    private fun arbitraryUrl(builder: IntentBuilder): IntentResult {
        val uri = builder.data ?: return IntentResult.Failed(IntentError.NoHandler)
        val escaped = uri.replace("\"", "\\\"")
        val rc = system("cmd /c start \"\" \"$escaped\"")
        return if (rc == 0) {
            IntentResult.Ok(IntentData(uri = uri, mimeType = builder.type))
        } else {
            IntentResult.Failed(IntentError.Unknown("cmd start exit=$rc"))
        }
    }

    /**
     * Show a Windows open-file dialog via PowerShell and return the chosen path(s).
     *
     * `$` is escaped throughout — these are PowerShell variables, not Kotlin templates. `-STA`
     * is required: WinForms dialogs refuse to run on a multi-threaded apartment.
     */
    private fun psOpenDialog(filter: String, multiple: Boolean): IntentResult {
        val script = buildString {
            append("Add-Type -AssemblyName System.Windows.Forms; ")
            append("\$d = New-Object System.Windows.Forms.OpenFileDialog; ")
            append("\$d.Filter = '").append(filter).append("'; ")
            append("\$d.Multiselect = \$").append(if (multiple) "true" else "false").append("; ")
            append("if (\$d.ShowDialog() -eq 'OK') { \$d.FileNames -join '|' }")
        }
        val cmd = "powershell -NoProfile -NonInteractive -STA -Command \"$script\" 2>NUL"

        val pipe = _popen(cmd, "r") ?: return IntentResult.Failed(IntentError.Unknown("_popen failed"))
        val line = memScoped {
            val buf = allocArray<kotlinx.cinterop.ByteVar>(8192)
            fgets(buf, 8192, pipe)?.toKString()?.trim()
        }
        _pclose(pipe)

        if (line.isNullOrEmpty()) return IntentResult.Cancelled

        val uris = line.split('|').filter { it.isNotBlank() }.map { "file:///" + it.replace('\\', '/') }
        return if (multiple) {
            IntentResult.Ok(IntentData(uri = uris.firstOrNull(), extras = mapOf("uris" to uris)))
        } else {
            IntentResult.Ok(IntentData(uri = uris.firstOrNull()))
        }
    }

    /** OpenFileDialog filter for a MIME type, defaulting to "All files". */
    private fun filterFor(mimeType: String?): String = when (mimeType?.lowercase()) {
        null, "*/*" -> "All files (*.*)|*.*"
        "application/pdf" -> "PDF document (*.pdf)|*.pdf|All files (*.*)|*.*"
        "text/plain" -> "Text file (*.txt)|*.txt|All files (*.*)|*.*"
        "text/csv" -> "CSV file (*.csv)|*.csv|All files (*.*)|*.*"
        "application/json" -> "JSON file (*.json)|*.json|All files (*.*)|*.*"
        else -> if (mimeType.startsWith("image/")) IMAGE_FILTER else "All files (*.*)|*.*"
    }

    private companion object {
        const val IMAGE_FILTER: String =
            "Image files (*.png;*.jpg;*.jpeg;*.gif;*.bmp;*.webp)|*.png;*.jpg;*.jpeg;*.gif;*.bmp;*.webp|" +
                "All files (*.*)|*.*"
    }
}
