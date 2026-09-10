/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
package com.mobilebytelabs.kmptoolkit.deeplink.di

import com.mobilebytelabs.kmptoolkit.deeplink.DeepLinkManager
import com.mobilebytelabs.kmptoolkit.deeplink.DeepLinkManagerImpl
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

/**
 * Koin module binding [DeepLinkManager] to [DeepLinkManagerImpl].
 *
 * ```kotlin
 * startKoin { modules(deepLinkModule, /* your modules */) }
 * ```
 *
 * A `single`, though it would be harmless either way: the implementation is a thin forwarder to
 * the process-wide `DeepLinkHandler`, so two instances would still share one stream. One
 * instance simply avoids implying otherwise.
 *
 * **Not using Koin?** Construct `DeepLinkManagerImpl()` and register it against [DeepLinkManager]
 * in whatever container you use.
 */
public val deepLinkModule: Module = module {
    singleOf(::DeepLinkManagerImpl) bind DeepLinkManager::class
}
