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
 * No HTML renderer exists on tvOS, watchOS, Linux, Windows or WASI, so generation goes through
 * [TextPdfWriter] and markup is stripped to text rather than laid out.
 */
public actual val platformSupportsHtmlLayout: Boolean = false
