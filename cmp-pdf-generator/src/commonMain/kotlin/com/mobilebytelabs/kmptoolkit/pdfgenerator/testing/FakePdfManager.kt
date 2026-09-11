/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
package com.mobilebytelabs.kmptoolkit.pdfgenerator.testing

import com.mobilebytelabs.kmptoolkit.pdfgenerator.ExperimentalPdfGeneratorApi
import com.mobilebytelabs.kmptoolkit.pdfgenerator.PageConfig
import com.mobilebytelabs.kmptoolkit.pdfgenerator.PdfBranding
import com.mobilebytelabs.kmptoolkit.pdfgenerator.PdfDocument
import com.mobilebytelabs.kmptoolkit.pdfgenerator.PdfGeneratorOptions
import com.mobilebytelabs.kmptoolkit.pdfgenerator.PdfManager
import com.mobilebytelabs.kmptoolkit.pdfgenerator.PdfOutput
import com.mobilebytelabs.kmptoolkit.pdfgenerator.PdfProgressEvent
import com.mobilebytelabs.kmptoolkit.pdfgenerator.PdfResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/** One recorded generation request. */
@ExperimentalPdfGeneratorApi
public data class GeneratedPdf(
    public val document: PdfDocument?,
    public val html: String?,
    public val output: PdfOutput,
    public val fileName: String,
)

/**
 * In-memory [PdfManager] for tests — shipped in the main artifact.
 *
 * Generating a PDF for real means invoking a renderer, and on most targets `PdfOutput.Share`,
 * `Print` and `Save` raise OS UI. A test of an export flow must do neither, so this records the
 * request and returns a scripted result.
 *
 * ```kotlin
 * val pdf = FakePdfManager()
 * InvoiceViewModel(pdf).export(invoice)
 *
 * val request = pdf.generated.single()
 * assertEquals(PdfOutput.Share, request.output)
 * assertTrue(request.fileName.endsWith(".pdf"))
 * ```
 *
 * Drive the failure branch, and the text-only targets:
 *
 * ```kotlin
 * val failing = FakePdfManager(result = PdfResult.Failure(PdfError.InvalidInput("no rows")))
 * val textOnly = FakePdfManager(supportsHtmlLayout = false)   // tvOS / watchOS / Linux / Win / WASI
 * ```
 */
@ExperimentalPdfGeneratorApi
public class FakePdfManager(
    /** Returned by every generate call. Defaults to success with a token byte payload. */
    public var result: PdfResult = PdfResult.Success(bytes = ByteArray(1), byteCount = 1),
    override val supportsHtmlLayout: Boolean = true,
) : PdfManager {

    /** Every generation request, in order. */
    public val generated: MutableList<GeneratedPdf> = mutableListOf()

    override suspend fun generate(
        document: PdfDocument,
        output: PdfOutput,
        options: PdfGeneratorOptions,
        fileName: String,
    ): PdfResult {
        generated += GeneratedPdf(document, null, output, fileName)
        return result
    }

    override suspend fun generateFromHtml(
        html: String,
        output: PdfOutput,
        pageConfig: PageConfig,
        branding: PdfBranding,
        options: PdfGeneratorOptions,
        fileName: String,
    ): PdfResult {
        generated += GeneratedPdf(null, html, output, fileName)
        return result
    }

    override fun progressFlow(): Flow<PdfProgressEvent> = emptyFlow()

    /** Forget every recorded request. */
    public fun reset() {
        generated.clear()
    }
}
