/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
package com.mobilebytelabs.kmptoolkit.intentlauncher.di

import com.mobilebytelabs.kmptoolkit.intentlauncher.IntentManager
import com.mobilebytelabs.kmptoolkit.intentlauncher.IntentManagerImpl
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Koin module binding [IntentManager] to [IntentManagerImpl].
 *
 * ```kotlin
 * startKoin { modules(intentLauncherModule, /* your modules */) }
 * ```
 *
 * ## Android
 * The default binding has no `IntentLauncher`, because one is Activity-scoped — so `openAppSettings`
 * and `createDocument` work out of the box and the pickers report unsupported. Override inside your
 * Activity to get the full surface:
 *
 * ```kotlin
 * loadKoinModules(module { single<IntentManager> { IntentManagerImpl(intentLauncher()) } })
 * ```
 *
 * **Not using Koin?** You do not need this module — construct `IntentManagerImpl()` and register
 * it against [IntentManager] in whatever container you use. Nothing else in cmp-intent-launcher
 * requires Koin.
 */
public val intentLauncherModule: Module = module {
    // An explicit lambda, NOT `singleOf(::IntentManagerImpl)`: `singleOf` resolves every
    // constructor parameter from the graph and ignores Kotlin default values, so it would demand
    // an `IntentLauncher` binding that only Android-with-an-Activity can provide. This way the
    // per-platform default applies.
    single<IntentManager> { IntentManagerImpl() }
}
