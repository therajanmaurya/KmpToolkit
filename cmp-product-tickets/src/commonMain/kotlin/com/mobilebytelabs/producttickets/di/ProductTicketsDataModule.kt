/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
package com.mobilebytelabs.producttickets.di

import com.mobilebytelabs.producttickets.data.remote.ProductTicketsRepository
import com.mobilebytelabs.producttickets.data.remote.ProductTicketsService
import com.mobilebytelabs.producttickets.data.remote.ProductTicketsServiceImpl
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

/**
 * Headless bindings — the Supabase-backed service and the repository above it. No UI, no ViewModel.
 *
 * ## Why this exists separately from `productTicketsModule`
 * `productTicketsModule` (now in cmp-product-tickets-compose) registers the ViewModel alongside these
 * bindings, which pinned the whole DI graph to the Compose targets. Splitting it here lets a server,
 * CLI or background worker fetch and submit tickets with no renderer on the class path — and keeps
 * [ProductTicketsService], its impl and the Ktor client `internal`, which moving the whole module to
 * the Compose artifact would have forced public.
 *
 * Most consumers never reference this directly: `productTicketsModule` includes it, so installing that
 * one module still wires everything exactly as before. Reach for this only in a headless host:
 *
 * ```kotlin
 * startKoin { modules(productTicketsDataModule) }   // tickets, no UI
 * ```
 */
public val productTicketsDataModule: Module = module {
    singleOf(::ProductTicketsServiceImpl) bind ProductTicketsService::class
    singleOf(::ProductTicketsRepository)
}
