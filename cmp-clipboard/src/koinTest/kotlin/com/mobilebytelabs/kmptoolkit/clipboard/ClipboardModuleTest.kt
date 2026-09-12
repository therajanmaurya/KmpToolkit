/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
package com.mobilebytelabs.kmptoolkit.clipboard

import com.mobilebytelabs.kmptoolkit.clipboard.di.clipboardModule
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

/** A module that compiles but cannot produce its binding fails at the consumer's first injection. */
class ClipboardModuleTest {

    @AfterTest
    fun tearDown() = stopKoin()

    @Test
    fun clipboard_manager_resolves_from_the_module() {
        val koin = startKoin { modules(clipboardModule()) }.koin
        assertEquals(
            ClipboardManagerConfig.Default.historySize,
            koin.get<ClipboardManager>().historyMaxSize,
        )
    }

    @Test
    fun clipboard_manager_is_a_singleton() {
        // Two instances would mean two monitors on one clipboard and a split history — the exact
        // bug this binding exists to prevent.
        val koin = startKoin { modules(clipboardModule()) }.koin
        assertSame(koin.get<ClipboardManager>(), koin.get<ClipboardManager>())
    }

    @Test
    fun the_narrow_surface_resolves_to_the_same_object_as_the_manager() {
        // Two instances would mean two histories and two monitors on one clipboard.
        val koin = startKoin { modules(clipboardModule()) }.koin
        assertSame<Any>(koin.get<ClipboardManager>(), koin.get<Clipboard>())
    }

    @Test
    fun a_custom_config_reaches_the_manager() {
        val koin = startKoin { modules(clipboardModule(ClipboardManagerConfig(historySize = 7))) }.koin
        assertEquals(7, koin.get<ClipboardManager>().historyMaxSize)
    }
}
