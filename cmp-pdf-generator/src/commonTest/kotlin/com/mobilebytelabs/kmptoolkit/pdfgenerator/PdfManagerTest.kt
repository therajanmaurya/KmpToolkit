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

import com.mobilebytelabs.kmptoolkit.pdfgenerator.testing.FakePdfManager
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Contract for the injectable facade and its test double. */
@OptIn(ExperimentalPdfGeneratorApi::class)
class PdfManagerTest {

    private fun doc() = PdfDocument(
        pages = listOf(PdfPage(listOf(PdfElement.Text("hello")))),
        config = PageConfig(),
        branding = PdfBranding.none(),
    )

    @Test
    fun generate_records_the_document_and_output() = runTest {
        val pdf = FakePdfManager()
        pdf.generate(doc(), PdfOutput.Share, fileName = "invoice.pdf")

        val request = pdf.generated.single()
        assertNotNull(request.document)
        assertNull(request.html)
        assertEquals(PdfOutput.Share, request.output)
        assertEquals("invoice.pdf", request.fileName)
    }

    @Test
    fun generateFromHtml_records_the_markup_instead_of_a_document() = runTest {
        val pdf = FakePdfManager()
        pdf.generateFromHtml("<p>hi</p>", PdfOutput.ByteArrayOutput)

        val request = pdf.generated.single()
        assertNull(request.document)
        assertEquals("<p>hi</p>", request.html)
    }

    @Test
    fun toBytes_unwraps_a_successful_result() = runTest {
        val pdf = FakePdfManager(result = PdfResult.Success(bytes = byteArrayOf(1, 2, 3), byteCount = 3))
        val bytes = assertNotNull(pdf.toBytes(doc()))
        assertEquals(3, bytes.size)
        // The convenience must request bytes, not a share sheet.
        assertEquals(PdfOutput.ByteArrayOutput, pdf.generated.single().output)
    }

    @Test
    fun toBytes_is_null_on_failure_rather_than_throwing() = runTest {
        val pdf = FakePdfManager(result = PdfResult.Failure(PdfError.InvalidInput("no rows")))
        assertNull(pdf.toBytes(doc()))
    }

    @Test
    fun a_scripted_failure_reaches_the_caller() = runTest {
        val pdf = FakePdfManager(result = PdfResult.Failure(PdfError.InvalidInput("no rows")))
        val failure = assertIs<PdfResult.Failure>(pdf.generate(doc(), PdfOutput.Share))
        assertIs<PdfError.InvalidInput>(failure.error)
    }

    @Test
    fun the_text_only_targets_can_be_simulated() {
        // So a test can prove the UI hides an HTML-templated export where styling would vanish.
        assertTrue(FakePdfManager().supportsHtmlLayout)
        assertEquals(false, FakePdfManager(supportsHtmlLayout = false).supportsHtmlLayout)
    }

    @Test
    fun reset_clears_the_ledger() = runTest {
        val pdf = FakePdfManager()
        pdf.generate(doc(), PdfOutput.Share)
        pdf.reset()
        assertTrue(pdf.generated.isEmpty())
    }

    @Test
    fun the_real_impl_agrees_with_the_platform_about_html_layout() {
        assertEquals(platformSupportsHtmlLayout, PdfManagerImpl().supportsHtmlLayout)
    }

    @Test
    fun the_real_impl_generates_bytes_on_every_target() = runTest {
        // ByteArrayOutput is the one output every target honours with no host wiring — the
        // "generate and upload" case. If this regresses, the fallback targets are silently broken.
        val bytes = assertNotNull(PdfManagerImpl().toBytes(doc()))
        assertTrue(bytes.isNotEmpty())
        assertTrue(bytes.decodeToString().startsWith("%PDF-"))
    }
}
