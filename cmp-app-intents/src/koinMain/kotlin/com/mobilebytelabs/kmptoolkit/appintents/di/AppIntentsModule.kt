/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
package com.mobilebytelabs.kmptoolkit.appintents.di

import com.mobilebytelabs.kmptoolkit.appintents.AppIntentsManager
import com.mobilebytelabs.kmptoolkit.appintents.AppIntentsManagerImpl
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

/**
 * Koin module binding [AppIntentsManager] to [AppIntentsManagerImpl].
 *
 * ```kotlin
 * startKoin { modules(appIntentsModule, /* your modules */) }
 * ```
 *
 * Bound as a `single` because [AppIntentsManagerImpl] is stateless — the registry it delegates to
 * is process-wide by design, since the OS callbacks that invoke intents arrive with no reference
 * to your object graph.
 *
 * **Not using Koin?** You do not need this module — construct `AppIntentsManagerImpl()` and
 * register it against [AppIntentsManager] in whatever container you use.
 */
public val appIntentsModule: Module = module {
    singleOf(::AppIntentsManagerImpl) bind AppIntentsManager::class
}
