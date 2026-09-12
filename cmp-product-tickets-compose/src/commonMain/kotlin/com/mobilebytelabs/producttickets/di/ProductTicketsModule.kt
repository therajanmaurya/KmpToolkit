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

import com.mobilebytelabs.producttickets.ui.ProductTicketsViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/**
 * Everything cmp-product-tickets needs: the headless bindings plus the Compose [ProductTicketsViewModel].
 *
 * Unchanged in name and effect from the single-artifact version — `includes(productTicketsDataModule)`
 * supplies the service and repository registrations that used to be written inline here, so a consumer
 * that installs this module gets the same graph as before. Only its artifact moved.
 */
public val productTicketsModule: Module = module {
    includes(productTicketsDataModule)
    viewModelOf(::ProductTicketsViewModel)
}
