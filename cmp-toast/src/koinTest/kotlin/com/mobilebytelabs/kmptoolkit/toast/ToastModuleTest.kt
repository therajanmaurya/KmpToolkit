/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
package com.mobilebytelabs.kmptoolkit.toast

import com.mobilebytelabs.kmptoolkit.toast.di.toastModule
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertSame

/** A module that compiles but cannot produce its binding fails at the consumer's first injection. */
class ToastModuleTest {

    @AfterTest
    fun tearDown() = stopKoin()

    @Test
    fun both_bindings_resolve_to_the_same_object() {
        // The whole point: ToastHostState serialises toasts through a mutex, so two instances
        // would race for one screen and a ToastHost bound to one would never show the other's.
        val koin = startKoin { modules(toastModule) }.koin
        assertSame<Any>(koin.get<ToastHostState>(), koin.get<ToastDispatcher>())
    }

    @Test
    fun the_state_is_a_singleton() {
        val koin = startKoin { modules(toastModule) }.koin
        assertSame(koin.get<ToastHostState>(), koin.get<ToastHostState>())
    }
}
