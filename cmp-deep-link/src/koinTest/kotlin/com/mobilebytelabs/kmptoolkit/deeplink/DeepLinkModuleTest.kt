/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
package com.mobilebytelabs.kmptoolkit.deeplink

import com.mobilebytelabs.kmptoolkit.deeplink.di.deepLinkModule
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertSame

/** A module that compiles but cannot produce its binding fails at the consumer's first injection. */
class DeepLinkModuleTest {

    @AfterTest
    fun tearDown() = stopKoin()

    @Test
    fun manager_resolves_from_the_module() {
        val koin = startKoin { modules(deepLinkModule) }.koin
        assertIs<DeepLinkManagerImpl>(koin.get<DeepLinkManager>())
    }

    @Test
    fun manager_is_a_singleton() {
        val koin = startKoin { modules(deepLinkModule) }.koin
        assertSame(koin.get<DeepLinkManager>(), koin.get<DeepLinkManager>())
    }
}
