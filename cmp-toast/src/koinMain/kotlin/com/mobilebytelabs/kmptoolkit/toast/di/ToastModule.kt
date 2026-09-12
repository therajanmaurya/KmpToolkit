/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
package com.mobilebytelabs.kmptoolkit.toast.di

import com.mobilebytelabs.kmptoolkit.toast.ToastDispatcher
import com.mobilebytelabs.kmptoolkit.toast.ToastHostState
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Koin module providing the shared [ToastHostState] and binding it as a [ToastDispatcher].
 *
 * ```kotlin
 * startKoin { modules(toastModule, appModule) }
 *
 * // in composition — the SAME instance the ViewModels inject
 * val state: ToastHostState = koinInject()
 * ProvideToastDispatcher(state) {
 *     ToastHost(state = state)
 *     App()
 * }
 * ```
 *
 * Both bindings resolve to one object on purpose. `ToastHostState` serialises toasts through a
 * mutex so only one shows at a time; a second instance would mean two queues racing for the same
 * screen, and a `ToastHost` bound to one would never render what the other was asked to show.
 *
 * **Not using Koin?** Create one `ToastHostState()` at your app root and pass it to both
 * `ProvideToastDispatcher` and `ToastHost`.
 */
public val toastModule: Module = module {
    single { ToastHostState() }
    // Not a second `single { }`: that would build a distinct instance and split the queue.
    single<ToastDispatcher> { get<ToastHostState>() }
}
