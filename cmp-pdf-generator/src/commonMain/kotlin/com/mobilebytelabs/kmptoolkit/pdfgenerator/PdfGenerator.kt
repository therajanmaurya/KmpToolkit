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

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.datetime.LocalDate
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Default [PdfGeneratorOptions.renderTimeout]. Single source for the primary constructor and the
 * binary-compatibility shim below — if those two ever disagreed, which one a call resolved to would
 * silently change behaviour.
 */
private val DEFAULT_RENDER_TIMEOUT: Duration = 60.seconds

/**
 * Render-time options.
 *
 * @param deterministic When true, the renderer freezes timestamps + object IDs for byte-stable
 *   output. Use in snapshot tests.
 * @param fixedDate When [deterministic] is true and a generation date appears in the output,
 *   use this date. Null falls back to a fixed epoch.
 * @param dpi DPI for raster image embedding. Default 300.
 * @param compress Compress PDF object streams. Default true.
 * @param renderTimeout Upper bound on how long an engine may take to produce the document before
 *   the call gives up with [PdfError.RenderTimeout].
 *
 *   This exists because the WebView-backed engines (iOS/macOS `WKWebView`, JS/wasmJs `iframe`)
 *   complete via a delegate or load callback that is not guaranteed to ever fire — no window
 *   context, a navigation that silently fails, an `iframe` the host page detaches. Before this
 *   bound those routes awaited a `CompletableDeferred` with no deadline, so such a case hung the
 *   caller's coroutine forever rather than returning the documented typed [PdfResult.Failure].
 *   Found when iOS tests were executed for the first time (see `IosPdfSmokeTest`).
 *
 *   Default 60s: generous enough for a large document on a slow device, short enough that a stuck
 *   render surfaces as an error rather than a hang. Pure-Kotlin routes (`TextPdfWriter`) never
 *   await a callback and so are unaffected by this value.
 */
@ExperimentalPdfGeneratorApi
public data class PdfGeneratorOptions(
    public val deterministic: Boolean = false,
    public val fixedDate: LocalDate? = null,
    public val dpi: Int = 300,
    public val compress: Boolean = true,
    public val renderTimeout: Duration = DEFAULT_RENDER_TIMEOUT,
) {
    init {
        require(dpi in 72..600) { "dpi must be 72..600" }
        require(renderTimeout > Duration.ZERO) { "renderTimeout must be positive, was $renderTimeout" }
    }

    /**
     * Binary-compatibility shim for callers compiled against the four-property version of this class.
     *
     * Adding [renderTimeout] to a data class primary constructor DELETES four JVM symbols — the
     * four-arg `<init>`, `copy`, and the two default-argument synthetics that every
     * `PdfGeneratorOptions(dpi = N)` / `copy(dpi = N)` call site compiles down to. Dropping those
     * would be a binary break (`NoSuchMethodError` for an already-published consumer), and the
     * default-argument synthetics are the COMMON shape for an options class, so restoring only the
     * plain forms would look additive in the `.api` file while still breaking real callers.
     *
     * This declaration plus [copy] below restore all four. Verified against the 3.5.x baseline: the
     * dumped API diff is purely additive, no removals. They carry defaults deliberately — that is
     * what regenerates the `DefaultConstructorMarker` / `copy$default` synthetics — and they are NOT
     * deprecated, because Kotlin resolves an ordinary `PdfGeneratorOptions(dpi = N)` to this
     * overload, so a deprecation would warn on perfectly idiomatic new code.
     */
    public constructor(
        deterministic: Boolean = false,
        fixedDate: LocalDate? = null,
        dpi: Int = 300,
        compress: Boolean = true,
    ) : this(deterministic, fixedDate, dpi, compress, DEFAULT_RENDER_TIMEOUT)

    /**
     * Four-property [copy], restoring the pre-[renderTimeout] `copy` and `copy$default` symbols.
     *
     * Forwards `this.renderTimeout` rather than the default: a `copy(dpi = N)` on an instance with a
     * custom timeout must PRESERVE it. Resetting it to 60s here would be a silent behaviour change
     * for exactly the callers this shim exists to protect.
     */
    public fun copy(
        deterministic: Boolean = this.deterministic,
        fixedDate: LocalDate? = this.fixedDate,
        dpi: Int = this.dpi,
        compress: Boolean = this.compress,
    ): PdfGeneratorOptions = copy(deterministic, fixedDate, dpi, compress, this.renderTimeout)
}

/**
 * Where the rendered PDF should land.
 */
@ExperimentalPdfGeneratorApi
public sealed class PdfOutput {
    /** Write to a specific filesystem path. */
    public data class File(public val path: String) : PdfOutput()

    /** Return bytes in-memory via [PdfResult.Success.bytes]. */
    public object ByteArrayOutput : PdfOutput()

    /** Write to platform-appropriate storage and pass a content URI to [callback]. */
    public data class Uri(public val callback: (String) -> Unit) : PdfOutput()

    /** Launch native share intent (Android `ACTION_SEND`, iOS `UIActivityViewController`, …). */
    public object Share : PdfOutput()

    /** Launch native print dialog. */
    public object Print : PdfOutput()

    /** Launch native save-as dialog (Android SAF, iOS document picker, NSSavePanel, browser download). */
    public object Save : PdfOutput()
}

/**
 * Result of a [PdfGenerator.generate] call.
 */
@ExperimentalPdfGeneratorApi
public sealed class PdfResult {
    /**
     * @param bytes Non-null when [PdfOutput.ByteArrayOutput] was used; null otherwise.
     * @param uri Non-null when [PdfOutput.Uri] was used; null otherwise.
     * @param byteCount Always populated when bytes were available.
     */
    public data class Success(
        public val bytes: ByteArray? = null,
        public val uri: String? = null,
        public val byteCount: Int = 0,
    ) : PdfResult() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Success) return false
            return uri == other.uri && byteCount == other.byteCount &&
                (bytes?.contentEquals(other.bytes) ?: (other.bytes == null))
        }

        override fun hashCode(): Int {
            var r = bytes?.contentHashCode() ?: 0
            r = 31 * r + (uri?.hashCode() ?: 0)
            r = 31 * r + byteCount
            return r
        }
    }

    public data class Failure(public val error: PdfError) : PdfResult()
}

/**
 * Cross-platform PDF generator. Each platform supplies an `actual class`.
 *
 * Two entry points:
 *
 *  - [generateAndSharePdf] — mifos-x back-compatible. HTML in, share intent / file save out.
 *  - [generate] — generic v0.1 entry. Takes a [PdfDocument] (DSL or HTML-via-template), routes
 *    to the requested [PdfOutput].
 *
 * The module declares the targets supported by `kotlinx-html` + Kotlin/Native Tier-1:
 * Android, iOS (3 archs), macOS (2 archs), JVM, JS, wasmJs. Adding more targets
 * requires verifying upstream library compatibility first.
 */
@ExperimentalPdfGeneratorApi
public expect class PdfGenerator() {
    /**
     * mifos-x back-compat — HTML in, share/print/save out. Behavior is platform-defined.
     */
    public suspend fun generateAndSharePdf(htmlContent: String, fileName: String, pageConfig: PageConfig)

    /**
     * Render a [PdfDocument] to the chosen [output].
     *
     * @param fileName Suggested name for the produced file. A `.pdf` extension is added
     *   automatically when missing (see [ensurePdfFileName]). Surfaced by the Share / Save /
     *   Print flows on every platform; ignored on wasmJs where the browser controls the name.
     */
    public suspend fun generate(
        document: PdfDocument,
        output: PdfOutput,
        options: PdfGeneratorOptions = PdfGeneratorOptions(),
        fileName: String = DEFAULT_PDF_FILE_NAME,
    ): PdfResult

    /**
     * Render raw [html] to the chosen [output], with optional branding overrides.
     *
     * @param fileName Suggested name for the produced file. A `.pdf` extension is added
     *   automatically when missing (see [ensurePdfFileName]). Surfaced by the Share / Save /
     *   Print flows on every platform; ignored on wasmJs where the browser controls the name.
     */
    public suspend fun generateFromHtml(
        html: String,
        output: PdfOutput,
        pageConfig: PageConfig = PageConfig(),
        branding: PdfBranding = PdfBranding.none(),
        options: PdfGeneratorOptions = PdfGeneratorOptions(),
        fileName: String = DEFAULT_PDF_FILE_NAME,
    ): PdfResult

    /**
     * Hot flow of render progress events. Default implementation emits nothing.
     * Tier-1 platforms emit Started / PageRendered / Finalizing / Complete.
     */
    public fun progressFlow(): Flow<PdfProgressEvent>
}

/**
 * Default no-op progress flow — referenced by platform impls that don't yet emit events.
 */
@ExperimentalPdfGeneratorApi
internal fun emptyProgressFlow(): Flow<PdfProgressEvent> = emptyFlow()
