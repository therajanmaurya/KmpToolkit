/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
package com.mobilebytelabs.kmptoolkit.share

import com.mobilebytelabs.kmptoolkit.share.di.shareModule
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertSame

/**
 * The DI module must actually resolve — a Koin module that compiles but cannot produce its
 * binding fails at the consumer's first injection, not here, which is far too late.
 */
class ShareModuleTest {

    @AfterTest
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun share_manager_resolves_from_the_module() {
        val koin = startKoin { modules(shareModule) }.koin
        assertIs<ShareManagerImpl>(koin.get<ShareManager>())
    }

    @Test
    fun share_manager_is_a_singleton() {
        val koin = startKoin { modules(shareModule) }.koin
        // ShareManagerImpl is stateless, so the module binds `single` — two injections in
        // different call sites must not build two objects.
        assertSame(koin.get<ShareManager>(), koin.get<ShareManager>())
    }
}
