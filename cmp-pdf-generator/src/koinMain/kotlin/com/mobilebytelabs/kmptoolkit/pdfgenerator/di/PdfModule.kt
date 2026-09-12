/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
package com.mobilebytelabs.kmptoolkit.pdfgenerator.di

import com.mobilebytelabs.kmptoolkit.pdfgenerator.PdfManager
import com.mobilebytelabs.kmptoolkit.pdfgenerator.PdfManagerImpl
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Koin module binding [PdfManager] to [PdfManagerImpl].
 *
 * ```kotlin
 * startKoin { modules(pdfModule, appModule) }
 * ```
 *
 * A `single`: `PdfGenerator` carries a progress flow that callers subscribe to, and a second
 * instance would emit into a flow nobody is watching.
 *
 * **Not using Koin?** Construct `PdfManagerImpl()` and register it against [PdfManager] in
 * whatever container you use. Nothing else in cmp-pdf-generator requires Koin.
 */
public val pdfModule: Module = module {
    single<PdfManager> { PdfManagerImpl() }
}
