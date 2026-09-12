/*
 * Copyright 2026 MobileByteLabs · Apache 2.0
 */
@file:OptIn(
    kotlinx.cinterop.ExperimentalForeignApi::class,
    kotlinx.cinterop.BetaInteropApi::class,
)

package com.mobilebytelabs.kmptoolkit.pdfgenerator

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.create

/** Convert NSData → ByteArray. */
@OptIn(ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)
internal fun NSData.toByteArray(): ByteArray {
    val size = length.toInt()
    val ba = ByteArray(size)
    if (size > 0) {
        ba.usePinned { pinned ->
            // `.convert()` rather than `.toULong()`: size_t / NSUInteger is 32-bit on
            // watchosArm32 and 64-bit elsewhere, and a hardcoded ULong will not compile on both.
            platform.posix.memcpy(pinned.addressOf(0), bytes, size.convert())
        }
    }
    return ba
}

/** Convert ByteArray → NSData. */
@OptIn(ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)
internal fun ByteArray.toNSData(): NSData = usePinned { pinned ->
    NSData.create(bytes = pinned.addressOf(0), length = size.convert())
}

// injectPageConfigCss moved to commonMain (PageConfigCssInjection.kt) — internal visibility
