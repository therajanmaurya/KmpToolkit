/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
package com.mobilebytelabs.kmptoolkit.appupdate.di

import com.mobilebytelabs.kmptoolkit.appupdate.AppUpdateConfig
import com.mobilebytelabs.kmptoolkit.appupdate.AppUpdateManager
import com.mobilebytelabs.kmptoolkit.appupdate.AppUpdateManagerImpl
import com.mobilebytelabs.kmptoolkit.appupdate.UpdateType
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Koin module binding [AppUpdateManager] to [AppUpdateManagerImpl].
 *
 * The config is a parameter rather than a graph lookup because it is app-specific — store ids,
 * download URLs, per-platform enables — and there is no sensible default the library could
 * inject on your behalf:
 *
 * ```kotlin
 * startKoin {
 *     modules(
 *         appUpdateModule(
 *             AppUpdateConfig(iosAppStoreId = "123456789", packageName = "com.example.app"),
 *         ),
 *     )
 * }
 * ```
 *
 * Bound as a `single` because [AppUpdateManagerImpl] is stateless.
 *
 * **Not using Koin?** Construct `AppUpdateManagerImpl(config)` and register it against
 * [AppUpdateManager] in whatever container you use.
 */
public fun appUpdateModule(
    config: AppUpdateConfig = AppUpdateConfig.Default,
    defaultUpdateType: UpdateType = UpdateType.IMMEDIATE,
): Module = module {
    single<AppUpdateManager> { AppUpdateManagerImpl(config, defaultUpdateType) }
}
