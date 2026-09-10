/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
package com.mobilebytelabs.kmptoolkit.appupdate

import com.mobilebytelabs.kmptoolkit.appupdate.di.appUpdateModule
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertSame

/** A module that compiles but cannot produce its binding fails at the consumer's first injection. */
class AppUpdateModuleTest {

    @AfterTest
    fun tearDown() = stopKoin()

    @Test
    fun manager_resolves_from_the_module() {
        val koin = startKoin { modules(appUpdateModule()) }.koin
        assertIs<AppUpdateManagerImpl>(koin.get<AppUpdateManager>())
    }

    @Test
    fun manager_is_a_singleton() {
        val koin = startKoin { modules(appUpdateModule()) }.koin
        assertSame(koin.get<AppUpdateManager>(), koin.get<AppUpdateManager>())
    }

    @Test
    fun a_custom_config_reaches_the_manager() {
        // The config carries store ids and download URLs, so a binding that dropped it would
        // silently disable openStore() on every platform.
        val koin = startKoin {
            modules(appUpdateModule(AppUpdateConfig(iosAppStoreId = "123456789")))
        }.koin
        assertIs<AppUpdateManagerImpl>(koin.get<AppUpdateManager>())
    }
}
