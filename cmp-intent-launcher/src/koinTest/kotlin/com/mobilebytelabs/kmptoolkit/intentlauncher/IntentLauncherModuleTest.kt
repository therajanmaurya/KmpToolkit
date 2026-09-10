/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
package com.mobilebytelabs.kmptoolkit.intentlauncher

import com.mobilebytelabs.kmptoolkit.intentlauncher.di.intentLauncherModule
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertSame

/**
 * A Koin module that compiles but cannot produce its binding fails at the consumer's first
 * injection, not here — which is far too late.
 */
class IntentLauncherModuleTest {

    @AfterTest
    fun tearDown() = stopKoin()

    @Test
    fun intent_manager_resolves_from_the_module() {
        val koin = startKoin { modules(intentLauncherModule) }.koin
        assertIs<IntentManagerImpl>(koin.get<IntentManager>())
    }

    @Test
    fun intent_manager_is_a_singleton() {
        val koin = startKoin { modules(intentLauncherModule) }.koin
        assertSame(koin.get<IntentManager>(), koin.get<IntentManager>())
    }
}
