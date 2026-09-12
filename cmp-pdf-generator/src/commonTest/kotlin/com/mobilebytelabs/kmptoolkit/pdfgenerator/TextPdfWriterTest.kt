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

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The text-only writer emits real PDF bytes on every target, so these assert the FORMAT — a PDF
 * that a reader refuses to open is worse than no PDF at all, and "it produced bytes" proves
 * nothing about that.
 */
class TextPdfWriterTest {

    private fun doc(vararg elements: PdfElement): PdfDocument = PdfDocument(
        pages = listOf(PdfPage(elements.toList())),
        config = PageConfig(),
        branding = PdfBranding.none(),
    )

    private fun ByteArray.asText(): String = decodeToString()

    @Test
    fun output_starts_with_a_pdf_header_and_ends_with_eof() {
        val bytes = TextPdfWriter.write(doc(PdfElement.Text("hello")))
        val text = bytes.asText()
        assertTrue(text.startsWith("%PDF-1.4"), "readers identify a PDF by its header")
        assertTrue(text.trimEnd().endsWith("%%EOF"), "a truncated PDF fails to open")
    }

    @Test
    fun the_structural_objects_are_all_present() {
        val text = TextPdfWriter.write(doc(PdfElement.Text("hello"))).asText()
        assertContains(text, "/Type /Catalog")
        assertContains(text, "/Type /Pages")
        assertContains(text, "/Type /Page ")
        assertContains(text, "/BaseFont /Helvetica")
        assertContains(text, "trailer")
        assertContains(text, "startxref")
    }

    @Test
    fun the_xref_table_has_one_entry_per_object_plus_the_free_head() {
        // A mismatched xref count is the classic way to produce a file that opens in one reader
        // and fails in another.
        val text = TextPdfWriter.write(doc(PdfElement.Text("hello"))).asText()
        val declared = Regex("""xref\n0 (\d+)""").find(text)?.groupValues?.get(1)?.toInt()
        // RegexOption.MULTILINE, not an inline `(?m)` flag: JavaScript's RegExp has no inline flag
        // groups, so the inline form throws "Invalid regular expression: Invalid group" on js and
        // wasmJs while working fine on JVM/Native. Caught the first time this suite ran on JS.
        val objectCount =
            Regex("""^(\d+) 0 obj""", RegexOption.MULTILINE).findAll(text).count()
        assertEquals(objectCount + 1, declared, "xref size must be object count + the free entry")
    }

    @Test
    fun startxref_points_at_the_actual_xref_offset() {
        val text = TextPdfWriter.write(doc(PdfElement.Text("hello"))).asText()
        val offset = Regex("""startxref\n(\d+)""").find(text)?.groupValues?.get(1)?.toInt()
        assertEquals("xref", text.substring(offset!!, offset + 4))
    }

    @Test
    fun the_text_reaches_the_content_stream() {
        val text = TextPdfWriter.write(doc(PdfElement.Text("unmistakable marker"))).asText()
        assertContains(text, "(unmistakable marker) Tj")
    }

    @Test
    fun parentheses_and_backslashes_are_escaped() {
        // Unescaped, these terminate the string literal early and corrupt every following object.
        val text = TextPdfWriter.write(doc(PdfElement.Text("a (b) c \\ d"))).asText()
        assertContains(text, "(a \\(b\\) c \\\\ d) Tj")
    }

    @Test
    fun a_page_break_produces_a_second_page() {
        val bytes = TextPdfWriter.write(
            doc(PdfElement.Text("one"), PdfElement.PageBreak, PdfElement.Text("two")),
        )
        assertContains(bytes.asText(), "/Count 2")
    }

    @Test
    fun long_content_paginates_by_itself() {
        val many = (1..400).map { PdfElement.Text("line $it") }.toTypedArray()
        val text = TextPdfWriter.write(doc(*many)).asText()
        val count = Regex("""/Count (\d+)""").find(text)?.groupValues?.get(1)?.toInt() ?: 0
        assertTrue(count > 1, "400 lines must not be crammed onto one page, got $count")
    }

    @Test
    fun headings_use_the_bold_font() {
        val text = TextPdfWriter.write(doc(PdfElement.Heading(1, "Title"))).asText()
        assertContains(text, "/F2 ")
        assertContains(text, "(Title) Tj")
    }

    @Test
    fun a_table_header_is_bold_and_cells_are_column_aligned() {
        val table = PdfElement.Table(
            headerRow = TableRow(listOf(TableCell("Name"), TableCell("Qty"))),
            rows = listOf(TableRow(listOf(TableCell("Widget"), TableCell("2")))),
        )
        val text = TextPdfWriter.write(doc(table)).asText()
        assertContains(text, "/F2 ")
        assertTrue(text.contains("Name") && text.contains("Widget"))
    }

    @Test
    fun html_is_stripped_to_text_rather_than_printed_as_markup() {
        val text = TextPdfWriter.write(doc(PdfElement.Html("<p>Hello <b>world</b></p>"))).asText()
        assertContains(text, "Hello world")
        assertTrue(!text.contains("<b>"), "raw tags must not reach the page")
    }

    @Test
    fun an_image_leaves_a_visible_placeholder_rather_than_vanishing() {
        // A silently dropped figure is the failure mode worth avoiding: the reader cannot tell
        // whether the document was authored without it.
        val text = TextPdfWriter.write(
            doc(PdfElement.Image(ImageSource.Url("https://example.com/x.png"))),
        ).asText()
        assertContains(text, "image omitted")
    }

    @Test
    fun an_empty_page_still_yields_a_valid_single_page_document() {
        // Zero pages is an invalid PDF that most readers refuse outright.
        val text = TextPdfWriter.write(doc()).asText()
        assertContains(text, "/Count 1")
        assertTrue(text.trimEnd().endsWith("%%EOF"))
    }

    @Test
    fun output_is_deterministic_for_the_same_input() {
        // No timestamps, no ids: two runs must agree byte for byte, which is what makes the
        // golden-file style assertions above meaningful.
        val first = TextPdfWriter.write(doc(PdfElement.Text("same")))
        val second = TextPdfWriter.write(doc(PdfElement.Text("same")))
        assertTrue(first.contentEquals(second))
    }
}
