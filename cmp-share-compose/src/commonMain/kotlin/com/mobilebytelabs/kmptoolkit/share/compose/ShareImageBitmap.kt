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
import com.mobilebytelabs.kmptoolkit.share.ShareError
import com.mobilebytelabs.kmptoolkit.share.ShareManager
import com.mobilebytelabs.kmptoolkit.share.ShareResult

/**
 * Share what is on screen — a chart, a QR code, a generated card — straight from an [ImageBitmap].
 *
 * ## Why this lives in the Compose module
 * [ImageBitmap] is a Compose type, so the headless `cmp-share` artifact cannot mention it without
 * dragging Compose onto targets that have none (tvOS, Linux, Windows). Encoding it is also the one
 * part of sharing that genuinely needs a platform bitmap codec — [encodeImageAsPng] carries that
 * split, and it is the last piece a consumer used to have to write themselves.
 *
 * ```kotlin
 * val share = rememberShareManager()
 * val graphicsLayer = rememberGraphicsLayer()
 * scope.launch { share.shareImage("sales-q3", graphicsLayer.toImageBitmap()) }
 * ```
 *
 * @param title names the shared file — `"$title.png"`.
 * @return [ShareResult.Failed] with [ShareError.Unknown] when the bitmap cannot be encoded, rather
 *   than raising a chooser that fails after the user has already picked a target.
 */
public suspend fun ShareManager.shareImage(title: String, image: ImageBitmap, message: String? = null): ShareResult {
    val bytes = encodeImageAsPng(image)
        ?: return ShareResult.Failed(ShareError.Unknown("Could not encode ImageBitmap as PNG"))
    return shareImage(
        bytes = bytes,
        mimeType = "image/png",
        filename = "$title.png",
        message = message,
    )
}

/** Encode [image] as PNG bytes, or `null` when this target's codec refuses it. */
internal expect fun encodeImageAsPng(image: ImageBitmap): ByteArray?
