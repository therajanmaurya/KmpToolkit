/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
package com.mobilebytelabs.kmptoolkit.share.di

import com.mobilebytelabs.kmptoolkit.share.ShareManager
import com.mobilebytelabs.kmptoolkit.share.ShareManagerImpl
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

/**
 * Koin module binding [ShareManager] to [ShareManagerImpl].
 *
 * ```kotlin
 * startKoin {
 *     modules(shareModule, /* your modules */)
 * }
 * ```
 *
 * Bound as a `single` because [ShareManagerImpl] is stateless.
 *
 * **Not using Koin?** You do not need this module — construct `ShareManagerImpl()` and register it
 * against [ShareManager] in whatever container you use. Nothing in cmp-share requires Koin except
 * this file.
 */
public val shareModule: Module = module {
    singleOf(::ShareManagerImpl) bind ShareManager::class
}
