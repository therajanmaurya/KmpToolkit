/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
package com.mobilebytelabs.kmptoolkit.appreview.di

import com.mobilebytelabs.kmptoolkit.appreview.AppReview
import com.mobilebytelabs.kmptoolkit.appreview.AppReviewManager
import com.mobilebytelabs.kmptoolkit.appreview.AppReviewManagerImpl
import com.mobilebytelabs.kmptoolkit.appreview.StoreListing
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Binds [AppReviewManager]. No parameters — register it and go.
 *
 * ```kotlin
 * startKoin { modules(appReviewModule) }
 * ```
 *
 * Or drop the binding straight into an existing platform module, which is the shape a consumer
 * already has for the other capability managers:
 *
 * ```kotlin
 * val platformModule = module {
 *     single<UrlLauncher> { UrlLauncherImpl() }
 *     single<ShareManager> { ShareManagerImpl() }
 *     single<AppReviewManager> { AppReviewManagerImpl() }   // no Activity, no config, no arguments
 * }
 * ```
 *
 * ## Where the store ids go
 * NOT here. Configure them once at startup and every injected manager picks them up, because
 * [AppReviewManagerImpl] resolves the listing at CALL time rather than capturing it:
 *
 * ```kotlin
 * AppReview.configure(
 *     StoreListing(appStoreId = BuildKonfig.APP_STORE_ID, webUrl = BuildKonfig.APP_WEB_URL),
 * )
 * ```
 *
 * Keeping ids out of the DI module is deliberate. They are deployment data — they belong wherever
 * your build already keeps app identity (a generated BuildKonfig constant, an app-profile YAML) and
 * they differ per flavour and per white-label fork, while the binding does not. Threading them
 * through the module would force every consumer to plumb build config into DI just to register a
 * capability, and a fork that forgot would get a silently empty listing.
 *
 * `single<AppReviewManager> { AppReviewManagerImpl() }` rather than
 * `singleOf(::AppReviewManagerImpl)`: Koin resolves every constructor parameter from the graph and
 * ignores Kotlin defaults, so the constructor-reference form would demand `StoreListing` and
 * `UrlLauncher` bindings the consumer never registered — the mistake that cost a debugging session
 * on `cmp-intent-launcher`.
 */
public val appReviewModule: Module = module {
    single<AppReviewManager> { AppReviewManagerImpl() }
}
