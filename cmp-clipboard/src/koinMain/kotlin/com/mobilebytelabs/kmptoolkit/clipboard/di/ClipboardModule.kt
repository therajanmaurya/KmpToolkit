/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
package com.mobilebytelabs.kmptoolkit.clipboard.di

import com.mobilebytelabs.kmptoolkit.clipboard.ClipboardManager
import com.mobilebytelabs.kmptoolkit.clipboard.ClipboardManagerConfig
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Koin module providing a [ClipboardManager].
 *
 * ```kotlin
 * startKoin { modules(clipboardModule(), /* your modules */) }
 * ```
 *
 * A `single` because [ClipboardManager] owns a monitor, an observer and a bounded history —
 * building a second one would start a second monitor watching the same clipboard and split the
 * history across two objects, which is the bug this binding exists to prevent.
 *
 * @param config passed straight to the manager; defaults to [ClipboardManagerConfig.Default].
 *   Supply your own to change history size, filters or URL matchers:
 *
 * ```kotlin
 * modules(clipboardModule(ClipboardManagerConfig(historySize = 50)))
 * ```
 *
 * **Not using Koin?** You do not need this module — construct `ClipboardManager()` once and share
 * that instance through whatever container you use.
 */
public fun clipboardModule(config: ClipboardManagerConfig = ClipboardManagerConfig.Default): Module = module {
    single { ClipboardManager(config) }
}
