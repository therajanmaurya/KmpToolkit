/*
 * Copyright 2026 MobileByteLabs · Apache 2.0
 */

package com.mobilebytelabs.kmptoolkit.pdfgenerator

import android.content.Context

/**
 * Factory for the Android implementation.
 *
 * Compose-side users:
 * ```
 * val context = LocalContext.current
 * val generator = remember { createPdfGenerator(context) }
 * ```
 */
public fun createPdfGenerator(context: Context): PdfGenerator = PdfGenerator().apply { setContext(context) }
