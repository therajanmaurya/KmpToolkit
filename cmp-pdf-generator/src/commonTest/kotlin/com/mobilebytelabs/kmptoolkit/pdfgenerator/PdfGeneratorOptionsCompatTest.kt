/*
 * Copyright 2026 MobileByteLabs · Apache 2.0
 */
@file:OptIn(ExperimentalPdfGeneratorApi::class)

package com.mobilebytelabs.kmptoolkit.pdfgenerator

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds

/**
 * Pins the four-property compatibility surface of [PdfGeneratorOptions].
 *
 * Adding `renderTimeout` to the primary constructor deletes four JVM symbols (the four-arg `<init>`
 * and `copy`, plus the two default-argument synthetics that `PdfGeneratorOptions(dpi = N)` and
 * `copy(dpi = N)` compile to). The shims restore them; these tests keep the SOURCE-level shapes
 * working and lock the one behaviour a shim can silently get wrong.
 *
 * The binary half is enforced by Binary Compatibility Validator (`apiCheck`), which is the only thing
 * that can actually see a removed JVM signature — a test cannot.
 */
class PdfGeneratorOptionsCompatTest {

    @Test
    fun the_four_property_call_shapes_all_still_resolve() {
        // Each of these is a shape a 3.5.x consumer could already have compiled.
        assertEquals(100, PdfGeneratorOptions(dpi = 100).dpi, "partial named args")
        assertEquals(150, PdfGeneratorOptions(true, null, 150, true).dpi, "full positional, 4 args")
        assertEquals(200, PdfGeneratorOptions(dpi = 100).copy(dpi = 200).dpi, "partial named copy")
        assertEquals(
            250,
            PdfGeneratorOptions(true, null, 150, true).copy(true, null, 250, true).dpi,
            "full positional copy, 4 args",
        )
    }

    @Test
    fun the_four_property_copy_preserves_a_custom_renderTimeout() {
        // The subtle one. A four-arg copy predates renderTimeout and cannot mention it, so it must
        // forward the receiver's value. Resetting to the 60s default here would silently un-configure
        // the timeout for exactly the callers the shim exists to protect.
        val configured = PdfGeneratorOptions(renderTimeout = 5.seconds)
        assertEquals(5.seconds, configured.copy(dpi = 400).renderTimeout, "named 4-property copy")
        assertEquals(
            5.seconds,
            configured.copy(true, null, 400, true).renderTimeout,
            "positional 4-property copy",
        )
    }

    @Test
    fun the_default_is_shared_by_the_primary_constructor_and_the_shim() {
        // If these two ever disagreed, which overload a call resolved to would change behaviour.
        assertEquals(60.seconds, PdfGeneratorOptions().renderTimeout, "documented default")
        assertEquals(60.seconds, PdfGeneratorOptions(dpi = 300).renderTimeout, "via the shim")
        assertEquals(
            60.seconds,
            PdfGeneratorOptions(true, null, 300, true).renderTimeout,
            "via the positional shim",
        )
    }
}
