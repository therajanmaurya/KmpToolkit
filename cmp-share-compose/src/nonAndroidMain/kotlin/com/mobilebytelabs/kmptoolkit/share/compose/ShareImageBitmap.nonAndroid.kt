/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
package com.mobilebytelabs.kmptoolkit.share.compose

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image

/**
 * JVM, iOS, macOS, JS and wasmJs — every non-Android Compose target draws through Skia, so one
 * encoder serves all five.
 */
internal actual fun encodeImageAsPng(image: ImageBitmap): ByteArray? = runCatching {
    Image.makeFromBitmap(image.asSkiaBitmap())
        .encodeToData(EncodedImageFormat.PNG)
        ?.bytes
}.getOrNull()
