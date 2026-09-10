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

/**
 * A dependency-free, text-only PDF writer, in pure `commonMain`.
 *
 * ## Why this exists
 * Every real renderer in this module is platform-bound: openhtmltopdf on the JVM, `UIGraphics` on
 * iOS, `WebView` on Android, the browser's own print pipeline on the web. None of them exists on
 * tvOS, watchOS, Linux, Windows or WASI, which is why the module shipped on 9 targets while the
 * rest of the toolkit reached 21.
 *
 * But **PDF is a file format, not a platform service**. Nothing about emitting a valid PDF needs
 * an OS: the format is a byte layout, and the 14 standard fonts (Helvetica among them) need no
 * embedding. So the missing targets can generate real, openable PDFs — just not ones laid out by
 * an HTML engine.
 *
 * ## What it does and does not do
 * Produces a valid PDF 1.4 document with text, headings, tables rendered as aligned columns,
 * spacers, dividers and page breaks — paginated, with a standard font.
 *
 * It does **not** do HTML or CSS layout, images, or web fonts. A caller that needs those wants a
 * Tier-1 target. [PdfElement.Image] renders a bracketed placeholder rather than being dropped, so
 * a missing figure is visible in the output instead of silently absent, and [PdfElement.Html] has
 * its tags stripped to text rather than being printed as markup.
 */
public object TextPdfWriter {

    private const val PT_PER_MM = 72.0 / 25.4
    private const val DEFAULT_FONT_SIZE = 10.0
    private const val LINE_SPACING = 1.35

    /** Render [document] to PDF bytes. */
    public fun write(document: PdfDocument): ByteArray {
        val config = document.config
        val pageWidth = config.size.widthMm * PT_PER_MM
        val pageHeight = config.size.heightMm * PT_PER_MM
        val left = config.margins.left * PT_PER_MM
        val right = config.margins.right * PT_PER_MM
        val top = config.margins.top * PT_PER_MM
        val bottom = config.margins.bottom * PT_PER_MM
        val usableWidth = pageWidth - left - right

        val lines = document.pages.flatMapIndexed { index, page ->
            val rendered = page.elements.flatMap { renderElement(it, usableWidth) }
            // An explicit page in the model is an explicit page in the output.
            if (index == document.pages.lastIndex) rendered else rendered + Line.PageBreak
        }

        val pageContents = paginate(lines, pageHeight - top - bottom)
        return buildPdf(pageContents, pageWidth, pageHeight, left, pageHeight - top)
    }

    // ---- element -> lines ---------------------------------------------------------------------

    private sealed class Line {
        data class Text(val text: String, val size: Double, val bold: Boolean) : Line()
        data class Gap(val points: Double) : Line()
        object PageBreak : Line()
    }

    private fun renderElement(element: PdfElement, usableWidth: Double): List<Line> = when (element) {
        is PdfElement.Text -> wrap(element.content, element.style.size.toDouble(), false, usableWidth)

        is PdfElement.Heading -> {
            // h1 largest through h6 smallest, mirroring HTML's own scale.
            val size = (26 - element.level * 3).toDouble()
            listOf(Line.Gap(size * 0.5)) +
                wrap(element.content, size, true, usableWidth) +
                Line.Gap(size * 0.3)
        }

        is PdfElement.Table -> renderTable(element, usableWidth)

        is PdfElement.Spacer -> listOf(Line.Gap(element.mm * PT_PER_MM))

        PdfElement.Divider -> listOf(Line.Gap(4.0), Line.Text("_".repeat(60), 8.0, false), Line.Gap(4.0))

        PdfElement.PageBreak -> listOf(Line.PageBreak)

        // Visible placeholder rather than a silent drop: a missing figure should be obvious.
        is PdfElement.Image -> listOf(Line.Text("[image omitted — text-only renderer]", 8.0, false))

        is PdfElement.Html -> wrap(stripTags(element.raw), DEFAULT_FONT_SIZE, false, usableWidth)
    }

    private fun renderTable(table: PdfElement.Table, usableWidth: Double): List<Line> {
        val allRows = listOfNotNull(table.headerRow) + table.rows
        if (allRows.isEmpty()) return emptyList()

        val columnCount = allRows.maxOf { row -> row.cells.sumOf { it.colSpan } }
        // Monospace-style column padding: without a text-measuring engine, even columns are the
        // only alignment that holds for arbitrary content.
        val charsPerColumn = ((usableWidth / (DEFAULT_FONT_SIZE * 0.5)) / columnCount).toInt()
            .coerceAtLeast(MIN_COLUMN_CHARS)

        return allRows.mapIndexed { index, row ->
            val bold = table.headerRow != null && index == 0
            val text = row.cells.joinToString("") { cell ->
                cell.content.take(charsPerColumn - 1).padEnd(charsPerColumn * cell.colSpan)
            }
            Line.Text(text.trimEnd(), DEFAULT_FONT_SIZE, bold)
        }
    }

    /** Break [text] to fit [usableWidth], estimating width from the font size. */
    private fun wrap(text: String, size: Double, bold: Boolean, usableWidth: Double): List<Line> {
        val fontSize = if (size <= 0) DEFAULT_FONT_SIZE else size
        // Helvetica averages ~0.5em per character; exact metrics would need the AFM tables, and
        // over-estimating slightly is safer than clipping.
        val maxChars = (usableWidth / (fontSize * 0.5)).toInt().coerceAtLeast(MIN_WRAP_CHARS)
        if (text.isEmpty()) return listOf(Line.Gap(fontSize))

        val out = mutableListOf<Line>()
        for (paragraph in text.split("\n")) {
            if (paragraph.isEmpty()) {
                out += Line.Gap(fontSize)
                continue
            }
            var current = StringBuilder()
            for (word in paragraph.split(" ")) {
                when {
                    current.isEmpty() -> current.append(word)

                    current.length + 1 + word.length <= maxChars -> current.append(' ').append(word)

                    else -> {
                        out += Line.Text(current.toString(), fontSize, bold)
                        current = StringBuilder(word)
                    }
                }
            }
            if (current.isNotEmpty()) out += Line.Text(current.toString(), fontSize, bold)
        }
        return out
    }

    private fun stripTags(html: String): String = html
        .replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
        .replace(Regex("</p>", RegexOption.IGNORE_CASE), "\n")
        .replace(Regex("<[^>]*>"), "")
        .replace("&nbsp;", " ")
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .trim()

    // ---- pagination ---------------------------------------------------------------------------

    private fun paginate(lines: List<Line>, usableHeight: Double): List<List<Line.Text>> {
        val pages = mutableListOf<List<Line.Text>>()
        var current = mutableListOf<Line.Text>()
        var used = 0.0

        fun flush() {
            pages += current
            current = mutableListOf()
            used = 0.0
        }

        for (line in lines) {
            when (line) {
                is Line.PageBreak -> flush()

                is Line.Gap -> used += line.points

                is Line.Text -> {
                    val height = line.size * LINE_SPACING
                    if (used + height > usableHeight && current.isNotEmpty()) flush()
                    current += line
                    used += height
                }
            }
        }
        // A trailing flush even when empty keeps at least one page: a PDF with zero pages is
        // invalid and most readers refuse to open it.
        if (current.isNotEmpty() || pages.isEmpty()) flush()
        return pages
    }

    // ---- PDF assembly -------------------------------------------------------------------------

    private fun buildPdf(
        pages: List<List<Line.Text>>,
        pageWidth: Double,
        pageHeight: Double,
        left: Double,
        topBaseline: Double,
    ): ByteArray {
        val contents = pages.map { contentStream(it, left, topBaseline) }

        // Object layout: 1 catalog, 2 pages tree, 3 Helvetica, 4 Helvetica-Bold,
        // then per page: a page object and its content stream.
        val objects = mutableListOf<String>()
        val pageIds = pages.indices.map { FIXED_OBJECTS + 1 + it * 2 }

        objects += "<< /Type /Catalog /Pages 2 0 R >>"
        objects += "<< /Type /Pages /Kids [${pageIds.joinToString(" ") { "$it 0 R" }}] /Count ${pages.size} >>"
        objects += "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>"
        objects += "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold /Encoding /WinAnsiEncoding >>"

        contents.forEachIndexed { index, stream ->
            val contentId = pageIds[index] + 1
            objects += "<< /Type /Page /Parent 2 0 R " +
                "/MediaBox [0 0 ${fmt(pageWidth)} ${fmt(pageHeight)}] " +
                "/Resources << /Font << /F1 3 0 R /F2 4 0 R >> >> " +
                "/Contents $contentId 0 R >>"
            objects += "<< /Length ${stream.length} >>\nstream\n$stream\nendstream"
        }

        val sb = StringBuilder()
        sb.append("%PDF-1.4\n")
        // A binary comment marks the file as binary for transport layers that sniff content.
        sb.append("%âãÏÓ\n")

        val offsets = IntArray(objects.size + 1)
        objects.forEachIndexed { index, body ->
            offsets[index + 1] = sb.length
            sb.append(index + 1).append(" 0 obj\n").append(body).append("\nendobj\n")
        }

        val xrefStart = sb.length
        sb.append("xref\n0 ").append(objects.size + 1).append('\n')
        sb.append("0000000000 65535 f \n")
        for (i in 1..objects.size) {
            sb.append(offsets[i].toString().padStart(10, '0')).append(" 00000 n \n")
        }
        sb.append("trailer\n<< /Size ").append(objects.size + 1).append(" /Root 1 0 R >>\n")
        sb.append("startxref\n").append(xrefStart).append("\n%%EOF\n")

        return sb.toString().encodeToLatin1()
    }

    private fun contentStream(lines: List<Line.Text>, left: Double, topBaseline: Double): String {
        val sb = StringBuilder("BT\n")
        var y = topBaseline
        for (line in lines) {
            y -= line.size * LINE_SPACING
            val font = if (line.bold) "/F2" else "/F1"
            sb.append(font).append(' ').append(fmt(line.size)).append(" Tf\n")
            sb.append("1 0 0 1 ").append(fmt(left)).append(' ').append(fmt(y)).append(" Tm\n")
            sb.append('(').append(escapePdfString(line.text)).append(") Tj\n")
        }
        sb.append("ET")
        return sb.toString()
    }

    /** `(`, `)` and `\` are structural inside a PDF string literal and must be escaped. */
    private fun escapePdfString(text: String): String = buildString {
        for (ch in text) {
            when (ch) {
                '\\' -> append("\\\\")

                '(' -> append("\\(")

                ')' -> append("\\)")

                // Outside Latin-1 there is no WinAnsi code point; '?' beats emitting a byte the
                // reader would render as mojibake.
                else -> append(if (ch.code in 32..255) ch else '?')
            }
        }
    }

    /** PDF numbers are plain decimals — no exponent form, no locale separators. */
    private fun fmt(value: Double): String {
        val scaled = (value * 100).toLong()
        return "${scaled / 100}.${(scaled % 100).toString().padStart(2, '0').trimEnd('0').ifEmpty { "0" }}"
    }

    private fun String.encodeToLatin1(): ByteArray =
        ByteArray(length) { index -> this[index].code.coerceIn(0, 255).toByte() }

    private const val FIXED_OBJECTS = 4
    private const val MIN_COLUMN_CHARS = 6
    private const val MIN_WRAP_CHARS = 10
}
