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
 * Binds [AppReviewManager] **and** applies the store listing — DI setup is the single place this is
 * configured.
 *
 * ```kotlin
 * startKoin {
 *     modules(
 *         platformModule,
 *         appReviewModule(
 *             StoreListing(
 *                 appStoreId = BuildKonfig.APP_STORE_ID,
 *                 webUrl = BuildKonfig.APP_WEB_URL,
 *             ),
 *         ),
 *     )
 * }
 * ```
 *
 * Nothing else to call at startup: registering the module IS the configuration step. The ids come
 * from wherever the build already keeps app identity — a generated BuildKonfig constant, an
 * app-profile YAML — so a white-label fork changes one value in its profile and nothing in code.
 *
 * ## Binding it inside your own platform module
 * If you would rather keep every capability in one module, do both there — `module { }` bodies run
 * eagerly, so the listing is applied as the module is built:
 *
 * ```kotlin
 * val platformModule = module {
 *     AppReview.configure(StoreListing(appStoreId = BuildKonfig.APP_STORE_ID))
 *
 *     single<UrlLauncher> { UrlLauncherImpl() }
 *     single<ShareManager> { ShareManagerImpl() }
 *     single<AppReviewManager> { AppReviewManagerImpl() }
 * }
 * ```
 *
 * Binding `AppReviewManagerImpl()` without configuring a listing is still valid — it is exactly what
 * a purely Android/iOS/macOS app wants, since those have a native prompt and never reach the
 * fallback. Every other target would report [com.mobilebytelabs.kmptoolkit.appreview.AppReviewResult.NoStoreConfigured].
 *
 * ## Why the listing is applied here rather than captured
 * [AppReviewManagerImpl] resolves the listing at CALL time, so the order of `configure` and the
 * binding does not matter and no consumer has to think about it. Capturing it in the constructor
 * would make a graph built before configuration silently hold an empty listing.
 *
 * `single<AppReviewManager> { AppReviewManagerImpl() }` rather than
 * `singleOf(::AppReviewManagerImpl)`: Koin resolves every constructor parameter from the graph and
 * ignores Kotlin defaults, so the constructor-reference form would demand `StoreListing` and
 * `UrlLauncher` bindings the consumer never registered — the mistake that cost a debugging session
 * on `cmp-intent-launcher`.
 *
 * @param listing where the app is published. Omit only if every target you ship has a native review
 *   API (Android, iOS, macOS); everything else needs it to have somewhere to go.
 */
public fun appReviewModule(listing: StoreListing = StoreListing.None): Module {
    AppReview.configure(listing)
    return module {
        single<AppReviewManager> { AppReviewManagerImpl() }
    }
}
