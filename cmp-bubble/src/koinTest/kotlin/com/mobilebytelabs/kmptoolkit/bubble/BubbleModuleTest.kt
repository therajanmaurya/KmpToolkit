/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
package com.mobilebytelabs.kmptoolkit.bubble

import com.mobilebytelabs.kmptoolkit.bubble.di.bubbleModule
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertSame
import kotlin.test.assertTrue

/** A module that compiles but cannot produce its binding fails at the consumer's first injection. */
class BubbleModuleTest {

    @AfterTest
    fun tearDown() = stopKoin()

    @Test
    fun bubble_and_permission_both_resolve() {
        val koin = startKoin { modules(bubbleModule()) }.koin
        assertTrue(koin.get<Bubble>().capabilityReason.isNotBlank())
        koin.get<BubblePermission>()
    }

    @Test
    fun the_bubble_is_a_singleton() {
        // A bubble owns platform resources — a tray icon, a notification channel. Two instances
        // would register them twice.
        val koin = startKoin { modules(bubbleModule()) }.koin
        assertSame(koin.get<Bubble>(), koin.get<Bubble>())
    }

    @Test
    fun a_custom_config_reaches_the_factory() {
        val koin = startKoin { modules(bubbleModule(BubbleConfig.Chat)) }.koin
        assertTrue(koin.get<Bubble>().capabilityReason.isNotBlank())
    }
}
