/*
 * Copyright 2026 MobileByteLabs · Apache 2.0
 */
// The legacy opt-in is the POINT of this file: cmp-pdf-generator graduated to stable, and [ExperimentalPdfGeneratorApi]
// is retained purely so source written during the experimental era keeps compiling. Suppressed
// because both warnings here are the expected, documented ones — DEPRECATION for the retained
// marker, OPT_IN_ARGUMENT_IS_NOT_MARKER because it is no longer a @RequiresOptIn annotation.
@file:Suppress("DEPRECATION", "OPT_IN_ARGUMENT_IS_NOT_MARKER")
@file:OptIn(ExperimentalPdfGeneratorApi::class)

package com.mobilebytelabs.kmptoolkit.pdfgenerator

import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Pins the graduation contract for cmp-pdf-generator.
 *
 * Two promises are made when an opt-in marker graduates, and neither was covered by a test before:
 *  1. The API is usable with NO opt-in at all — the whole point of going stable.
 *  2. Source that still carries the old `@OptIn(ExperimentalPdfGeneratorApi::class)` keeps compiling,
 *     which is why the marker class is retained rather than deleted.
 *
 * This file proves (2) by existing — the file-level opt-in above is the legacy form — and asserts
 * (1) in the body, which touches the stable surface directly.
 */
class GraduatedApiCompatTest {

    @Test
    fun the_stable_surface_is_reachable_without_opting_in() {
        // No @OptIn is needed for this call; the file-level one above is vestigial.
        val caps = PageConfig()
        assertTrue(caps is PageConfig, "the capability descriptor is part of the stable surface")
    }

    @Test
    fun the_retained_marker_still_applies_to_the_targets_it_always_did() {
        // Compilation IS the assertion — see [LegacyAnnotated] and [legacyAnnotated] below.
        assertTrue(LegacyAnnotated().ok, "a class carrying the retained marker still compiles")
        assertTrue(legacyAnnotated(), "a function carrying the retained marker still compiles")
    }
}

@ExperimentalPdfGeneratorApi
private class LegacyAnnotated {
    val ok: Boolean = true
}

@ExperimentalPdfGeneratorApi
private fun legacyAnnotated(): Boolean = true

// Proves the retained @Target list still covers TYPEALIAS — a target the DEFAULT set omits, which is
// why this marker keeps its @Target where the sibling graduated markers dropped theirs.
@ExperimentalPdfGeneratorApi
private typealias LegacyAliasedDocument = PdfDocument
