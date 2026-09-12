/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
package com.mobilebytelabs.kmptoolkit.pdfgenerator

import kotlinx.coroutines.CancellationException
import kotlin.time.Duration

/**
 * Sealed hierarchy of PDF generation failures.
 *
 * Use with exhaustive `when`:
 * ```
 * when (val r = generator.generate(doc, output)) {
 *     is PdfResult.Success -> publish(r.bytes)
 *     is PdfResult.Failure -> when (val e = r.error) {
 *         is PdfError.PermissionDenied -> requestPermission()
 *         else -> showError(e.message)
 *     }
 * }
 * ```
 */
public sealed class PdfError(message: String, cause: Throwable? = null) : Throwable(message, cause) {
    /** Underlying engine (WebView, PDFBox, pdf-lib, …) failed. */
    public class EngineFailure(cause: Throwable) :
        PdfError("PDF engine failed: ${cause.message ?: cause::class.simpleName}", cause)

    /** Coroutine was cancelled mid-render. */
    public object CancellationError : PdfError("PDF generation cancelled")

    /** Host-app permission denied (e.g. Android FileProvider authority missing). */
    public class PermissionDenied(message: String) : PdfError(message)

    /** File I/O failure during write / read. */
    public class IoError(cause: Throwable) :
        PdfError("PDF I/O failed: ${cause.message ?: cause::class.simpleName}", cause)

    /** Requested feature exists in the API but is not supported by this platform's engine. */
    public class UnsupportedFeature(public val feature: String) :
        PdfError("Unsupported feature on this platform: $feature")

    /** Input failed validation (negative margin, empty doc, malformed HTML, …). */
    public class InvalidInput(public val reason: String) : PdfError("Invalid input: $reason")

    /**
     * The engine did not finish within [PdfGeneratorOptions.renderTimeout].
     *
     * Distinct from [EngineFailure]: the engine did not report a problem, it simply never called
     * back. The WebView-backed routes complete through a delegate/load callback that can fail to
     * fire at all (no window context, a silently failed navigation, a detached iframe), so this is
     * the difference between a caller getting a typed failure and its coroutine hanging forever.
     */
    public class RenderTimeout(public val timeout: Duration) :
        PdfError("PDF render did not complete within $timeout")
}

/**
 * Wrap an arbitrary throwable as a [PdfError]. Cancellation is preserved.
 */
public fun Throwable.toPdfError(): PdfError = when (this) {
    is PdfError -> this
    is CancellationException -> PdfError.CancellationError
    else -> PdfError.EngineFailure(this)
}
