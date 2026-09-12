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

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import java.io.ByteArrayOutputStream

/** Android — the platform's own PNG encoder. `compress` returns false if the format is refused. */
internal actual fun encodeImageAsPng(image: ImageBitmap): ByteArray? = runCatching {
    ByteArrayOutputStream().use { out ->
        if (image.asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, PNG_QUALITY, out)) {
            out.toByteArray()
        } else {
            null
        }
    }
}.getOrNull()

/** PNG is lossless, so this is a compression-effort hint rather than a quality setting. */
private const val PNG_QUALITY = 100
