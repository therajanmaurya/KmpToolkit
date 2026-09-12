/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
package com.mobilebytelabs.kmptoolkit.openurl.di

import com.mobilebytelabs.kmptoolkit.openurl.UrlLauncher
import com.mobilebytelabs.kmptoolkit.openurl.UrlLauncherImpl
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

/**
 * Koin module binding [UrlLauncher] to [UrlLauncherImpl].
 *
 * ```kotlin
 * startKoin { modules(openUrlModule, /* your modules */) }
 * ```
 *
 * Bound as a `single` because [UrlLauncherImpl] is stateless.
 *
 * **Not using Koin?** You do not need this module — construct `UrlLauncherImpl()` and register it
 * against [UrlLauncher] in whatever container you use.
 */
public val openUrlModule: Module = module {
    singleOf(::UrlLauncherImpl) bind UrlLauncher::class
}
