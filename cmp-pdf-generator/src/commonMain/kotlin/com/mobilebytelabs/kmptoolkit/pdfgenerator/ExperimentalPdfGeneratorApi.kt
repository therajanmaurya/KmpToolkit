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
 * Retained no-op marker. **cmp-pdf-generator graduated to a stable API — no opt-in is required.**
 *
 * This annotation no longer carries [RequiresOptIn], so it neither warns nor demands
 * `@OptIn`. It is kept only so that source written against the experimental era —
 * `@OptIn(ExperimentalPdfGeneratorApi::class)` or `-opt-in=...ExperimentalPdfGeneratorApi` — keeps
 * compiling. Both are now redundant and can be deleted.
 *
 * Unlike the sibling graduated markers this one keeps its [Target] list, because the experimental
 * surface allowed `TYPEALIAS` — a target the default set does NOT include, so dropping the list
 * would stop a consumer's `@ExperimentalPdfGeneratorApi typealias …` from compiling. Retaining it
 * costs nothing and is the whole point of keeping the class.
 *
 * Scheduled for removal in the next major version.
 */
@Deprecated(
    message = "cmp-pdf-generator is stable; the opt-in is no longer required. Remove the @OptIn / annotation.",
    level = DeprecationLevel.WARNING,
)
@Retention(AnnotationRetention.BINARY)
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.TYPEALIAS,
)
public annotation class ExperimentalPdfGeneratorApi
